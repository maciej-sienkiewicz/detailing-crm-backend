package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.signature.domain.service.TabletPairingService
import com.carslab.crm.signature.api.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/tablets")
class TabletPairingController(
    private val tabletPairingService: TabletPairingService
) : BaseController() {

    @PostMapping("/register")
    fun initiateRegistration(
        @Valid @RequestBody request: TabletRegistrationRequest
    ): ResponseEntity<PairingCodeResponse> {
        logger.info("Initiating tablet registration for tenant: ${request.tenantId}")

        val response = tabletPairingService.initiateRegistration(request)

        logger.info("Generated pairing code: ${response.code}")
        return ok(response)
    }

    @PostMapping("/pair")
    fun completeTabletPairing(
        @Valid @RequestBody request: TabletPairingRequest
    ): ResponseEntity<TabletCredentials> {
        logger.info("Completing tablet pairing with code: ${request.code}")

        val credentials = tabletPairingService.completeTabletPairing(request)

        logger.info("Tablet paired successfully: ${credentials.deviceId}")
        return created(credentials)
    }
}