package com.carslab.crm.signature.service

import com.carslab.crm.signature.api.dto.TabletConnectionInfo
import com.carslab.crm.signature.api.dto.TabletDeviceDto
import com.carslab.crm.signature.api.dto.TabletStatus
import com.carslab.crm.signature.exception.*
import com.carslab.crm.signature.infrastructure.persistance.entity.DeviceStatus
import com.carslab.crm.signature.infrastructure.persistance.entity.TabletDevice
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.infrastructure.persistance.repository.WorkstationRepository
import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional(readOnly = true)
class TabletManagementService(
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val workstationRepository: WorkstationRepository,
    private val applicationContext: ApplicationContext
) {

    // Lazy initialization to break circular dependency
    private val webSocketHandler by lazy {
        try {
            applicationContext.getBean(SignatureWebSocketHandler::class.java)
        } catch (e: Exception) {
            null // WebSocket handler might not be available in all contexts
        }
    }

    /**
     * Get list of tablets for tenant with online status information
     */
    fun listTenantTabletsWithStatus(tenantId: UUID): List<TabletDeviceDto> {
        return tabletDeviceRepository.findByTenantId(tenantId).map { tablet ->
            val connectionInfo = getTabletConnectionInfo(tablet.id)

            TabletDeviceDto(
                id = tablet.id.toString(),
                tenantId = tablet.tenantId.toString(),
                locationId = tablet.locationId.toString(),
                friendlyName = tablet.friendlyName,
                workstationId = tablet.workstationId?.toString(),
                status = tablet.status.name,
                isOnline = isTabletOnline(tablet.id),
                lastSeen = tablet.lastSeen.toString(),
                createdAt = tablet.createdAt.toString(),
                connectionInfo = connectionInfo
            )
        }
    }

    /**
     * Get detailed tablet information with connection status
     */
    fun getTabletDetailsWithStatus(tabletId: UUID, tenantId: UUID): TabletDeviceDto? {
        val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)

        return if (tablet?.tenantId == tenantId) {
            val connectionInfo = getTabletConnectionInfo(tabletId)
            val workstationName = tablet.workstationId?.let {
                workstationRepository.findById(it).orElse(null)?.workstationName
            }

            TabletDeviceDto(
                id = tablet.id.toString(),
                tenantId = tablet.tenantId.toString(),
                locationId = tablet.locationId.toString(),
                friendlyName = tablet.friendlyName,
                workstationId = workstationName,
                status = tablet.status.name,
                isOnline = isTabletOnline(tabletId),
                lastSeen = tablet.lastSeen.toString(),
                createdAt = tablet.createdAt.toString(),
                connectionInfo = connectionInfo
            )
        } else null
    }

    /**
     * Get tablet connection information from WebSocket handler
     */
    private fun getTabletConnectionInfo(tabletId: UUID): TabletConnectionInfo? {
        return try {
            val connectionData = webSocketHandler?.getTabletConnectionInfo(tabletId)

            if (connectionData != null) {
                TabletConnectionInfo(
                    connectedAt = connectionData["connectedAt"]?.toString(),
                    lastHeartbeat = connectionData["lastHeartbeat"]?.toString(),
                    isAuthenticated = connectionData["isAuthenticated"] as? Boolean ?: false,
                    sessionOpen = connectionData["sessionOpen"] as? Boolean ?: false,
                    uptimeMinutes = connectionData["uptime"] as? Long
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun listTenantTablets(tenantId: UUID): List<TabletStatus> {
        return tabletDeviceRepository.findByTenantId(tenantId).map { tablet ->
            TabletStatus(
                id = tablet.id,
                name = tablet.friendlyName,
                location = getLocationName(tablet.locationId),
                isOnline = isTabletOnline(tablet.id),
                lastSeen = tablet.lastSeen,
                assignedWorkstation = tablet.workstationId?.let {
                    workstationRepository.findById(it).orElse(null)?.workstationName
                }
            )
        }
    }

    fun getTabletDetails(tabletId: UUID, tenantId: UUID): TabletDevice? {
        val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)
        return if (tablet?.tenantId == tenantId) tablet else null
    }

    fun checkTabletAccess(tabletId: UUID, tenantId: UUID): Boolean {
        val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)
        return tablet?.tenantId == tenantId
    }

    fun getTabletConnectionStats(tabletId: UUID): Map<String, Any> {
        return try {
            val isOnline = isTabletOnline(tabletId)
            val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)
            val connectionInfo = webSocketHandler?.getTabletConnectionInfo(tabletId)

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

    /**
     * Get tablet connection status for API response
     */
    fun getTabletConnectionStatus(tabletId: UUID): Map<String, Any> {
        val isOnline = isTabletOnline(tabletId)
        val connectionStats = getTabletConnectionStats(tabletId)

        return mapOf(
            "tabletId" to tabletId.toString(),
            "isOnline" to isOnline,
            "timestamp" to Instant.now().toString(),
            "connectionStats" to connectionStats
        )
    }

    @Transactional
    fun disconnectTablet(tabletId: UUID) {
        try {
            // Wysłij disconnect command przez WebSocket
            webSocketHandler?.disconnectTablet(tabletId)

            // Update status w bazie
            val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)
            tablet?.let {
                tabletDeviceRepository.save(it.copy(status = DeviceStatus.INACTIVE))
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to disconnect tablet $tabletId", e)
        }
    }

    @Transactional
    fun unpairTablet(tabletId: UUID) {
        try {
            // Disconnect WebSocket if connected
            if (isTabletOnline(tabletId)) {
                webSocketHandler?.disconnectTablet(tabletId)
            }

            // Remove from workstation assignment
            val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)
            tablet?.workstationId?.let { workstationId ->
                val workstation = workstationRepository.findById(workstationId).orElse(null)
                workstation?.let {
                    workstationRepository.save(it.copy(pairedTabletId = null))
                }
            }

            // Delete tablet from database
            tabletDeviceRepository.deleteById(tabletId)

        } catch (e: Exception) {
            throw RuntimeException("Failed to unpair tablet $tabletId", e)
        }
    }

    fun getTabletStats(tenantId: UUID): Map<String, Any> {
        val allTablets = tabletDeviceRepository.findByTenantId(tenantId)
        val onlineTablets = allTablets.filter { isTabletOnline(it.id) }
        val activeTablets = allTablets.filter { it.status == DeviceStatus.ACTIVE }

        return mapOf(
            "total" to allTablets.size,
            "online" to onlineTablets.size,
            "offline" to (allTablets.size - onlineTablets.size),
            "active" to activeTablets.size,
            "inactive" to allTablets.count { it.status == DeviceStatus.INACTIVE },
            "maintenance" to allTablets.count { it.status == DeviceStatus.MAINTENANCE },
            "error" to allTablets.count { it.status == DeviceStatus.ERROR },
            "connectedTablets" to onlineTablets.size,
            "lastUpdated" to Instant.now()
        )
    }

    fun isTabletOnline(tabletId: UUID): Boolean {
        return try {
            webSocketHandler?.isTabletConnected(tabletId) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun selectTablet(workstationId: UUID): TabletDevice? {
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
        return findNearestAvailableTablet(workstation.tenantId, workstation.locationId)
    }

    private fun findNearestAvailableTablet(tenantId: UUID, locationId: UUID): TabletDevice? {
        val fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES)

        return tabletDeviceRepository.findAvailableTablets(
            tenantId = tenantId,
            locationId = locationId,
            status = DeviceStatus.ACTIVE,
            lastSeenAfter = fiveMinutesAgo
        ).firstOrNull { isTabletOnline(it.id) }
    }

    @Transactional
    fun updateTabletLastSeen(tabletId: UUID) {
        tabletDeviceRepository.updateLastSeen(tabletId, Instant.now())
    }

    fun testTablet(tabletId: UUID): Boolean {
        return try {
            webSocketHandler?.pingTablet(tabletId) ?: false
        } catch (e: Exception) {
            throw RuntimeException("Failed to send test request to tablet $tabletId", e)
        }
    }

    private fun getLocationName(locationId: UUID): String {
        // TODO: Implement location service or add location table
        return "Location ${locationId.toString().take(8)}"
    }
}