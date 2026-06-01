package dev.kuisd.sdui.core

import kotlinx.serialization.Serializable

/**
 * Cuerpo de error estructurado (estilo RFC 7807, `application/problem+json`), compartido entre
 * servidor y cliente para mostrar errores legibles sin filtrar detalle interno.
 */
@Serializable
data class ProblemDetail(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
)
