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
    private val cashJpaRepository: CashJpaRepository
) : CashRepository {

    @Transactional
    override fun save(transaction: CashTransaction): CashTransaction {
        val entity = CashTransactionEntity.fromDomain(transaction)
        val savedEntity = cashJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: TransactionId): CashTransaction? {
        return cashJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAll(
        filter: CashTransactionFilterDTO?,
        page: Int,
        size: Int
    ): Pair<List<CashTransaction>, Long> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date", "createdAt"))

        if (filter == null) {
            val pageResult = cashJpaRepository.findAll(pageable)
            return Pair(
                pageResult.content.map { it.toDomain() },
                pageResult.totalElements
            )
        }

        val type = filter.type?.let { try { TransactionType.valueOf(it) } catch (e: IllegalArgumentException) { null } }

        val pageResult = cashJpaRepository.searchTransactions(
            type = type,
            description = filter.description,
            dateFrom = filter.dateFrom,
            dateTo = filter.dateTo,
            visitId = filter.visitId,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            pageable = pageable
        )

        return Pair(
            pageResult.content.map { it.toDomain() },
            pageResult.totalElements
        )
    }

    @Transactional
    override fun deleteById(id: TransactionId): Boolean {
        return if (cashJpaRepository.existsById(id.value)) {
            cashJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }

    override fun getCurrentBalance(): BigDecimal {
        return cashJpaRepository.calculateCurrentBalance() ?: BigDecimal.ZERO
    }

    override fun getStatisticsForPeriod(startDate: LocalDate, endDate: LocalDate): CashStatistics {
        val income = cashJpaRepository.sumAmountByTypeAndDateRange(
            TransactionType.INCOME, startDate, endDate
        ) ?: BigDecimal.ZERO

        val expense = cashJpaRepository.sumAmountByTypeAndDateRange(
            TransactionType.EXPENSE, startDate, endDate
        ) ?: BigDecimal.ZERO

        val transactionCount = cashJpaRepository.countTransactionsByDateRange(startDate, endDate)

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