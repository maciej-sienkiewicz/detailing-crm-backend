package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.signature.api.dto.TabletListResponse
import com.carslab.crm.signature.service.TabletManagementService
import com.carslab.crm.signature.service.TabletPairingService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/tablets")
class TabletManagementController(
    private val tabletManagementService: TabletManagementService,
    private val pairingService: TabletPairingService,
    private val securityContext: SecurityContext
) : BaseController() {

    @GetMapping
    fun listCompanyTablets(): ResponseEntity<TabletListResponse> {
        logger.info("Listing tablets for company")
        val companyId = securityContext.getCurrentCompanyId()

        val tablets = tabletManagementService.listingAllTablets(companyId)

        return ok(TabletListResponse(
            success = true,
            tablets = tablets,
            totalCount = tablets.size,
            onlineCount = tablets.count { it.isOnline },
            timestamp = Instant.now()
        ))
    }

    @GetMapping("/{tabletId}")
    fun getTabletDetails(
        @PathVariable tabletId: UUID
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()

        logger.info("Getting tablet details: $tabletId for company: ${companyId}")

        val tabletDetails = tabletManagementService.getTabletDetailsWithStatus(tabletId, companyId)

        return if (tabletDetails != null) {
            ok(createSuccessResponse("Tablet details retrieved", tabletDetails))
        } else {
            ResponseEntity.notFound().build()
        }
    }


    @GetMapping("/{tabletId}/status")
    fun getTabletConnectionStatus(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()

        // Sprawdź czy tablet należy do firmy użytkownika
        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, companyId)
        if (!hasAccess) {
            return ResponseEntity.status(403).body(createErrorResponse("Access denied to tablet"))
        }

        val connectionStatus = tabletManagementService.getTabletConnectionStatus(tabletId)

        return ok(connectionStatus)
    }

    @PostMapping("/{tabletId}/test")
    fun testTablet(
        @PathVariable tabletId: UUID,
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()

        logger.info("Testing tablet: $tabletId by company: ${companyId}")

        // Sprawdź dostęp
        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, companyId)
        if (!hasAccess) {
            return ResponseEntity.status(403).body(createErrorResponse("Access denied to tablet"))
        }

        return try {
            val testResult = tabletManagementService.testTablet(tabletId)
            if (testResult) {
                ok(createSuccessResponse("Test request sent to tablet"))
            } else {
                badRequest("Failed to send test request - tablet may be offline")
            }
        } catch (e: Exception) {
            logger.error("Error testing tablet $tabletId", e)
            ResponseEntity.status(500).body(createErrorResponse("Failed to test tablet: ${e.message}"))
        }
    }

    @PostMapping("/{tabletId}/disconnect")
    fun disconnectTablet(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()

        logger.info("Disconnecting tablet: $tabletId by user: ${companyId}")

        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, companyId)
        if (!hasAccess) {
            return ResponseEntity.status(403).body(createErrorResponse("Access denied to tablet"))
        }

        return try {
            tabletManagementService.disconnectTablet(tabletId)
            ok(createSuccessResponse("Tablet disconnected successfully"))
        } catch (e: Exception) {
            logger.error("Error disconnecting tablet $tabletId", e)
            ResponseEntity.status(500).body(createErrorResponse("Failed to disconnect tablet: ${e.message}"))
        }
    }

    @DeleteMapping("/{tabletId}")
    fun unpairTablet(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()


        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, companyId)
        if (!hasAccess) {
            return ResponseEntity.status(403).body(createErrorResponse("Access denied to tablet"))
        }

        return try {
            tabletManagementService.unpairTablet(tabletId)
            ok(createSuccessResponse("Tablet unpaired successfully"))
        } catch (e: Exception) {
            logger.error("Error unpairing tablet $tabletId", e)
            ResponseEntity.status(500).body(createErrorResponse("Failed to unpair tablet: ${e.message}"))
        }
    }

    @GetMapping("/stats")
    fun getTabletsStats(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()

        val stats = tabletManagementService.getTabletStats(companyId)

        return ok(stats)
    }

    @PostMapping("/generate-pairing-code")
    fun generatePairingCode(
        authentication: Authentication,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val response = pairingService.initiateRegistration(companyId)

            ok(mapOf(
                "success" to true,
                "code" to response.code,
                "expiresIn" to response.expiresIn,
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error generating pairing code", e)
            ResponseEntity.status(500).body(createErrorResponse("Failed to generate pairing code: ${e.message}"))
        }
    }
    
    @PostMapping("/code/status")
    fun isCodeActive(@RequestBody request: CodeRequest): ResponseEntity<Boolean> =
        ok( pairingService.isCodeActive(request.code))

    private fun createErrorResponse(message: String): Map<String, Any> {
        return mapOf(
            "success" to false,
            "message" to message,
            "timestamp" to Instant.now()
        )
    }
}

data class CodeRequest(val code: String)
