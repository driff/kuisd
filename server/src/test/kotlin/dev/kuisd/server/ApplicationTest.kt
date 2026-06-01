package dev.kuisd.server

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.KUISD_VERSION_HEADER
import dev.kuisd.sdui.core.ProblemDetail
import dev.kuisd.sdui.core.SduiEnvelope
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun health_endpoint_returns_ok() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun screen_home_serves_a_valid_sdui_envelope() = testApplication {
        application { module() }
        val response = client.get("/screen/home")
        assertEquals(HttpStatusCode.OK, response.status)

        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        assertEquals("home", envelope.screenId)
        assertEquals("column", envelope.root.type)
    }

    @Test
    fun screen_details_serves_a_valid_sdui_envelope() = testApplication {
        application { module() }
        val response = client.get("/screen/details")
        assertEquals(HttpStatusCode.OK, response.status)

        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        assertEquals("details", envelope.screenId)
    }

    @Test
    fun screen_details_contains_a_badge_node() = testApplication {
        application { module() }
        val response = client.get("/screen/details")
        assertEquals(HttpStatusCode.OK, response.status)

        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        assertTrue(
            envelope.root.children.any { it.type == "badge" },
            "esperaba un nodo type==badge en details",
        )
    }

    @Test
    fun screen_more_serves_a_valid_sdui_envelope() = testApplication {
        application { module() }
        val response = client.get("/screen/more")
        assertEquals(HttpStatusCode.OK, response.status)

        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        assertEquals("more", envelope.screenId)
    }

    @Test
    fun unknown_screen_returns_problem_detail_404() = testApplication {
        application { module() }
        val response = client.get("/screen/does-not-exist")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(
            response.headers[HttpHeaders.ContentType].orEmpty().contains("problem+json"),
            "esperaba application/problem+json",
        )

        val problem = DefaultSduiJson.decodeFromString(ProblemDetail.serializer(), response.bodyAsText())
        assertEquals(HttpStatusCode.NotFound.value, problem.status)
    }

    @Test
    fun responses_carry_a_correlation_id() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertNotNull(response.headers[HttpHeaders.XRequestId], "esperaba header de correlación")
    }

    @Test
    fun screen_counter_serves_an_envelope_with_initial_variables() = testApplication {
        application { module() }
        val response = client.get("/screen/counter")
        assertEquals(HttpStatusCode.OK, response.status)

        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        assertEquals("counter", envelope.screenId)
        assertEquals(JsonPrimitive(0), envelope.variables["count"])
        assertEquals(JsonPrimitive(false), envelope.variables["flag"])
        assertTrue(
            envelope.root.children.any { node ->
                node.type == "text" && node.props["text"]?.jsonPrimitive?.content == "\$count"
            },
            "esperaba un text con props.text==\"\$count\"",
        )
    }

    @Test
    fun client_version_header_is_accepted() = testApplication {
        application { module() }
        val response = client.get("/screen/home") {
            header(KUISD_VERSION_HEADER, "1")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
