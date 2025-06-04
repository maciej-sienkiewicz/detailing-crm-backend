// src/main/kotlin/com/carslab/crm/signature/api/controller/TabletManagementController.kt
package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.service.TabletManagementService
import com.carslab.crm.signature.service.TabletPairingService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/tablets")
class TabletManagementController(
    private val tabletManagementService: TabletManagementService,
    private val pairingService: TabletPairingService,
) : BaseController() {

    /**
     * Get list of all tablets for user's company with online status information
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    fun listCompanyTablets(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "lastSeen") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): ResponseEntity<PaginatedApiResponse<TabletDeviceDto>> {
        val userPrincipal = authentication.principal as UserEntity
        val companyId = userPrincipal.companyId

        logger.info("Listing tablets for company: $companyId")

        try {
            val sort = if (sortDir.lowercase() == "desc") {
                Sort.by(sortBy).descending()
            } else {
                Sort.by(sortBy).ascending()
            }

            val pageable = PageRequest.of(page, size, sort)
            val tabletsPage = tabletManagementService.listCompanyTabletsWithStatus(companyId, pageable)

            val response = PaginatedApiResponse(
                success = true,
                data = tabletsPage.content,
                pagination = PaginationInfo(
                    currentPage = page,
                    pageSize = size,
                    totalItems = tabletsPage.totalElements,
                    totalPages = tabletsPage.totalPages,
                    hasNext = tabletsPage.hasNext(),
                    hasPrevious = tabletsPage.hasPrevious()
                ),
                companyId = companyId
            )

            return ok(response)
        } catch (e: Exception) {
            logger.error("Error listing tablets for company $companyId", e)
            val errorResponse = PaginatedApiResponse<TabletDeviceDto>(
                success = false,
                data = emptyList(),
                pagination = PaginationInfo(0, size, 0, 0, false, false),
                error = "Failed to retrieve tablets: ${e.message}",
                companyId = companyId
            )
            return ResponseEntity.status(500).body(errorResponse)
        }
    }

    /**
     * Get details of specific tablet
     */
    @GetMapping("/{tabletId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    fun getTabletDetails(
        @PathVariable tabletId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<TabletDeviceDto>> {
        val userPrincipal = authentication.principal as UserEntity
        val companyId = userPrincipal.companyId

        logger.info("Getting tablet details: $tabletId for company: $companyId")

        try {
            val tabletDetails = tabletManagementService.getTabletDetailsWithStatus(tabletId, companyId)

            return if (tabletDetails != null) {
                ok(ApiResponse(
                    success = true,
                    data = tabletDetails,
                    message = "Tablet details retrieved successfully",
                    companyId = companyId
                ))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error getting tablet details for tablet $tabletId", e)
            return ResponseEntity.status(500).body(
                ApiResponse<TabletDeviceDto>(
                    success = false,
                    error = "Failed to retrieve tablet details: ${e.message}",
                    companyId = companyId
                )
            )
        }
    }

    /**
     * Get tablet connection status
     */
    @GetMapping("/{tabletId}/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    fun getTabletConnectionStatus(
        @PathVariable tabletId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val userPrincipal = authentication.principal as UserEntity
        val companyId = userPrincipal.companyId

        try {
            // Check access
            val hasAccess = tabletManagementService.checkTabletAccess(tabletId, companyId)
            if (!hasAccess) {
                return ResponseEntity.status(403).body(
                    ApiResponse<Map<String, Any>>(
                        success = false,
                        error = "Access denied to tablet",
                        companyId = companyId
                    )
                )
            }

            val connectionStatus = tabletManagementService.getTabletConnectionStatus(tabletId)

            return ok(ApiResponse(
                success = true,
                data = connectionStatus,
                companyId = companyId
            ))
        } catch (e: Exception) {
            logger.error("Error getting connection status for tablet $tabletId", e)
            return ResponseEntity.status(500).body(
                ApiResponse<Map<String, Any>>(
                    success = false,
                    error = "Failed to get connection status: ${e.message}",
                    companyId = companyId
                )
            )
        }
    }

    /**
     * Send test request to tablet
     */
    @PostMapping("/{tabletId}/test")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    fun testTablet(
        @PathVariable tabletId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<String>> {
        val userPrincipal = authentication.principal as UserEntity
        val companyId = userPrincipal.companyId

        logger.info("Testing tablet: $tabletId by user: ${userPrincipal.id}")

        try {
            val testResult = tabletManagementService.testTablet(tabletId, companyId)

            return if (testResult) {
                ok(ApiResponse(
                    success = true,
                    data = "Test request sent successfully",
                    message = "Tablet responded to test request",
                    companyId = companyId
                ))
            } else {
                badRequest("Failed to send test request - tablet may be offline")
            }
        } catch (e: Exception) {
            logger.error("Error testing tablet $tabletId", e)
            return ResponseEntity.status(500).body(
                ApiResponse<String>(
                    success = false,
                    error = "Failed to test tablet: ${e.message}",
                    companyId = companyId
                )
            )
        }
    }

    /**
     * Disconnect tablet (soft disconnect)
     */
    @PostMapping("/{tabletId}/disconnect")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    fun disconnectTablet(
        @PathVariable tabletId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<String>> {
        val userPrincipal = authentication.principal as UserEntity
        val companyId = userPrincipal.companyId

        logger.info("Disconnecting tablet: $tabletId by user: ${userPrincipal.id}")

        try {
            tabletManagementService.disconnectTablet(tabletId, companyId)

            return ok(ApiResponse(
                success = true,
                data = "Tablet disconnected successfully",
                message = "Tablet has been disconnected and marked as inactive",
                companyId = companyId
            ))
        } catch (e: Exception) {
            logger.error("Error disconnecting tablet $tabletId", e)
            return ResponseEntity.status(500).body(
                ApiResponse<String>(
                    success = false,
                    error = "Failed to disconnect tablet: ${e.message}",
                    companyId = companyId
                )
            )
        }
    }

    /**
     * Remove tablet from system (unpair)
     */
    @DeleteMapping("/{tabletId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    fun unpairTablet(
        @PathVariable tabletId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<String>> {
        val userPrincipal = authentication.principal as UserEntity
        val companyId = userPrincipal.companyId

        logger.info("Unpairing tablet: $tabletId by user: ${userPrincipal.id}")

        try {
            tabletManagementService.unpairTablet(tabletId, companyId)

            return ok(ApiResponse(
                success = true,
                data = "Tablet unpaired successfully",
                message = "Tablet has been removed from the system",
                companyId = companyId
            ))
        } catch (e: Exception) {
            logger.error("Error unpairing tablet $tabletId", e)
            return ResponseEntity.status(500).body(
                ApiResponse<String>(
                    success = false,
                    error = "Failed to unpair tablet: ${e.message}",
                    companyId = companyId
                )
            )
        }
    }

    /**
     * Get tablet statistics for company
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    fun getTabletsStats(authentication: Authentication): ResponseEntity<ApiResponse<TabletStatsResponse>> {
        val userPrincipal = authentication.principal as UserEntity
        val companyId = userPrincipal.companyId

        try {
            val stats = tabletManagementService.getTabletStats(companyId)

            return ok(ApiResponse(
                success = true,
                data = stats,
                companyId = companyId
            ))
        } catch (e: Exception) {
            logger.error("Error getting tablet stats for company $companyId", e)
            return ResponseEntity.status(500).body(
                ApiResponse<TabletStatsResponse>(
                    success = false,
                    error = "Failed to get tablet statistics: ${e.message}",
                    companyId = companyId
                )
            )
        }
    }

    /**
     * Generate pairing code for new tablet
     */
    @PostMapping("/generate-pairing-code")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    fun generatePairingCode(
        authentication: Authentication,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<PairingCodeResponse>> {
        val userPrincipal = authentication.principal as UserEntity
        val companyId = userPrincipal.companyId

        logger.info("Generating pairing code for company: $companyId")

        try {
            // Extract optional parameters
            val locationId = (request["locationId"] as? Number)?.toLong() ?: 1L // Default location
            val workstationId = (request["workstationId"] as? Number)?.toLong()
            val deviceName = request["deviceName"] as? String

            val registrationRequest = TabletRegistrationRequest(
                companyId = companyId,
                locationId = locationId,
                workstationId = workstationId,
                deviceName = deviceName
            )

            val response = pairingService.initiateRegistration(registrationRequest)

            return ok(ApiResponse(
                success = true,
                data = response,
                message = "Pairing code generated successfully",
                companyId = companyId
            ))
        } catch (e: Exception) {
            logger.error("Error generating pairing code for company $companyId", e)
            return ResponseEntity.status(500).body(
                ApiResponse<PairingCodeResponse>(
                    success = false,
                    error = "Failed to generate pairing code: ${e.message}",
                    companyId = companyId
                )
            )
        }
    }

    /**
     * Get pairing statistics
     */
    @GetMapping("/pairing-stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    fun getPairingStats(authentication: Authentication): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val userPrincipal = authentication.principal as UserEntity
        val companyId = userPrincipal.companyId

        try {
            val stats = pairingService.getPairingStats(companyId)

            return ok(ApiResponse(
                success = true,
                data = stats,
                companyId = companyId
            ))
        } catch (e: Exception) {
            logger.error("Error getting pairing stats for company $companyId", e)
            return ResponseEntity.status(500).body(
                ApiResponse<Map<String, Any>>(
                    success = false,
                    error = "Failed to get pairing statistics: ${e.message}",
                    companyId = companyId
                )
            )
        }
    }
}