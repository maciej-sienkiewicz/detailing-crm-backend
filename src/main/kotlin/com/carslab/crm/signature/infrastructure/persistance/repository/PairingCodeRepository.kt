package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.infrastructure.persistance.entity.PairingCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface PairingCodeRepository : JpaRepository<PairingCode, String> {

    fun findByCodeAndExpiresAtAfter(code: String, expiresAt: Instant): PairingCode?

    fun deleteByExpiresAtBefore(expiresAt: Instant)
}