package com.carslab.crm.signature.api.controller

import com.carslab.crm.signature.infrastructure.persistance.entity.TabletDevice
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/tablets")
class TabletPairingController {

    @PostMapping("/register")
    fun initiateRegistration(@RequestBody request: TabletRegistrationRequest): PairingCode {
        val code = generatePairingCode()

        redis.setex(
            "pairing:$code",
            300, // 5 minut
            TabletPairingData(
                tenantId = request.tenantId,
                locationId = request.locationId,
                workstationId = request.workstationId
            )
        )

        return PairingCode(code, expiresIn = 300)
    }

    @PostMapping("/pair")
    fun completeTabletPairing(@RequestBody request: TabletPairingRequest): TabletCredentials {
        val pairingData = redis.get("pairing:${request.code}")
            ?: throw InvalidPairingCodeException()

        val tablet = TabletDevice(
            id = UUID.randomUUID(),
            tenantId = pairingData.tenantId,
            locationId = pairingData.locationId,
            deviceToken = generateSecureToken(),
            friendlyName = request.deviceName,
            workstationId = pairingData.workstationId,
            status = DeviceStatus.ACTIVE,
            lastSeen = Instant.now()
        )

        tabletRepository.save(tablet)
        redis.del("pairing:${request.code}")

        return TabletCredentials(
            deviceId = tablet.id,
            deviceToken = tablet.deviceToken,
            websocketUrl = "wss://api.crm.com/ws/tablet/${tablet.id}"
        )
    }
}