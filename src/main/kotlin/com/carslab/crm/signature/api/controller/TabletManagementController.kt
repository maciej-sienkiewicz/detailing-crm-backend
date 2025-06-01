package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.signature.api.dto.TabletStatus
import com.carslab.crm.signature.service.TabletManagementService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin/tablets")
class TabletManagementController(
    private val tabletManagementService: TabletManagementService
) : BaseController() {

    @GetMapping
    fun listTenantTablets(authentication: Authentication): ResponseEntity<List<TabletStatus>> {
        val tenantId = getTenantId(authentication)

        val tablets = tabletManagementService.listTenantTablets(tenantId)

        return ok(tablets)
    }

    @PostMapping("/{tabletId}/test")
    fun testTablet(@PathVariable tabletId: UUID): ResponseEntity<Map<String, Any>> {
        logger.info("Testing tablet: $tabletId")

        tabletManagementService.testTablet(tabletId)

        return ok(createSuccessResponse("Test request sent to tablet"))
    }

    @GetMapping("/{tabletId}/status")
    fun getTabletStatus(@PathVariable tabletId: UUID): ResponseEntity<Map<String, Any>> {
        val isOnline = tabletManagementService.isTabletOnline(tabletId)

        return ok(mapOf(
            "tabletId" to tabletId,
            "isOnline" to isOnline,
            "timestamp" to java.time.Instant.now()
        ))
    }

    private fun getTenantId(authentication: Authentication): UUID {
        // Extract tenant ID from JWT or security context
        // This depends on your authentication implementation
        return authentication.details as? UUID
            ?: throw IllegalStateException("Tenant ID not found in authentication")
    }
}