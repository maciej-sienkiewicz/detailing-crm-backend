package com.carslab.crm.config

import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import org.springframework.web.socket.config.annotation.*

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val signatureWebSocketHandler: SignatureWebSocketHandler
) : WebSocketConfigurer {

    private val logger = LoggerFactory.getLogger(WebSocketConfig::class.java)

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        logger.info("Registering WebSocket handlers...")

        registry.addHandler(signatureWebSocketHandler, "/ws/tablet/{deviceId}", "/ws/workstation/{workstationId}")
            .setAllowedOriginPatterns("*")
            .addInterceptors(WebSocketHandshakeInterceptor())

        logger.info("WebSocket handlers registered successfully")
    }
}

@Component
class WebSocketHandshakeInterceptor : HandshakeInterceptor {

    private val logger = LoggerFactory.getLogger(WebSocketHandshakeInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val uri = request.uri.toString()
        logger.info("WebSocket handshake attempt: $uri")

        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        if (exception != null) {
            logger.error("WebSocket handshake failed: ${request.uri}", exception)
        } else {
            logger.info("WebSocket handshake successful: ${request.uri}")
        }
    }
}
