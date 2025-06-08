package com.carslab.crm.finances.infrastructure.entitiy

import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.view.finance.*
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import jakarta.persistence.*
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class InvoiceEntityId(
    val id: String = "",
    val companyId: Long = 0L
)

@Entity
@Table(name = "invoices")
@IdClass(InvoiceEntityId::class)
class InvoiceEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @Id
    @Column(nullable = false)
    var companyId: Long,

    @Column(nullable = false, unique = true)
    var number: String,

    @Column(nullable = false)
    var title: String,

    @Column(name = "issued_date", nullable = false)
    var issuedDate: LocalDate,

    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDate,

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

    @Column(name = "client_id")
    var clientId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: InvoiceStatus,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: InvoiceType,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    var paymentMethod: PaymentMethod,

    @Column(name = "total_net", nullable = false, precision = 19, scale = 2)
    var totalNet: BigDecimal,

    @Column(name = "total_tax", nullable = false, precision = 19, scale = 2)
    var totalTax: BigDecimal,

    @Column(name = "total_gross", nullable = false, precision = 19, scale = 2)
    var totalGross: BigDecimal,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "protocol_id")
    var protocolId: String? = null,

    @Column(name = "protocol_number")
    var protocolNumber: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,

    @OneToMany(mappedBy = "invoice", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<InvoiceItemEntity> = mutableListOf(),

    @OneToOne(mappedBy = "invoice", cascade = [CascadeType.ALL], orphanRemoval = true)
    var attachment: InvoiceAttachmentEntity? = null
) {
    fun toDomain(): Invoice {
        return Invoice(
            id = InvoiceId(id),
            number = number,
            title = title,
            issuedDate = issuedDate,
            dueDate = dueDate,
            sellerName = sellerName,
            sellerTaxId = sellerTaxId,
            sellerAddress = sellerAddress,
            buyerName = buyerName,
            buyerTaxId = buyerTaxId,
            buyerAddress = buyerAddress,
            clientId = clientId?.let { ClientId(it) },
            status = status,
            type = type,
            paymentMethod = paymentMethod,
            totalNet = totalNet,
            totalTax = totalTax,
            totalGross = totalGross,
            currency = currency,
            notes = notes,
            protocolId = protocolId,
            protocolNumber = protocolNumber,
            items = items.map { it.toDomain() },
            attachment = attachment?.toDomain(),
            audit = Audit(createdAt = createdAt, updatedAt = updatedAt)
        )
    }

    companion object {
        fun fromDomain(domain: Invoice): InvoiceEntity {
            return InvoiceEntity(
                id = domain.id.value,
                companyId =(SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                number = domain.number,
                title = domain.title,
                issuedDate = domain.issuedDate,
                dueDate = domain.dueDate,
                sellerName = domain.sellerName,
                sellerTaxId = domain.sellerTaxId,
                sellerAddress = domain.sellerAddress,
                buyerName = domain.buyerName,
                buyerTaxId = domain.buyerTaxId,
                buyerAddress = domain.buyerAddress,
                clientId = domain.clientId?.value,
                status = domain.status,
                type = domain.type,
                paymentMethod = domain.paymentMethod,
                totalNet = domain.totalNet,
                totalTax = domain.totalTax,
                totalGross = domain.totalGross,
                currency = domain.currency,
                notes = domain.notes,
                protocolId = domain.protocolId,
                protocolNumber = domain.protocolNumber,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }
    }
}

@Entity
@Table(name = "invoice_items")
class InvoiceItemEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @Column(name = "invoice_id", nullable = false)
    val invoiceId: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(name = "invoice_id", referencedColumnName = "id", insertable = false, updatable = false),
        JoinColumn(name = "company_id", referencedColumnName = "companyId", insertable = false, updatable = false)
    )
    var invoice: InvoiceEntity? = null,

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
    fun toDomain(): InvoiceItem {
        return InvoiceItem(
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
        fun fromDomain(domain: InvoiceItem, invoice: InvoiceEntity): InvoiceItemEntity {
            return InvoiceItemEntity(
                id = domain.id,
                invoiceId = invoice.id,
                companyId = invoice.companyId,
                invoice = invoice,
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
@Table(name = "invoice_attachments")
class InvoiceAttachmentEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @Column(name = "invoice_id", nullable = false)
    val invoiceId: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(name = "invoice_id", referencedColumnName = "id", insertable = false, updatable = false),
        JoinColumn(name = "company_id", referencedColumnName = "companyId", insertable = false, updatable = false)
    )
    var invoice: InvoiceEntity? = null,

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
    fun toDomain(): InvoiceAttachment {
        return InvoiceAttachment(
            id = id,
            name = name,
            size = size,
            type = type,
            storageId = storageId,
            uploadedAt = uploadedAt
        )
    }

    companion object {
        fun fromDomain(domain: InvoiceAttachment, invoice: InvoiceEntity): InvoiceAttachmentEntity {
            return InvoiceAttachmentEntity(
                id = domain.id,
                invoiceId = invoice.id,
                companyId = invoice.companyId,
                invoice = invoice,
                name = domain.name,
                size = domain.size,
                type = domain.type,
                storageId = domain.storageId,
                uploadedAt = domain.uploadedAt
            )
        }
    }
}