package dev.kuisd.sdui.core

/**
 * Header HTTP con el que el cliente declara la versión de esquema SDUI que soporta.
 * El servidor lo lee en `GET /screen/{id}` para (futuro) capability negotiation.
 */
const val KUISD_VERSION_HEADER: String = "X-Kuisd-Version"
