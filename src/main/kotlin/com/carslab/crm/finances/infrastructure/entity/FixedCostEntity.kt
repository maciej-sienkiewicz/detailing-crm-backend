// src/main/kotlin/com/carslab/crm/finances/infrastructure/entity/FixedCostEntity.kt
package com.carslab.crm.finances.infrastructure.entity

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.finances.domain.model.fixedcosts.*
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "fixed_costs")
@NamedEntityGraphs(
    NamedEntityGraph(
        name = "FixedCost.withPayments",
        attributeNodes = [NamedAttributeNode("payments")]
    )
)
class FixedCostEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @Column(nullable = false)
    var companyId: Long,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: FixedCostCategory,

    @Column(name = "monthly_amount", nullable = false, precision = 19, scale = 2)
    var monthlyAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var frequency: CostFrequency,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FixedCostStatus,

    @Column(name = "auto_renew")
    var autoRenew: Boolean = false,

    @Column(name = "supplier_name")
    var supplierName: String? = null,

    @Column(name = "supplier_tax_id")
    var supplierTaxId: String? = null,

    @Column(name = "contract_number")
    var contractNumber: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,

    @OneToMany(
        mappedBy = "fixedCost",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @Fetch(FetchMode.SUBSELECT)
    @BatchSize(size = 20)
    @OrderBy("paymentDate DESC")
    var payments: MutableList<FixedCostPaymentEntity> = mutableListOf()
) {

    /**
     * Konwersja do modelu domenowego
     */
    fun toDomain(): FixedCost {
        return FixedCost(
            id = FixedCostId(id),
            name = name,
            description = description,
            category = category,
            monthlyAmount = monthlyAmount,
            frequency = frequency,
            startDate = startDate,
            endDate = endDate,
            status = status,
            autoRenew = autoRenew,
            supplierInfo = createSupplierInfo(),
            contractNumber = contractNumber,
            notes = notes,
            payments = safeMapPayments(),
            audit = Audit(createdAt = createdAt, updatedAt = updatedAt)
        )
    }

    /**
     * Bezpieczne mapowanie płatności
     */
    private fun safeMapPayments(): List<FixedCostPayment> {
        return try {
            payments.map { it.toDomain() }
        } catch (e: Exception) {
            println("Warning: Could not load payments for fixed cost $id: ${e.message}")
            emptyList()
        }
    }

    /**
     * Tworzenie informacji o dostawcy
     */
    private fun createSupplierInfo(): SupplierInfo? {
        return if (supplierName != null) {
            SupplierInfo(
                name = supplierName!!,
                taxId = supplierTaxId
            )
        } else null
    }

    companion object {
        fun fromDomain(domain: FixedCost): FixedCostEntity {
            return FixedCostEntity(
                id = domain.id.value,
                companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                name = domain.name,
                description = domain.description,
                category = domain.category,
                monthlyAmount = domain.monthlyAmount,
                frequency = domain.frequency,
                startDate = domain.startDate,
                endDate = domain.endDate,
                status = domain.status,
                autoRenew = domain.autoRenew,
                supplierName = domain.supplierInfo?.name,
                supplierTaxId = domain.supplierInfo?.taxId,
                contractNumber = domain.contractNumber,
                notes = domain.notes,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }
    }
}

@Entity
@Table(name = "fixed_cost_payments")
class FixedCostPaymentEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_cost_id", nullable = false)
    var fixedCost: FixedCostEntity,

    @Column(name = "payment_date", nullable = false)
    var paymentDate: LocalDate,

    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,

    @Column(name = "planned_amount", nullable = false, precision = 19, scale = 2)
    var plannedAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    var paymentMethod: PaymentMethod? = null,

    @Column(name = "document_id")
    var documentId: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime
) {

    /**
     * Konwersja do modelu domenowego
     */
    fun toDomain(): FixedCostPayment {
        return FixedCostPayment(
            id = id,
            paymentDate = paymentDate,
            amount = amount,
            plannedAmount = plannedAmount,
            status = status,
            paymentMethod = paymentMethod,
            documentId = documentId,
            notes = notes,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(domain: FixedCostPayment, fixedCost: FixedCostEntity): FixedCostPaymentEntity {
            return FixedCostPaymentEntity(
                id = domain.id,
                fixedCost = fixedCost,
                paymentDate = domain.paymentDate,
                amount = domain.amount,
                plannedAmount = domain.plannedAmount,
                status = domain.status,
                paymentMethod = domain.paymentMethod,
                documentId = domain.documentId,
                notes = domain.notes,
                createdAt = domain.createdAt
            )
        }
    }
}

@Entity
@Table(name = "breakeven_configurations")
class BreakevenConfigurationEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @Column(nullable = false)
    var companyId: Long,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "average_service_price", nullable = false, precision = 19, scale = 2)
    var averageServicePrice: BigDecimal,

    @Column(name = "average_margin_percentage", nullable = false, precision = 5, scale = 2)
    var averageMarginPercentage: BigDecimal,

    @Column(name = "working_days_per_month", nullable = false)
    var workingDaysPerMonth: Int = 22,

    @Column(name = "target_services_per_day")
    var targetServicesPerDay: Int? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime
) {

    /**
     * Konwersja do modelu domenowego
     */
    fun toDomain(): BreakevenConfiguration {
        return BreakevenConfiguration(
            id = BreakevenConfigurationId(id),
            name = name,
            description = description,
            averageServicePrice = averageServicePrice,
            averageMarginPercentage = averageMarginPercentage,
            workingDaysPerMonth = workingDaysPerMonth,
            targetServicesPerDay = targetServicesPerDay,
            isActive = isActive,
            audit = com.carslab.crm.domain.model.Audit(createdAt = createdAt, updatedAt = updatedAt)
        )
    }

    companion object {
        fun fromDomain(domain: BreakevenConfiguration): BreakevenConfigurationEntity {
            return BreakevenConfigurationEntity(
                id = domain.id.value,
                companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                name = domain.name,
                description = domain.description,
                averageServicePrice = domain.averageServicePrice,
                averageMarginPercentage = domain.averageMarginPercentage,
                workingDaysPerMonth = domain.workingDaysPerMonth,
                targetServicesPerDay = domain.targetServicesPerDay,
                isActive = domain.isActive,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }
    }
}