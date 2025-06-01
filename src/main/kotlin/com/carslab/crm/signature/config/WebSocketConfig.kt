package com.carslab.crm.config

import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val signatureWebSocketHandler: SignatureWebSocketHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(signatureWebSocketHandler, "/ws/tablet/*", "/ws/workstation/*")
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }
}