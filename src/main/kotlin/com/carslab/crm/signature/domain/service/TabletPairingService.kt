package com.carslab.crm.signature.domain.service

import com.carslab.crm.signature.infrastructure.persistance.entity.*
import com.carslab.crm.signature.infrastructure.persistance.repository.*
import com.carslab.crm.signature.infrastructure.exception.InvalidPairingCodeException
import com.carslab.crm.signature.api.dto.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class TabletPairingService(
    private val pairingCodeRepository: PairingCodeRepository,
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val workstationRepository: WorkstationRepository
) {

    private val secureRandom = SecureRandom()

    fun initiateRegistration(request: TabletRegistrationRequest): PairingCodeResponse {
        val code = generatePairingCode()
        val expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES)

        val pairingCode = PairingCode(
            code = code,
            tenantId = request.tenantId,
            locationId = request.locationId,
            workstationId = request.workstationId,
            expiresAt = expiresAt
        )

        pairingCodeRepository.save(pairingCode)

        return PairingCodeResponse(
            code = code,
            expiresIn = 300
        )
    }

    fun completeTabletPairing(request: TabletPairingRequest): TabletCredentials {
        val pairingData = pairingCodeRepository.findByCodeAndExpiresAtAfter(
            request.code,
            Instant.now()
        ) ?: throw InvalidPairingCodeException()

        val tablet = TabletDevice(
            tenantId = pairingData.tenantId,
            locationId = pairingData.locationId,
            deviceToken = generateSecureToken(),
            friendlyName = request.deviceName,
            workstationId = pairingData.workstationId,
            status = DeviceStatus.ACTIVE
        )

        val savedTablet = tabletDeviceRepository.save(tablet)

        // Update workstation if paired
        pairingData.workstationId?.let { workstationId ->
            val workstation = workstationRepository.findById(workstationId).orElse(null)
            workstation?.let {
                workstationRepository.save(it.copy(pairedTabletId = savedTablet.id))
            }
        }

        pairingCodeRepository.delete(pairingData)

        return TabletCredentials(
            deviceId = savedTablet.id,
            deviceToken = savedTablet.deviceToken,
            websocketUrl = "wss://api.crm.com/ws/tablet/${savedTablet.id}"
        )
    }

    private fun generatePairingCode(): String {
        return String.format("%06d", secureRandom.nextInt(1000000))
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun cleanupExpiredCodes() {
        pairingCodeRepository.deleteByExpiresAtBefore(Instant.now())
    }
}