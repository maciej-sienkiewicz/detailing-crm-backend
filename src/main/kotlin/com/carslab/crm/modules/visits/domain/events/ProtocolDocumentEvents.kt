package com.carslab.crm.modules.visits.domain.events

import com.carslab.crm.domain.model.view.protocol.ProtocolDocumentType
import com.carslab.crm.infrastructure.events.BaseDomainEvent

data class ProtocolDocumentUploadedEvent(
    val protocolId: String,
    val documentId: String,
    val documentType: ProtocolDocumentType,
    val originalName: String,
    val fileSize: Long,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = protocolId,
    aggregateType = "DOCUMENT",
    eventType = "PROTOCOL_DOCUMENT_UPLOADED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "documentId" to documentId,
        "documentType" to documentType,
        "originalName" to originalName,
        "fileSize" to fileSize
    ) + additionalMetadata
)

/**
 * Event: Usunięcie dokumentu z protokołu
 */
data class ProtocolDocumentDeletedEvent(
    val protocolId: String,
    val documentId: String,
    val documentType: String,
    val originalName: String,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = protocolId,
    aggregateType = "PROTOCOL",
    eventType = "PROTOCOL_DOCUMENT_DELETED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "documentId" to documentId,
        "documentType" to documentType,
        "originalName" to originalName
    ) + additionalMetadata
)

/**
 * Event: Utworzenie protokołu
 */
data class ProtocolCreatedEvent(
    val protocolId: String,
    val protocolTitle: String,
    val clientId: String?,
    val clientName: String?,
    val vehicleId: String?,
    val vehicleMake: String?,
    val vehicleModel: String?,
    val licensePlate: String?,
    val status: String,
    val servicesCount: Int,
    val totalAmount: Double,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = protocolId,
    aggregateType = "PROTOCOL",
    eventType = "PROTOCOL_CREATED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "protocolTitle" to protocolTitle,
        "clientId" to clientId,
        "clientName" to clientName,
        "vehicleId" to vehicleId,
        "vehicleMake" to vehicleMake,
        "vehicleModel" to vehicleModel,
        "licensePlate" to licensePlate,
        "status" to status,
        "servicesCount" to servicesCount,
        "totalAmount" to totalAmount
    ) + additionalMetadata
)

/**
 * Event: Usunięcie protokołu
 */
data class ProtocolDeletedEvent(
    val protocolId: String,
    val protocolTitle: String,
    val clientName: String?,
    val vehicleInfo: String?,
    val deletionReason: String?,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = protocolId,
    aggregateType = "PROTOCOL",
    eventType = "PROTOCOL_DELETED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "protocolTitle" to protocolTitle,
        "clientName" to clientName,
        "vehicleInfo" to vehicleInfo,
        "deletionReason" to deletionReason
    ) + additionalMetadata
)
