package com.carslab.crm.signature.service

import com.carslab.crm.signature.api.dto.TabletConnectionInfo
import com.carslab.crm.signature.api.dto.TabletDeviceDto
import com.carslab.crm.signature.infrastructure.persistance.entity.DeviceStatus
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.infrastructure.persistance.repository.WorkstationRepository
import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional(readOnly = true)
class TabletManagementService(
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val workstationRepository: WorkstationRepository,
    private val applicationContext: ApplicationContext
) {

    private val webSocketHandler by lazy {
        try {
            applicationContext.getBean(SignatureWebSocketHandler::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun listingAllTablets(companyId: Long): List<TabletDeviceDto> {
        return tabletDeviceRepository.findByCompanyId(companyId).map { tablet ->
            val connectionInfo = getTabletConnectionInfo(tablet.id)

            TabletDeviceDto(
                id = tablet.id.toString(),
                companyId = tablet.companyId,
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

    fun getTabletDetailsWithStatus(tabletId: UUID, companyId: Long): TabletDeviceDto? {
        val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)

        return if (tablet?.companyId == companyId) {
            val connectionInfo = getTabletConnectionInfo(tabletId)
            val workstationName = tablet.workstationId?.let {
                workstationRepository.findById(it).orElse(null)?.workstationName
            }

            TabletDeviceDto(
                id = tablet.id.toString(),
                companyId = tablet.companyId,
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

    fun getTabletStats(companyId: Long): Map<String, Any> {
        val allTablets = tabletDeviceRepository.findByCompanyId(companyId)
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

    fun checkTabletAccess(tabletId: UUID, companyId: Long): Boolean {
        val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)
        return tablet?.companyId == companyId
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
}