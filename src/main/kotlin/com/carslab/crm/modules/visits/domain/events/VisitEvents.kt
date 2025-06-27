package com.carslab.crm.modules.visits.domain.events

import com.carslab.crm.infrastructure.events.BaseDomainEvent

/**
 * Event: Zaplanowanie wizyty
 */
data class VisitScheduledEvent(
    val visitId: String,
    val visitTitle: String,
    val clientId: String?,
    val clientName: String?,
    val vehicleId: String?,
    val vehicleMake: String?,
    val vehicleModel: String?,
    val licensePlate: String?,
    val scheduledDate: String,
    val services: List<String>,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = visitId,
    aggregateType = "VISIT",
    eventType = "VISIT_SCHEDULED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "visitTitle" to visitTitle,
        "clientId" to clientId,
        "clientName" to clientName,
        "vehicleId" to vehicleId,
        "vehicleMake" to vehicleMake,
        "vehicleModel" to vehicleModel,
        "licensePlate" to licensePlate,
        "scheduledDate" to scheduledDate,
        "services" to services
    ) + additionalMetadata
)

/**
 * Event: Rozpoczęcie wizyty
 */
data class VisitStartedEvent(
    val visitId: String,
    val visitTitle: String,
    val clientId: String?,
    val clientName: String?,
    val vehicleId: String?,
    val vehicleDisplayName: String?,
    val startedAt: String,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = visitId,
    aggregateType = "VISIT",
    eventType = "VISIT_STARTED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "visitTitle" to visitTitle,
        "clientId" to clientId,
        "clientName" to clientName,
        "vehicleId" to vehicleId,
        "vehicleDisplayName" to vehicleDisplayName,
        "startedAt" to startedAt
    ) + additionalMetadata
)

/**
 * Event: Edycja protokołu
 */
data class ProtocolEditedEvent(
    val protocolId: String,
    val protocolTitle: String,
    val visitId: String?,
    val changedFields: Map<String, Pair<String?, String?>>,
    val editReason: String?,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = protocolId,
    aggregateType = "PROTOCOL",
    eventType = "PROTOCOL_EDITED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "protocolTitle" to protocolTitle,
        "visitId" to visitId,
        "changedFields" to changedFields,
        "editReason" to editReason
    ) + additionalMetadata
)

/**
 * Event: Oddanie pojazdu
 */
data class VehicleReleasedEvent(
    val visitId: String,
    val protocolId: String,
    val clientId: String,
    val clientName: String,
    val vehicleId: String,
    val vehicleDisplayName: String,
    val paymentMethod: String,
    val totalAmount: Double?,
    val releaseNotes: String?,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = visitId,
    aggregateType = "VISIT",
    eventType = "VEHICLE_RELEASED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "protocolId" to protocolId,
        "clientId" to clientId,
        "clientName" to clientName,
        "vehicleId" to vehicleId,
        "vehicleDisplayName" to vehicleDisplayName,
        "paymentMethod" to paymentMethod,
        "totalAmount" to totalAmount,
        "releaseNotes" to releaseNotes
    ) + additionalMetadata
)

/**
 * Event: Dodanie/usunięcie zdjęcia w wizycie
 */
data class VisitMediaChangedEvent(
    val visitId: String,
    val mediaId: String,
    val mediaName: String,
    val action: MediaAction, // ADD, REMOVE, UPDATE
    val vehicleId: String?,
    val vehicleDisplayName: String?,
    val mediaType: String = "PHOTO",
    val mediaSize: Long? = null,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = visitId,
    aggregateType = "VISIT",
    eventType = "VISIT_MEDIA_CHANGED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "mediaId" to mediaId,
        "mediaName" to mediaName,
        "action" to action.name,
        "vehicleId" to vehicleId,
        "vehicleDisplayName" to vehicleDisplayName,
        "mediaType" to mediaType,
        "mediaSize" to mediaSize
    ) + additionalMetadata
)

enum class MediaAction { ADD, REMOVE, UPDATE }

/**
 * Event: Anulowanie wizyty
 */
data class VisitCancelledEvent(
    val visitId: String,
    val visitTitle: String,
    val clientId: String?,
    val clientName: String?,
    val vehicleId: String?,
    val vehicleDisplayName: String?,
    val cancellationReason: String,
    val scheduledDate: String,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = visitId,
    aggregateType = "VISIT",
    eventType = "VISIT_CANCELLED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "visitTitle" to visitTitle,
        "clientId" to clientId,
        "clientName" to clientName,
        "vehicleId" to vehicleId,
        "vehicleDisplayName" to vehicleDisplayName,
        "cancellationReason" to cancellationReason,
        "scheduledDate" to scheduledDate
    ) + additionalMetadata
)