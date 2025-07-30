package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.api.dto.DocumentStatus
import com.carslab.crm.signature.api.dto.SignatureSessionStatus
import com.carslab.crm.signature.infrastructure.persistance.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface DocumentSignatureSessionRepository : JpaRepository<DocumentSignatureSession, UUID> {

    fun findBySessionIdAndCompanyId(sessionId: UUID, companyId: Long): DocumentSignatureSession?
    fun findBySessionId(sessionId: UUID): DocumentSignatureSession?
}