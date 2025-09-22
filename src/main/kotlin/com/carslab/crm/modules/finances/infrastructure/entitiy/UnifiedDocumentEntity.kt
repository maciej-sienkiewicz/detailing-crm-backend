package com.carslab.crm.finances.infrastructure.entitiy

import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.api.model.DocumentType
import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.view.finance.*
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime


data class UnifiedDocumentEntityId(
    val id: String = "",
    val companyId: Long = 0L
)

@Entity
@Table(name = "unified_financial_documents")

@NamedEntityGraphs(
    NamedEntityGraph(
        name = "UnifiedDocument.withItems",
        attributeNodes = [NamedAttributeNode("items")]
    ),
    NamedEntityGraph(
        name = "UnifiedDocument.withItemsAndAttachment",
        attributeNodes = [
            NamedAttributeNode("items"),
            NamedAttributeNode("attachment")
        ]
    )
)
@IdClass(UnifiedDocumentEntityId::class)
class UnifiedDocumentEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @Id
    @Column(nullable = false)
    var companyId: Long,

    @Column(nullable = false, unique = true)
    var number: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: DocumentType,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "issued_date", nullable = false)
    var issuedDate: LocalDate,

    @Column(name = "due_date")
    var dueDate: LocalDate? = null,

    @Column(name = "seller_name", nullable = false)
    var sellerName: String,

    @Column(name = "seller_tax_id")
    var sellerTaxId: String? = null,

    @Column(name = "seller_address", columnDefinition = "TEXT")
    var sellerAddress: String? = null,

    @Column(name = "buyer_name", nullable = false)
    var buyerName: String,

    @Column(name = "buyer_tax_id")
    var buyerTaxId: String? = null,

    @Column(name = "buyer_address", columnDefinition = "TEXT")
    var buyerAddress: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DocumentStatus,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var direction: TransactionDirection,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    var paymentMethod: PaymentMethod,

    @Column(name = "total_net", nullable = false, precision = 19, scale = 2)
    var totalNet: BigDecimal,

    @Column(name = "total_tax", nullable = false, precision = 19, scale = 2)
    var totalTax: BigDecimal,

    @Column(name = "total_gross", nullable = false, precision = 19, scale = 2)
    var totalGross: BigDecimal,

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    var paidAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "protocol_id")
    var protocolId: String? = null,

    @Column(name = "protocol_number")
    var protocolNumber: String? = null,

    @Column(name = "visit_id")
    var visitId: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,

    // NAPRAWIONE: Eager loading z optymalizacjami
    @OneToMany(
        mappedBy = "document",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @Fetch(FetchMode.SUBSELECT)
    @BatchSize(size = 20)
    @OrderBy("id ASC")
    var items: MutableList<DocumentItemEntity> = mutableListOf(),

    // NAPRAWIONE: Eager loading dla attachment
    @OneToOne(
        mappedBy = "document",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    var attachment: DocumentAttachmentEntity? = null
) {

    /**
     * Bezpieczna konwersja do modelu domenowego z obsługą lazy loading
     */
    fun toDomain(): UnifiedFinancialDocument {
        return UnifiedFinancialDocument(
            id = UnifiedDocumentId(id),
            number = number,
            type = type,
            title = title,
            description = description,
            issuedDate = issuedDate,
            dueDate = dueDate,
            sellerName = sellerName,
            sellerTaxId = sellerTaxId,
            sellerAddress = sellerAddress,
            buyerName = buyerName,
            buyerTaxId = buyerTaxId,
            buyerAddress = buyerAddress,
            status = status,
            direction = direction,
            paymentMethod = paymentMethod,
            totalNet = totalNet,
            totalTax = totalTax,
            totalGross = totalGross,
            paidAmount = paidAmount,
            currency = currency,
            notes = notes,
            protocolId = protocolId,
            protocolNumber = protocolNumber,
            visitId = visitId,
            items = safeMapItems(),
            attachment = safeMapAttachment(),
            audit = Audit(createdAt = createdAt, updatedAt = updatedAt)
        )
    }

    /**
     * Bezpieczne mapowanie items z obsługą lazy loading
     */
    private fun safeMapItems(): List<DocumentItem> {
        return try {
            // Sprawdzenie czy kolekcja jest zainicjalizowana
            if (items.isNotEmpty()) {
                items.map { it.toDomain() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            // Log warning ale nie przerwaj operacji
            println("Warning: Could not load items for document $id: ${e.message}")
            emptyList()
        }
    }

    /**
     * Bezpieczne mapowanie attachment z obsługą lazy loading
     */
    private fun safeMapAttachment(): DocumentAttachment? {
        return try {
            attachment?.toDomain()
        } catch (e: Exception) {
            // Log warning ale nie przerwij operacji
            println("Warning: Could not load attachment for document $id: ${e.message}")
            null
        }
    }

    companion object {
        fun fromDomain(domain: UnifiedFinancialDocument, authContext: AuthContext? = null): UnifiedDocumentEntity {
            return UnifiedDocumentEntity(
                id = domain.id.value,
                companyId = authContext?.companyId?.value ?: (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                number = domain.number,
                type = domain.type,
                title = domain.title,
                description = domain.description,
                issuedDate = domain.issuedDate,
                dueDate = domain.dueDate,
                sellerName = domain.sellerName,
                sellerTaxId = domain.sellerTaxId,
                sellerAddress = domain.sellerAddress,
                buyerName = domain.buyerName,
                buyerTaxId = domain.buyerTaxId,
                buyerAddress = domain.buyerAddress,
                status = domain.status,
                direction = domain.direction,
                paymentMethod = domain.paymentMethod,
                totalNet = domain.totalNet,
                totalTax = domain.totalTax,
                totalGross = domain.totalGross,
                paidAmount = domain.paidAmount,
                currency = domain.currency,
                notes = domain.notes,
                protocolId = domain.protocolId,
                protocolNumber = domain.protocolNumber,
                visitId = domain.visitId,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }
    }
}

@Entity
@Table(name = "document_items")
class DocumentItemEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @Column(name = "document_id", nullable = false)
    val documentId: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(name = "document_id", referencedColumnName = "id", insertable = false, updatable = false),
        JoinColumn(name = "company_id", referencedColumnName = "companyId", insertable = false, updatable = false)
    )
    var document: UnifiedDocumentEntity? = null,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, precision = 19, scale = 2)
    var quantity: BigDecimal,

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    var unitPrice: BigDecimal,

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    var taxRate: BigDecimal,

    @Column(name = "total_net", nullable = false, precision = 19, scale = 2)
    var totalNet: BigDecimal,

    @Column(name = "total_gross", nullable = false, precision = 19, scale = 2)
    var totalGross: BigDecimal
) {
    fun toDomain(): DocumentItem {
        return DocumentItem(
            id = id,
            name = name,
            description = description,
            quantity = quantity,
            unitPrice = unitPrice,
            taxRate = taxRate,
            totalNet = totalNet,
            totalGross = totalGross
        )
    }

    companion object {
        fun fromDomain(domain: DocumentItem, document: UnifiedDocumentEntity): DocumentItemEntity {
            return DocumentItemEntity(
                id = domain.id,
                documentId = document.id,
                companyId = document.companyId,
                document = document,
                name = domain.name,
                description = domain.description,
                quantity = domain.quantity,
                unitPrice = domain.unitPrice,
                taxRate = domain.taxRate,
                totalNet = domain.totalNet,
                totalGross = domain.totalGross
            )
        }
    }
}

@Entity
@Table(name = "document_attachments")
class DocumentAttachmentEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @Column(name = "document_id", nullable = false)
    val documentId: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(name = "document_id", referencedColumnName = "id", insertable = false, updatable = false),
        JoinColumn(name = "company_id", referencedColumnName = "companyId", insertable = false, updatable = false)
    )
    var document: UnifiedDocumentEntity? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var size: Long,

    @Column(nullable = false)
    var type: String,

    @Column(name = "storage_id", nullable = false)
    var storageId: String,

    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: LocalDateTime
) {
    fun toDomain(): DocumentAttachment {
        return DocumentAttachment(
            id = id,
            name = name,
            size = size,
            type = type,
            storageId = storageId,
            uploadedAt = uploadedAt
        )
    }

    companion object {
        fun fromDomain(domain: DocumentAttachment, document: UnifiedDocumentEntity): DocumentAttachmentEntity {
            return DocumentAttachmentEntity(
                id = domain.id,
                documentId = document.id,
                companyId = document.companyId,
                document = document,
                name = domain.name,
                size = domain.size,
                type = domain.type,
                storageId = domain.storageId,
                uploadedAt = domain.uploadedAt
            )
        }
    }
}