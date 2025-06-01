package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.infrastructure.persistance.entity.SignatureSession
import com.carslab.crm.signature.infrastructure.persistance.entity.SignatureSessionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface SignatureSessionRepository : JpaRepository<SignatureSession, UUID> {

    fun findBySessionId(sessionId: String): SignatureSession?

    fun findByTenantIdAndStatus(tenantId: UUID, status: SignatureSessionStatus): List<SignatureSession>

    fun findByExpiresAtBeforeAndStatus(expiresAt: Instant, status: SignatureSessionStatus): List<SignatureSession>
}