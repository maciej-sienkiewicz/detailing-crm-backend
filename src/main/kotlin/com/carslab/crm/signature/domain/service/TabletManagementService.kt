package com.carslab.crm.signature.service

import com.carslab.crm.signature.api.dto.TabletStatus
import com.carslab.crm.signature.exception.*
import com.carslab.crm.signature.infrastructure.persistance.entity.DeviceStatus
import com.carslab.crm.signature.infrastructure.persistance.entity.TabletDevice
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.infrastructure.persistance.repository.WorkstationRepository
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
            applicationContext.getBean("signatureWebSocketHandler",
                com.carslab.crm.signature.websocket.SignatureWebSocketHandler::class.java)
        } catch (e: Exception) {
            null // WebSocket handler might not be available in all contexts
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

            mapOf(
                "isConnected" to isOnline,
                "lastSeen" to (tablet?.lastSeen ?: Instant.now()),
                "status" to (tablet?.status?.name ?: "UNKNOWN"),
                "connectedSince" to if (isOnline) tablet?.lastSeen else null,
                "uptime" to if (isOnline && tablet != null) {
                    ChronoUnit.MINUTES.between(tablet.lastSeen, Instant.now())
                } else 0
            )
        } catch (e: Exception) {
            mapOf(
                "isConnected" to false,
                "error" to e.message
            )
        }
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

    fun testTablet(tabletId: UUID) {
        try {
            webSocketHandler?.sendSignatureRequest(
                tabletId,
                com.carslab.crm.signature.websocket.SignatureRequestDto(
                    sessionId = "test-${UUID.randomUUID()}",
                    tenantId = UUID.randomUUID(),
                    workstationId = UUID.randomUUID(),
                    customerName = "Test Customer",
                    vehicleInfo = com.carslab.crm.signature.websocket.VehicleInfoDto(
                        make = "Test",
                        model = "Test",
                        licensePlate = "TEST-123"
                    ),
                    serviceType = "Test Service",
                    documentType = "Test Document"
                )
            ) ?: throw RuntimeException("WebSocket handler not available")
        } catch (e: Exception) {
            throw RuntimeException("Failed to send test request to tablet $tabletId", e)
        }
    }

    private fun getLocationName(locationId: UUID): String {
        // TODO: Implement location service or add location table
        return "Location ${locationId.toString().take(8)}"
    }
}}