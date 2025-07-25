package com.carslab.crm.modules.visits.domain.events

import com.carslab.crm.infrastructure.events.BaseDomainEvent
import com.carslab.crm.modules.clients.domain.model.VehicleId
import java.math.BigDecimal

/**
 * Event: Zmiana statusu protokołu
 */
data class ProtocolStatusChangedEvent(
    val protocolId: String,
    val oldStatus: String,
    val newStatus: String,
    val reason: String?,
    val protocolTitle: String? = null,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = protocolId,
    aggregateType = "PROTOCOL",
    eventType = "PROTOCOL_STATUS_CHANGED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "oldStatus" to oldStatus,
        "newStatus" to newStatus,
        "reason" to reason,
        "protocolTitle" to protocolTitle
    ) + additionalMetadata
)

/**
 * Event: Aktualizacja protokołu
 */
data class ProtocolUpdatedEvent(
    val protocolId: String,
    val protocolTitle: String,
    val changedFields: Map<String, Pair<String?, String?>>,
    val updateReason: String?,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = protocolId,
    aggregateType = "PROTOCOL",
    eventType = "PROTOCOL_UPDATED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "protocolTitle" to protocolTitle,
        "changedFields" to changedFields,
        "updateReason" to updateReason
    ) + additionalMetadata
)

/**
 * Event: Przejście protokołu do gotowości odbioru
 */
data class ProtocolReadyForPickupEvent(
    val protocolId: String,
    val protocolTitle: String,
    val clientId: String?,
    val clientName: String?,
    val vehicleInfo: String,
    val totalAmount: Double,
    val servicesCompleted: List<String>,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = protocolId,
    aggregateType = "PROTOCOL",
    eventType = "PROTOCOL_READY_FOR_PICKUP",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "protocolTitle" to protocolTitle,
        "clientId" to clientId,
        "clientName" to clientName,
        "vehicleInfo" to vehicleInfo,
        "totalAmount" to totalAmount,
        "servicesCompleted" to servicesCompleted
    ) + additionalMetadata
)

/**
 * Event: Rozpoczęcie pracy nad protokołem
 */
data class ProtocolWorkStartedEvent(
    val protocolId: String,
    val protocolTitle: String,
    val clientId: String,
    val clientName: String,
    val vehicleId: String,
    val vehicleDisplayName: String,
    val assignedTechnicians: List<String>,
    val estimatedCompletionTime: String?,
    val plannedServices: List<String>,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = protocolId,
    aggregateType = "PROTOCOL",
    eventType = "PROTOCOL_WORK_STARTED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "protocolTitle" to protocolTitle,
        "assignedTechnicians" to assignedTechnicians,
        "estimatedCompletionTime" to estimatedCompletionTime,
        "plannedServices" to plannedServices
    ) + additionalMetadata
)

/**
 * Event: Zakończenie pracy nad protokołem
 */
data class ProtocolWorkCompletedEvent(
    val protocolId: String,
    val protocolTitle: String,
    val completedServices: List<String>,
    val completionTime: String,
    val qualityScore: Int?,
    val techniciansInvolved: List<String>,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = protocolId,
    aggregateType = "PROTOCOL",
    eventType = "PROTOCOL_WORK_COMPLETED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "protocolTitle" to protocolTitle,
        "completedServices" to completedServices,
        "completionTime" to completionTime,
        "qualityScore" to qualityScore,
        "techniciansInvolved" to techniciansInvolved
    ) + additionalMetadata
)

data class VehicleServiceCompletedEvent(
    val protocolId: String,
    val protocolTitle: String,
    val clientId: Long,
    val clientName: String,
    val clientTaxId: String?,
    val clientAddress: String?,
    val services: List<ServiceItem>,
    val totalNet: BigDecimal,
    val totalTax: BigDecimal,
    val totalGross: BigDecimal,
    val paymentMethod: String,
    val documentType: String,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null
) : BaseDomainEvent(
    aggregateId = protocolId,
    aggregateType = "PROTOCOL",
    eventType = "VEHICLE_SERVICE_COMPLETED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "clientId" to clientId,
        "totalAmount" to totalGross,
        "paymentMethod" to paymentMethod,
        "documentType" to documentType
    )
)

data class ServiceItem(
    val name: String,
    val description: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val taxRate: BigDecimal,
    val totalNet: BigDecimal,
    val totalGross: BigDecimal
)