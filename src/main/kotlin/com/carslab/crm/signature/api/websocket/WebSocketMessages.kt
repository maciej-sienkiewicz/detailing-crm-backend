package com.carslab.crm.signature.api.websocket

import java.time.Instant

sealed class WebSocketMessage(val type: String)

data class ConnectionStatusMessage(
    val status: String,
    val timestamp: Instant = Instant.now()
) : WebSocketMessage("connection")

data class ErrorMessage(
    val error: String,
    val details: String? = null
) : WebSocketMessage("error")