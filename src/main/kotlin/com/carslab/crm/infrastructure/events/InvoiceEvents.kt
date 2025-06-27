package com.carslab.crm.infrastructure.events

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import java.math.BigDecimal

/**
 * Event: Dodano fakturę
 */
data class InvoiceCreatedEvent(
    val invoiceId: String,
    val invoiceNumber: String,
    val clientId: String?,
    val clientName: String?,
    val visitId: String?,
    val visitTitle: String?,
    val amount: BigDecimal,
    val currency: String = "PLN",
    val dueDate: String?,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = invoiceId,
    aggregateType = "INVOICE",
    eventType = "INVOICE_CREATED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "invoiceNumber" to invoiceNumber,
        "clientId" to clientId,
        "clientName" to clientName,
        "visitId" to visitId,
        "visitTitle" to visitTitle,
        "amount" to amount,
        "currency" to currency,
        "dueDate" to dueDate
    ) + additionalMetadata
) {
    companion object {
        fun create(
            visit: CarReceptionProtocol?,
            document: UnifiedFinancialDocument,
            userId: String?,
            companyId: Long,
            userName: String?
        ) = InvoiceCreatedEvent(
            invoiceId = document.id.value,
            invoiceNumber = document.number,
            clientId = visit?.client?.id.toString(),
            clientName = visit?.client?.name.toString(),
            visitId = visit?.id?.value,
            visitTitle = visit?.title,
            amount = visit?.protocolServices?.sumOf { it.finalPrice.amount.toBigDecimal() } ?: BigDecimal.ZERO,
            currency = "PLN",
            dueDate = document.dueDate.toString(),
            userId = userId,
            userName = userName,
            companyId = companyId
        )
    }
}

fun InvoiceCreatedEvent.create(visit: CarReceptionProtocol?, document: UnifiedFinancialDocument, userId: String, companyId: Long, userName: String) =
    InvoiceCreatedEvent(
        invoiceId = document.id.value,
        invoiceNumber = document.number,
        clientId = visit?.client?.id.toString(),
        clientName = visit?.client?.name.toString(),
        visitId = visit?.id?.value,
        visitTitle = visit?.title,
        amount = visit?.protocolServices?.sumOf { it.finalPrice.amount.toBigDecimal() } ?: BigDecimal.ZERO,
        currency = "PLN",
        dueDate = document.dueDate.toString(),
        userId = userId,
        userName = userName,
        companyId = companyId
    )

/**
 * Event: Zmiana statusu faktury
 */
data class InvoiceStatusChangedEvent(
    val invoiceId: String,
    val invoiceNumber: String,
    val oldStatus: String,
    val newStatus: String,
    val clientId: String?,
    val clientName: String?,
    val visitId: String?,
    val statusChangeReason: String?,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = invoiceId,
    aggregateType = "INVOICE",
    eventType = "INVOICE_STATUS_CHANGED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "invoiceNumber" to invoiceNumber,
        "oldStatus" to oldStatus,
        "newStatus" to newStatus,
        "clientId" to clientId,
        "clientName" to clientName,
        "visitId" to visitId,
        "statusChangeReason" to statusChangeReason
    ) + additionalMetadata
)

/**
 * Event: Edycja faktury
 */
data class InvoiceEditedEvent(
    val invoiceId: String,
    val invoiceNumber: String,
    val changedFields: Map<String, Pair<String?, String?>>,
    val clientId: String?,
    val visitId: String?,
    val editReason: String?,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = invoiceId,
    aggregateType = "INVOICE",
    eventType = "INVOICE_EDITED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "invoiceNumber" to invoiceNumber,
        "changedFields" to changedFields,
        "clientId" to clientId,
        "visitId" to visitId,
        "editReason" to editReason
    ) + additionalMetadata
)

/**
 * Event: Usunięcie faktury
 */
data class InvoiceDeletedEvent(
    val invoiceId: String,
    val invoiceNumber: String,
    val clientId: String?,
    val clientName: String?,
    val visitId: String?,
    val deletionReason: String,
    val amount: Double,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = invoiceId,
    aggregateType = "INVOICE",
    eventType = "INVOICE_DELETED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "invoiceNumber" to invoiceNumber,
        "clientId" to clientId,
        "clientName" to clientName,
        "visitId" to visitId,
        "deletionReason" to deletionReason,
        "amount" to amount
    ) + additionalMetadata
)