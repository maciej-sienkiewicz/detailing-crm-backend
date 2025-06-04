package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.security.UserPrincipal
import com.carslab.crm.signature.api.dto.TabletStatus
import com.carslab.crm.signature.service.TabletManagementService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/tablets")
class TabletManagementController(
    private val tabletManagementService: TabletManagementService
) : BaseController() {

    /**
     * Pobierz listę wszystkich tabletów dla firmy użytkownika
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    fun listCompanyTablets(authentication: Authentication): ResponseEntity<List<TabletStatus>> {
        val userPrincipal = authentication.principal as UserPrincipal
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        logger.info("Listing tablets for company: ${userPrincipal.companyId}, tenant: $tenantId")

        val tablets = tabletManagementService.listTenantTablets(tenantId)

        return ok(tablets)
    }

    /**
     * Pobierz szczegóły konkretnego tabletu
     */
    @GetMapping("/{tabletId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    fun getTabletDetails(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserPrincipal
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        logger.info("Getting tablet details: $tabletId for company: ${userPrincipal.companyId}")

        val tablet = tabletManagementService.getTabletDetails(tabletId, tenantId)

        return if (tablet != null) {
            ok(mapOf(
                "tablet" to mapOf(
                    "id" to tablet.id.toString(),
                    "name" to tablet.friendlyName,
                    "location" to "Location ${tablet.locationId}",
                    "isOnline" to tabletManagementService.isTabletOnline(tabletId),
                    "lastSeen" to tablet.lastSeen.toString(),
                    "assignedWorkstation" to (tablet.workstationId?.toString() ?: null),
                    "status" to tablet.status.name
                ),
                "isOnline" to tabletManagementService.isTabletOnline(tabletId),
                "lastActivity" to tablet.lastSeen.toString(),
                "connectionStats" to tabletManagementService.getTabletConnectionStats(tabletId)
            ))
        } else {
            notFound()
        }
    }

    /**
     * Pobierz status połączenia tabletu
     */
    @GetMapping("/{tabletId}/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    fun getTabletConnectionStatus(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserPrincipal
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        // Sprawdź czy tablet należy do firmy użytkownika
        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, tenantId)
        if (!hasAccess) {
            return forbidden(createErrorResponse("Access denied to tablet"))
        }

        val isOnline = tabletManagementService.isTabletOnline(tabletId)
        val connectionStats = tabletManagementService.getTabletConnectionStats(tabletId)

        return ok(mapOf(
            "tabletId" to tabletId,
            "isOnline" to isOnline,
            "timestamp" to Instant.now(),
            "connectionStats" to connectionStats
        ))
    }

    /**
     * Wyślij test request do tabletu
     */
    @PostMapping("/{tabletId}/test")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    fun testTablet(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserPrincipal
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        logger.info("Testing tablet: $tabletId by user: ${userPrincipal.userUsername}")

        // Sprawdź dostęp
        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, tenantId)
        if (!hasAccess) {
            return forbidden(createErrorResponse("Access denied to tablet"))
        }

        return try {
            tabletManagementService.testTablet(tabletId)
            ok(createSuccessResponse("Test request sent to tablet"))
        } catch (e: Exception) {
            logger.error("Error testing tablet $tabletId", e)
            badRequest(createErrorResponse("Failed to test tablet: ${e.message}"))
        }
    }

    /**
     * Odłącz tablet (soft disconnect)
     */
    @PostMapping("/{tabletId}/disconnect")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    fun disconnectTablet(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserPrincipal
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        logger.info("Disconnecting tablet: $tabletId by user: ${userPrincipal.userUsername}")

        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, tenantId)
        if (!hasAccess) {
            return forbidden(createErrorResponse("Access denied to tablet"))
        }

        return try {
            tabletManagementService.disconnectTablet(tabletId)
            ok(createSuccessResponse("Tablet disconnected successfully"))
        } catch (e: Exception) {
            logger.error("Error disconnecting tablet $tabletId", e)
            badRequest(createErrorResponse("Failed to disconnect tablet: ${e.message}"))
        }
    }

    /**
     * Usuń tablet z systemu (unpair)
     */
    @DeleteMapping("/{tabletId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    fun unpairTablet(
        @PathVariable tabletId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserPrincipal
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        logger.info("Unpairing tablet: $tabletId by user: ${userPrincipal.userUsername}")

        val hasAccess = tabletManagementService.checkTabletAccess(tabletId, tenantId)
        if (!hasAccess) {
            return forbidden(createErrorResponse("Access denied to tablet"))
        }

        return try {
            tabletManagementService.unpairTablet(tabletId)
            ok(createSuccessResponse("Tablet unpaired successfully"))
        } catch (e: Exception) {
            logger.error("Error unpairing tablet $tabletId", e)
            badRequest(createErrorResponse("Failed to unpair tablet: ${e.message}"))
        }
    }

    /**
     * Pobierz statystyki tabletów dla firmy
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    fun getTabletsStats(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserPrincipal
        val tenantId = convertCompanyIdToTenantId(userPrincipal.companyId)

        val stats = tabletManagementService.getTabletStats(tenantId)

        return ok(stats)
    }

    /**
     * Wygeneruj kod parowania dla nowego tabletu
     */
    @PostMapping("/generate-pairing-code")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    fun generatePairingCode(
        authentication: Authentication,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        val userPrincipal = authentication.principal as UserPrincipal
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

            val pairingService = applicationContext.getBean(com.carslab.crm.signature.service.TabletPairingService::class.java)
            val response = pairingService.initiateRegistration(registrationRequest)

            ok(mapOf(
                "code" to response.code,
                "expiresIn" to response.expiresIn,
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error generating pairing code", e)
            badRequest(createErrorResponse("Failed to generate pairing code: ${e.message}"))
        }
    }

    private fun convertCompanyIdToTenantId(companyId: Long): UUID {
        // Temporary conversion - można zastąpić właściwym mapowaniem
        return UUID.fromString("${String.format("%08d", companyId)}-0000-0000-0000-000000000000")
    }

    private fun createSuccessResponse(message: String): Map<String, Any> {
        return mapOf(
            "success" to true,
            "message" to message,
            "timestamp" to Instant.now()
        )
    }

    private fun createErrorResponse(message: String): Map<String, Any> {
        return mapOf(
            "success" to false,
            "message" to message,
            "timestamp" to Instant.now()
        )
    }
}