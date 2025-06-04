package com.carslab.crm.signature.service

import com.carslab.crm.signature.websocket.SignatureRequestDto
import java.util.UUID

interface WebSocketService {
    fun isTabletConnected(tabletId: UUID): Boolean
    fun sendSignatureRequest(tabletId: UUID, request: SignatureRequestDto): Boolean
    fun notifyWorkstation(workstationId: Long, sessionId: String, success: Boolean, signedAt: java.time.Instant?)
    fun getActiveConnectionsCount(): Int
    fun getActiveTabletsCount(): Int
    fun getActiveWorkstationsCount(): Int
}