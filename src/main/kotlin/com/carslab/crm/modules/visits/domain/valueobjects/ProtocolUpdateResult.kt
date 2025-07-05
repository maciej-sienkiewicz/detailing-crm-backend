package com.carslab.crm.modules.visits.domain.valueobjects

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus

data class ProtocolUpdateResult(
    val protocolId: ProtocolId,
    val oldStatus: ProtocolStatus,
    val newStatus: ProtocolStatus,
    val servicesUpdateResult: ServicesUpdateResult,
    val statusChangeResult: StatusChangeResult,
    val updatedProtocol: CarReceptionProtocol
) {
    fun buildChangedFieldsMap(): Map<String, Pair<String?, String?>> {
        val changes = mutableMapOf<String, Pair<String?, String?>>()

        if (oldStatus != newStatus) {
            changes["status"] = Pair(oldStatus.name, newStatus.name)
        }

        if (servicesUpdateResult.hasChanges()) {
            changes["servicesCount"] = Pair(null, servicesUpdateResult.updatedCount.toString())
        }

        return changes
    }
}

data class ServicesUpdateResult(
    val updatedCount: Int,
    val serviceIds: List<String>,
    val totalAmount: Double,
    val serviceNames: List<String>
) {
    fun hasChanges(): Boolean = updatedCount > 0

    companion object {
        fun success(updatedCount: Int, serviceIds: List<String>, totalAmount: Double): ServicesUpdateResult {
            return ServicesUpdateResult(updatedCount, serviceIds, totalAmount, serviceIds.map { "Service $it" })
        }

        fun noChanges(): ServicesUpdateResult {
            return ServicesUpdateResult(0, emptyList(), 0.0, emptyList())
        }
    }
}

sealed class StatusChangeResult {
    data class VisitStarted(val clientId: Long) : StatusChangeResult()
    data class StatusChanged(val oldStatus: ProtocolStatus, val newStatus: ProtocolStatus) : StatusChangeResult()
    object NoChange : StatusChangeResult()

    fun hasChanged(): Boolean = this !is NoChange

    companion object {
        fun visitStarted(clientId: Long) = VisitStarted(clientId)
        fun statusChanged(old: ProtocolStatus, new: ProtocolStatus) = StatusChanged(old, new)
        fun noChange() = NoChange
    }
}
