package com.carslab.crm.finances.infrastructure.entitiy

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.UserId
import com.carslab.crm.domain.model.view.finance.CashTransaction
import com.carslab.crm.domain.model.view.finance.TransactionId
import com.carslab.crm.domain.model.view.finance.TransactionType
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import jakarta.persistence.*
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "cash_transactions")
class CashTransactionEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @Column(nullable = false)
    var companyId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: TransactionType,

    @Column(nullable = false)
    var description: String,

    @Column(nullable = false)
    var date: LocalDate,

    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,

    @Column(name = "visit_id")
    var visitId: String? = null,

    @Column(name = "created_by", nullable = false)
    var createdBy: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime
) {
    fun toDomain(): CashTransaction {
        return CashTransaction(
            id = TransactionId(id),
            type = type,
            description = description,
            date = date,
            amount = amount,
            visitId = visitId,
            createdBy = UserId(createdBy),
            audit = Audit(createdAt = createdAt, updatedAt = updatedAt)
        )
    }

    companion object {
        fun fromDomain(domain: CashTransaction): CashTransactionEntity {
            return CashTransactionEntity(
                id = domain.id.value,
                companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                type = domain.type,
                description = domain.description,
                date = domain.date,
                amount = domain.amount,
                visitId = domain.visitId,
                createdBy = domain.createdBy.value,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }
    }
}