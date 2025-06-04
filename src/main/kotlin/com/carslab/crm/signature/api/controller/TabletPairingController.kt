package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.service.TabletPairingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/tablets")
class TabletPairingController(
    private val tabletPairingService: TabletPairingService,
    private val securityContext: SecurityContext
) : BaseController() {

    @PostMapping("/register")
    fun initiateRegistration(): ResponseEntity<PairingCodeResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val response = tabletPairingService.initiateRegistration(companyId)

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