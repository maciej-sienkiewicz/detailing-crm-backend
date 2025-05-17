package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.api.model.CashTransactionFilterDTO
import com.carslab.crm.domain.model.view.finance.CashStatistics
import com.carslab.crm.domain.model.view.finance.CashTransaction
import com.carslab.crm.domain.model.view.finance.TransactionId
import com.carslab.crm.domain.model.view.finance.TransactionType
import com.carslab.crm.domain.port.CashRepository
import com.carslab.crm.infrastructure.persistence.entity.CashTransactionEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Repository
class JpaCashRepositoryAdapter(
    private val cashJpaRepository: CashJpaRepository,
) : CashRepository {

    @Transactional
    override fun save(transaction: CashTransaction): CashTransaction {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val entity = if (transaction.id.value.isNotEmpty() && cashJpaRepository.existsById(transaction.id.value)) {
            // Update existing transaction
            val existingEntity = cashJpaRepository.findByCompanyIdAndId(companyId, transaction.id.value)
                .orElseThrow { IllegalArgumentException("Transaction not found or access denied") }

            existingEntity.type = transaction.type
            existingEntity.description = transaction.description
            existingEntity.date = transaction.date
            existingEntity.amount = transaction.amount
            existingEntity.visitId = transaction.visitId
            existingEntity.createdBy = transaction.createdBy.value
            existingEntity.updatedAt = transaction.audit.updatedAt

            existingEntity
        } else {
            // Create new transaction
            CashTransactionEntity(
                id = transaction.id.value,
                companyId = companyId,
                type = transaction.type,
                description = transaction.description,
                date = transaction.date,
                amount = transaction.amount,
                visitId = transaction.visitId,
                createdBy = transaction.createdBy.value,
                createdAt = transaction.audit.createdAt,
                updatedAt = transaction.audit.updatedAt
            )
        }

        val savedEntity = cashJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: TransactionId): CashTransaction? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return cashJpaRepository.findByCompanyIdAndId(companyId, id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAll(
        filter: CashTransactionFilterDTO?,
        page: Int,
        size: Int
    ): Pair<List<CashTransaction>, Long> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date", "createdAt"))

        if (filter == null) {
            val pageResult = cashJpaRepository.findByCompanyId(companyId)
            return Pair(
                pageResult.map { it.toDomain() },
                pageResult.size.toLong()
            )
        }

        val type = filter.type?.let { try { TransactionType.valueOf(it) } catch (e: IllegalArgumentException) { null } }

        val pageResult = cashJpaRepository.searchTransactionsForCompany(
            type = type,
            description = filter.description,
            dateFrom = filter.dateFrom,
            dateTo = filter.dateTo,
            visitId = filter.visitId,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            companyId = companyId,
            pageable = pageable
        )

        return Pair(
            pageResult.content.map { it.toDomain() },
            pageResult.totalElements
        )
    }

    @Transactional
    override fun deleteById(id: TransactionId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val entity = cashJpaRepository.findByCompanyIdAndId(companyId, id.value)
            .orElse(null) ?: return false

        cashJpaRepository.delete(entity)
        return true
    }

    override fun getCurrentBalance(): BigDecimal {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return cashJpaRepository.calculateCurrentBalanceForCompany(companyId) ?: BigDecimal.ZERO
    }

    override fun getStatisticsForPeriod(startDate: LocalDate, endDate: LocalDate): CashStatistics {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val income = cashJpaRepository.sumAmountByTypeAndDateRangeAndCompanyId(
            TransactionType.INCOME, startDate, endDate, companyId
        ) ?: BigDecimal.ZERO

        val expense = cashJpaRepository.sumAmountByTypeAndDateRangeAndCompanyId(
            TransactionType.EXPENSE, startDate, endDate, companyId
        ) ?: BigDecimal.ZERO

        val transactionCount = cashJpaRepository.countTransactionsByDateRangeAndCompanyId(startDate, endDate, companyId)

        return CashStatistics(
            periodStart = startDate,
            periodEnd = endDate,
            income = income,
            expense = expense,
            balance = income - expense,
            transactionCount = transactionCount
        )
    }

    override fun getCurrentMonthStatistics(): CashStatistics {
        val now = LocalDate.now()
        val currentMonth = YearMonth.from(now)
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()

        return getStatisticsForPeriod(startDate, endDate)
    }

    override fun findByVisitId(visitId: String): List<CashTransaction> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return cashJpaRepository.findByVisitIdAndCompanyId(visitId, companyId)
            .map { it.toDomain() }
    }

    override fun findByType(type: TransactionType): List<CashTransaction> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return cashJpaRepository.findByTypeAndCompanyId(type, companyId)
            .map { it.toDomain() }
    }
}