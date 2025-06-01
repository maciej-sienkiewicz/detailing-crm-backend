package com.carslab.crm.signature.config

import com.carslab.crm.signature.websocket.SecureWebSocketHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.*

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val webSocketHandler: SecureWebSocketHandler,
    @Value("\${app.websocket.allowed-origins}") private val allowedOrigins: List<String>
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(webSocketHandler, "/ws/tablet/*", "/ws/workstation/*")
            .setAllowedOrigins(*allowedOrigins.toTypedArray())
            .withSockJS()
    }
}