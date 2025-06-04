package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.service.TabletPairingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/tablets")
class TabletPairingController(
    private val tabletPairingService: TabletPairingService
) : BaseController() {

    /**
     * Initiate tablet registration and generate pairing code
     * This endpoint is typically called by the web interface
     */
    @PostMapping("/register")
    fun initiateRegistration(
        @Valid @RequestBody request: TabletRegistrationRequest
    ): ResponseEntity<ApiResponse<PairingCodeResponse>> {
        logger.info("Initiating tablet registration for company: ${request.companyId}")

        try {
            val response = tabletPairingService.initiateRegistration(request)

            logger.info("Generated pairing code: ${response.code}")
            return ok(ApiResponse(
                success = true,
                data = response,
                message = "Pairing code generated successfully",
                companyId = request.companyId
            ))
        } catch (e: Exception) {
            logger.error("Error initiating tablet registration", e)
            return ResponseEntity.status(500).body(
                ApiResponse<PairingCodeResponse>(
                    success = false,
                    error = "Failed to generate pairing code: ${e.message}",
                    companyId = request.companyId
                )
            )
        }
    }

    /**
     * Complete tablet pairing with provided code
     * This endpoint is called by the tablet app
     */
    @PostMapping("/pair")
    fun completeTabletPairing(
        @Valid @RequestBody request: TabletPairingRequest
    ): ResponseEntity<ApiResponse<TabletCredentials>> {
        logger.info("Completing tablet pairing with code: ${request.code}")

        try {
            val credentials = tabletPairingService.completeTabletPairing(request)

            logger.info("Tablet paired successfully: ${credentials.deviceId}")
            return created(ApiResponse(
                success = true,
                data = credentials,
                message = "Tablet paired successfully",
                companyId = credentials.companyId
            ))
        } catch (e: Exception) {
            logger.error("Error completing tablet pairing", e)
            return ResponseEntity.status(400).body(
                ApiResponse<TabletCredentials>(
                    success = false,
                    error = "Failed to pair tablet: ${e.message}"
                )
            )
        }
    }
}