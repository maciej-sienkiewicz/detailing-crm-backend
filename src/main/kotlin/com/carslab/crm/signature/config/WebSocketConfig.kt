package com.carslab.crm.signature.config

import com.carslab.crm.signature.api.websocket.MultiTenantWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.*

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val webSocketHandler: MultiTenantWebSocketHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(webSocketHandler, "/ws/tablet/*", "/ws/workstation/*")
            .setAllowedOrigins("*") // Configure according to your security requirements
            .withSockJS()
    }
}