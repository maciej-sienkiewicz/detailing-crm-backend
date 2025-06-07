package com.carslab.crm.signature.service

import com.carslab.crm.signature.websocket.SignatureRequestDto
import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class WebSocketServiceImpl(
    private val signatureWebSocketHandler: SignatureWebSocketHandler
) : WebSocketService {

    override fun isTabletConnected(tabletId: UUID): Boolean {
        return signatureWebSocketHandler.isTabletConnected(tabletId)
    }

    override fun sendSignatureRequest(tabletId: UUID, request: SignatureRequestDto): Boolean {
        return signatureWebSocketHandler.sendSignatureRequest(tabletId, request)
    }

    override fun notifyWorkstation(workstationId: UUID, sessionId: String, success: Boolean, signedAt: Instant?) {
        signatureWebSocketHandler.notifyWorkstation(workstationId, sessionId, success, signedAt)
    }

    override fun getActiveConnectionsCount(): Int {
        return signatureWebSocketHandler.getActiveConnectionsCount()
    }

    override fun getActiveTabletsCount(): Int {
        return signatureWebSocketHandler.getActiveTabletsCount()
    }

    override fun getActiveWorkstationsCount(): Int {
        return signatureWebSocketHandler.getActiveWorkstationsCount()
    }
}