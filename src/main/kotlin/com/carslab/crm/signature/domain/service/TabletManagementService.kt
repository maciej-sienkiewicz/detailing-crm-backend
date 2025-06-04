// src/main/kotlin/com/carslab/crm/signature/service/TabletManagementService.kt
package com.carslab.crm.signature.service

import com.carslab.crm.signature.api.dto.TabletConnectionInfo
import com.carslab.crm.signature.api.dto.TabletDeviceDto
import com.carslab.crm.signature.api.dto.TabletStatsResponse
import com.carslab.crm.signature.exception.*
import com.carslab.crm.signature.infrastructure.persistance.entity.DeviceStatus
import com.carslab.crm.signature.infrastructure.persistance.entity.TabletDevice
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.infrastructure.persistance.repository.WorkstationRepository
import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class TabletManagementService(
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val workstationRepository: WorkstationRepository,
    private val webSocketHandler: SignatureWebSocketHandler
) {

    /**
     * Get list of tablets for company with online status information
     */
    fun listCompanyTabletsWithStatus(companyId: Long, pageable: Pageable? = null): Page<TabletDeviceDto> {
        val tabletsPage = if (pageable != null) {
            tabletDeviceRepository.findByCompanyId(companyId, pageable)
        } else {
            // Convert list to page for consistency
            val tablets = tabletDeviceRepository.findByCompanyId(companyId)
            org.springframework.data.domain.PageImpl(tablets)
        }

        return tabletsPage.map { tablet ->
            mapToTabletDeviceDto(tablet)
        }
    }

    /**
     * Get tablets for company without pagination
     */
    fun listCompanyTablets(companyId: Long): List<TabletDeviceDto> {
        return tabletDeviceRepository.findByCompanyId(companyId)
            .map { tablet -> mapToTabletDeviceDto(tablet) }
    }

    /**
     * Get detailed tablet information with connection status
     */
    fun getTabletDetailsWithStatus(tabletId: Long, companyId: Long): TabletDeviceDto? {
        val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)

        return if (tablet?.companyId == companyId) {
            mapToTabletDeviceDto(tablet)
        } else null
    }

    /**
     * Get tablet connection information from WebSocket handler
     */
    private fun getTabletConnectionInfo(tabletId: Long): TabletConnectionInfo? {
        return try {
            val connectionData = webSocketHandler.getTabletConnectionInfo(tabletId)

            if (connectionData != null) {
                TabletConnectionInfo(
                    connectedAt = connectionData["connectedAt"] as? Instant,
                    lastHeartbeat = connectionData["lastHeartbeat"] as? Instant,
                    isAuthenticated = connectionData["isAuthenticated"] as? Boolean ?: false,
                    sessionOpen = connectionData["sessionOpen"] as? Boolean ?: false,
                    uptimeMinutes = connectionData["uptime"] as? Long,
                    signalStrength = connectionData["signalStrength"] as? String,
                    batteryLevel = connectionData["batteryLevel"] as? Int
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if user has access to tablet
     */
    fun checkTabletAccess(tabletId: Long, companyId: Long): Boolean {
        val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)
        return tablet?.companyId == companyId
    }

    /**
     * Get tablet connection statistics
     */
    fun getTabletConnectionStatus(tabletId: Long): Map<String, Any> {
        val isOnline = isTabletOnline(tabletId)
        val connectionStats = getTabletConnectionStats(tabletId)

        return mapOf(
            "tabletId" to tabletId,
            "isOnline" to isOnline,
            "timestamp" to Instant.now(),
            "connectionStats" to connectionStats
        )
    }

    fun getTabletConnectionStats(tabletId: Long): Map<String, Any> {
        return try {
            val isOnline = isTabletOnline(tabletId)
            val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)
            val connectionInfo = webSocketHandler.getTabletConnectionInfo(tabletId)

            mapOf<String, Any>(
                "isConnected" to isOnline,
                "lastSeen" to (tablet?.lastSeen ?: Instant.now()),
                "status" to (tablet?.status?.name ?: "UNKNOWN"),
                "connectedSince" to (connectionInfo?.get("connectedAt") ?: "Never"),
                "lastHeartbeat" to (connectionInfo?.get("lastHeartbeat") ?: "Never"),
                "isAuthenticated" to (connectionInfo?.get("isAuthenticated") ?: false),
                "sessionOpen" to (connectionInfo?.get("sessionOpen") ?: false),
                "uptime" to (connectionInfo?.get("uptime") ?: 0)
            )
        } catch (e: Exception) {
            mapOf<String, Any>(
                "isConnected" to false,
                "error" to (e.message ?: "Unknown error")
            )
        }
    }

    @Transactional
    fun disconnectTablet(tabletId: Long, companyId: Long) {
        if (!checkTabletAccess(tabletId, companyId)) {
            throw TabletNotFoundException(tabletId)
        }

        try {
            // Send disconnect command through WebSocket
            webSocketHandler.disconnectTablet(tabletId)

            // Update status in database
            tabletDeviceRepository.updateStatus(tabletId, DeviceStatus.INACTIVE)
        } catch (e: Exception) {
            throw RuntimeException("Failed to disconnect tablet $tabletId", e)
        }
    }

    @Transactional
    fun unpairTablet(tabletId: Long, companyId: Long) {
        if (!checkTabletAccess(tabletId, companyId)) {
            throw TabletNotFoundException(tabletId)
        }

        try {
            // Disconnect WebSocket if connected
            if (isTabletOnline(tabletId)) {
                webSocketHandler.disconnectTablet(tabletId)
            }

            val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)

            // Remove from workstation assignment
            tablet?.workstationId?.let { workstationId ->
                val workstation = workstationRepository.findById(workstationId).orElse(null)
                workstation?.let {
                    val updatedWorkstation = it.copy(pairedTabletId = null, updatedAt = Instant.now())
                    workstationRepository.save(updatedWorkstation)
                }
            }

            // Delete tablet from database
            tabletDeviceRepository.deleteById(tabletId)

        } catch (e: Exception) {
            throw RuntimeException("Failed to unpair tablet $tabletId", e)
        }
    }

    fun getTabletStats(companyId: Long): TabletStatsResponse {
        val allTablets = tabletDeviceRepository.findByCompanyId(companyId)
        val onlineTablets = allTablets.filter { isTabletOnline(it.id!!) }
        val activeTablets = allTablets.filter { it.status == DeviceStatus.ACTIVE }

        // Get status distribution
        val statusDistribution = allTablets.groupBy { it.status.name }
            .mapValues { it.value.size }

        // Get location distribution
        val locationDistribution = allTablets.groupBy { it.locationId }
            .mapValues { it.value.size }

        // Get session counts (would need to inject SignatureSessionRepository)
        val today = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val weekAgo = today.minus(7, ChronoUnit.DAYS)
        val monthAgo = today.minus(30, ChronoUnit.DAYS)

        return TabletStatsResponse(
            companyId = companyId,
            totalTablets = allTablets.size,
            onlineTablets = onlineTablets.size,
            activeTablets = activeTablets.size,
            tabletsByStatus = statusDistribution,
            tabletsByLocation = locationDistribution,
            sessionsToday = 0, // TODO: Implement with session repository
            sessionsThisWeek = 0,
            sessionsThisMonth = 0,
            timestamp = Instant.now()
        )
    }

    fun isTabletOnline(tabletId: Long): Boolean {
        return try {
            webSocketHandler.isTabletConnected(tabletId)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Select best available tablet for workstation
     */
    fun selectTabletForWorkstation(workstationId: Long): TabletDevice? {
        val workstation = workstationRepository.findById(workstationId).orElseThrow {
            WorkstationNotFoundException(workstationId)
        }

        // 1. Check assigned tablet first
        workstation.pairedTabletId?.let { tabletId ->
            val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)
            if (tablet?.status == DeviceStatus.ACTIVE && isTabletOnline(tabletId)) {
                return tablet
            }
        }

        // 2. Find nearest available tablet
        return findNearestAvailableTablet(workstation.companyId, workstation.locationId)
    }

    private fun findNearestAvailableTablet(companyId: Long, locationId: Long): TabletDevice? {
        val fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES)

        return tabletDeviceRepository.findAvailableTablets(
            companyId = companyId,
            locationId = locationId,
            status = DeviceStatus.ACTIVE,
            lastSeenAfter = fiveMinutesAgo
        ).firstOrNull { isTabletOnline(it.id!!) }
    }

    @Transactional
    fun updateTabletLastSeen(tabletId: Long) {
        tabletDeviceRepository.updateLastSeen(tabletId, Instant.now())
    }

    fun testTablet(tabletId: Long, companyId: Long): Boolean {
        if (!checkTabletAccess(tabletId, companyId)) {
            throw TabletNotFoundException(tabletId)
        }

        return try {
            webSocketHandler.pingTablet(tabletId)
        } catch (e: Exception) {
            throw RuntimeException("Failed to send test request to tablet $tabletId", e)
        }
    }

    /**
     * Map entity to DTO
     */
    private fun mapToTabletDeviceDto(tablet: TabletDevice): TabletDeviceDto {
        val connectionInfo = getTabletConnectionInfo(tablet.id!!)
        val workstationName = tablet.workstationId?.let {
            workstationRepository.findById(it).orElse(null)?.workstationName
        }

        return TabletDeviceDto(
            id = tablet.id!!,
            companyId = tablet.companyId,
            locationId = tablet.locationId,
            friendlyName = tablet.friendlyName,
            workstationId = tablet.workstationId,
            workstationName = workstationName,
            status = tablet.status.name,
            isOnline = isTabletOnline(tablet.id!!),
            lastSeen = tablet.lastSeen,
            createdAt = tablet.createdAt,
            updatedAt = tablet.updatedAt,
            connectionInfo = connectionInfo
        )
    }
}