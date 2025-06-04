package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.security.UserPrincipal
import com.carslab.crm.signature.api.dto.TabletDeviceDto
import com.carslab.crm.signature.api.dto.TabletListResponse
import com.carslab.crm.signature.service.TabletManagementService
import com.carslab.crm.signature.service.TabletPairingService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.adapter.WebHttpHandlerBuilder.applicationContext
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/tablets")
class TabletManagementController(
    private val tabletManagementService: TabletManagementService,
    private val pairingService: TabletPairingService,
) : BaseController() {

    /**
     * Pobierz listę wszystkich tabletów dla firmy użytkownika z informacją o statusie online
     */
    @GetMapping
    fun listCompanyTablets(authentication: Authentication): ResponseEntity<TabletListResponse> {
        val userPrincipal = authentication.principal as UserEntity
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        logger.info("Listing tablets for company: ${userPrincipal.companyId}, tenant: $tenantId")

        val tablets = tabletManagementService.listTenantTabletsWithStatus(tenantId)

        return ok(TabletListResponse(
            success = true,
            tablets = tablets,
            totalCount = tablets.size,
            onlineCount = tablets.count { it.isOnline },
            timestamp = Instant.now()
        ))
    }

    /**
     * Pobierz szczegóły konkretnego tabletu
     */
    @GetMapping("/{tabletId}")
    fun getTabletDetails(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserEntity
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        logger.info("Getting tablet details: $tabletId for company: ${userPrincipal.companyId}")

        val tabletDetails = tabletManagementService.getTabletDetailsWithStatus(tabletId, tenantId)

        return if (tabletDetails != null) {
            ok(createSuccessResponse("Tablet details retrieved", tabletDetails))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Pobierz status połączenia tabletu
     */
    @GetMapping("/{tabletId}/status")
    fun getTabletConnectionStatus(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserEntity
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        // Sprawdź czy tablet należy do firmy użytkownika
        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, tenantId)
        if (!hasAccess) {
            return ResponseEntity.status(403).body(createErrorResponse("Access denied to tablet"))
        }

        val connectionStatus = tabletManagementService.getTabletConnectionStatus(tabletId)

        return ok(connectionStatus)
    }

    /**
     * Wyślij test request do tabletu
     */
    @PostMapping("/{tabletId}/test")
    fun testTablet(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserEntity
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        logger.info("Testing tablet: $tabletId by user: ${userPrincipal.id}")

        // Sprawdź dostęp
        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, tenantId)
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

    /**
     * Odłącz tablet (soft disconnect)
     */
    @PostMapping("/{tabletId}/disconnect")
    fun disconnectTablet(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserEntity
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        logger.info("Disconnecting tablet: $tabletId by user: ${userPrincipal.id}")

        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, tenantId)
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

    /**
     * Usuń tablet z systemu (unpair)
     */
    @DeleteMapping("/{tabletId}")
    fun unpairTablet(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserEntity
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        logger.info("Unpairing tablet: $tabletId by user: ${userPrincipal.id}")

        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, tenantId)
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

    /**
     * Pobierz statystyki tabletów dla firmy
     */
    @GetMapping("/stats")
    fun getTabletsStats(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserEntity
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        val stats = tabletManagementService.getTabletStats(tenantId)

        return ok(stats)
    }

    /**
     * Wygeneruj kod parowania dla nowego tabletu
     */
    @PostMapping("/generate-pairing-code")
    fun generatePairingCode(
        authentication: Authentication,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserEntity
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        logger.info("Generating pairing code for company: ${userPrincipal.companyId}")

        return try {
            val locationId = UUID.fromString("12345678-0000-0000-0000-000000000002") // Default location
            val workstationId = request["workstationId"]?.toString()?.let { UUID.fromString(it) }

            val registrationRequest = com.carslab.crm.signature.api.dto.TabletRegistrationRequest(
                tenantId = tenantId,
                locationId = locationId,
                workstationId = workstationId
            )

            val response = pairingService.initiateRegistration(registrationRequest)

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

    private fun convertCompanyIdToTenantId(companyId: Long): UUID {
        // Temporary conversion - można zastąpić właściwym mapowaniem
        return UUID.fromString("${String.format("%08d", companyId)}-0000-0000-0000-000000000000")
    }

    private fun createErrorResponse(message: String): Map<String, Any> {
        return mapOf(
            "success" to false,
            "message" to message,
            "timestamp" to Instant.now()
        )
    }
}