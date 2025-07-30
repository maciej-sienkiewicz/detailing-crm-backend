package com.carslab.crm.signature.service

import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class TabletConnectionService(
    private val tabletDeviceRepository: TabletDeviceRepository
) {

    @Transactional
    fun updateTabletLastSeen(tabletId: UUID) {
        try {
            tabletDeviceRepository.updateLastSeen(tabletId, Instant.now())
        } catch (e: Exception) {
            // Log error but don't fail the WebSocket operation
            println("Failed to update tablet last seen for $tabletId: ${e.message}")
        }
    }
}