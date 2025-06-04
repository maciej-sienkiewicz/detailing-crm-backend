
// src/main/kotlin/com/carslab/crm/signature/service/TabletPairingService.kt
package com.carslab.crm.signature.service

import com.carslab.crm.security.JwtService
import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.infrastructure.persistance.entity.*
import com.carslab.crm.signature.exception.InvalidPairingCodeException
import com.carslab.crm.signature.infrastructure.persistance.repository.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
@Transactional
class TabletPairingService(
    private val pairingCodeRepository: PairingCodeRepository,
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val workstationRepository: WorkstationRepository,
    private val jwtService: JwtService,
    @Value("\${app.websocket.base-url:ws://localhost:8080}") private val wsBaseUrl: String
) {

    private val secureRandom = SecureRandom()

    fun initiateRegistration(request: TabletRegistrationRequest): PairingCodeResponse {
        val code = generatePairingCode()
        val now = Instant.now()
        val expiresAt = now.plus(5, ChronoUnit.MINUTES)

        val pairingCode = PairingCode(
            code = code,
            companyId = request.companyId,
            locationId = request.locationId,
            workstationId = request.workstationId,
            deviceName = request.deviceName,
            expiresAt = expiresAt,
            createdAt = now
        )

        pairingCodeRepository.save(pairingCode)

        return PairingCodeResponse(
            code = code,
            expiresIn = 300, // 5 minutes
            expiresAt = expiresAt,
            companyId = request.companyId,
            locationId = request.locationId
        )
    }

    fun completeTabletPairing(request: TabletPairingRequest): TabletCredentials {
        val pairingData = pairingCodeRepository.findByCodeAndExpiresAtAfter(
            request.code,
            Instant.now()
        ) ?: throw InvalidPairingCodeException()

        // Generate secure device token
        val deviceToken = generateSecureToken()
        val now = Instant.now()

        // Create tablet device
        val tablet = TabletDevice(
            companyId = pairingData.companyId,
            locationId = pairingData.locationId,
            deviceToken = deviceToken,
            friendlyName = request.deviceName,
            workstationId = pairingData.workstationId,
            status = DeviceStatus.ACTIVE,
            lastSeen = now
        )

        val savedTablet = tabletDeviceRepository.save(tablet)

        // Update workstation if paired
        pairingData.workstationId?.let { workstationId ->
            val workstation = workstationRepository.findById(workstationId).orElse(null)
            workstation?.let {
                val updatedWorkstation = it.copy(
                    pairedTabletId = savedTablet.id!!,
                )
                workstationRepository.save(updatedWorkstation)
            }
        }

        // Mark pairing code as used
        pairingCodeRepository.markAsUsed(pairingData.code, now, savedTablet.id!!)

        // Generate JWT token for authentication
        val jwtToken = jwtService.generateTabletToken(savedTablet.id!!, savedTablet.companyId)

        return TabletCredentials(
            deviceId = savedTablet.id!!,
            deviceToken = jwtToken,
            websocketUrl = "$wsBaseUrl/ws/tablet/${savedTablet.id}",
            companyId = savedTablet.companyId,
            locationId = savedTablet.locationId
        )
    }

    private fun generatePairingCode(): String {
        return String.format("%06d", secureRandom.nextInt(1000000))
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return java.util.Base64.getEncoder().encodeToString(bytes)
    }

    @Transactional
    fun cleanupExpiredCodes() {
        val deletedCount = pairingCodeRepository.deleteExpiredCodes(Instant.now())
        if (deletedCount > 0) {
            println("Cleaned up $deletedCount expired pairing codes")
        }
    }

    /**
     * Get pairing statistics for company
     */
    fun getPairingStats(companyId: Long): Map<String, Any> {
        val now = Instant.now()
        val last24Hours = now.minus(24, ChronoUnit.HOURS)
        val last7Days = now.minus(7, ChronoUnit.DAYS)

        return mapOf(
            "codesGenerated24h" to pairingCodeRepository.countByCompanyIdAndCreatedAtAfter(companyId, last24Hours),
            "codesUsed24h" to pairingCodeRepository.countUsedCodesSince(companyId, last24Hours),
            "codesUsed7d" to pairingCodeRepository.countUsedCodesSince(companyId, last7Days),
            "activeTablets" to tabletDeviceRepository.countByCompanyIdAndStatus(companyId, DeviceStatus.ACTIVE)
        )
    }
}