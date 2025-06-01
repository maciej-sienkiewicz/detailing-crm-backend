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
        applicationContext.getBean("signatureWebSocketHandler",
            com.carslab.crm.signature.websocket.SignatureWebSocketHandler::class.java)
    }

    fun listTenantTablets(tenantId: UUID): List<TabletStatus> {
        return tabletDeviceRepository.findByTenantId(tenantId).map { tablet ->
            TabletStatus(
                id = tablet.id,
                name = tablet.friendlyName,
                location = "Location ${tablet.locationId}", // TODO: Fetch actual location name
                isOnline = isTabletOnline(tablet.id),
                lastSeen = tablet.lastSeen,
                assignedWorkstation = tablet.workstationId?.let {
                    workstationRepository.findById(it).orElse(null)?.workstationName
                }
            )
        }
    }

    fun isTabletOnline(tabletId: UUID): Boolean {
        return try {
            webSocketHandler.isTabletConnected(tabletId)
        } catch (e: Exception) {
            // If WebSocket handler is not available yet, assume offline
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
            // Create a test signature request to send to the tablet
            webSocketHandler.sendSignatureRequest(
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
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to send test request to tablet $tabletId", e)
        }
    }
}