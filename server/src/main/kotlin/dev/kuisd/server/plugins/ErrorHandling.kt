package dev.kuisd.server.plugins

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.ProblemDetail
import dev.kuisd.server.screens.ScreenNotFoundException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText

private val ProblemJsonType = ContentType("application", "problem+json")

/** Modelo de error RFC 7807. En producción no filtra `cause.message` (solo con KUISD_DEV=true). */
fun Application.configureErrorHandling(isDev: Boolean) {
    install(StatusPages) {
        exception<ScreenNotFoundException> { call, cause ->
            call.respondProblem(HttpStatusCode.NotFound, "Screen not found", cause.message)
        }
        exception<IllegalArgumentException> { call, cause ->
            // Validación de entrada (p.ej. `require(...)` en una acción) → 400 (spec 009).
            call.respondProblem(HttpStatusCode.BadRequest, "Bad Request", cause.message)
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error (callId=${call.callId})", cause)
            call.respondProblem(
                status = HttpStatusCode.InternalServerError,
                title = "Internal Server Error",
                detail = if (isDev) cause.message else null,
            )
        }
    }
}

private suspend fun ApplicationCall.respondProblem(
    status: HttpStatusCode,
    title: String,
    detail: String? = null,
) {
    val problem = ProblemDetail(
        title = title,
        status = status.value,
        detail = detail,
        instance = callId,
    )
    respondText(
        text = DefaultSduiJson.encodeToString(ProblemDetail.serializer(), problem),
        contentType = ProblemJsonType,
        status = status,
    )
}
