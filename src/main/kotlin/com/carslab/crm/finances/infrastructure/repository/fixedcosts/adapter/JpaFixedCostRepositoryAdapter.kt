package com.carslab.crm.finances.infrastructure.repository.fixedcosts.adapter

import com.carslab.crm.api.model.FixedCostFilterDTO
import com.carslab.crm.finances.domain.PaginatedResult
import com.carslab.crm.finances.domain.model.fixedcosts.CostFrequency
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCost
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostCategory
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostId
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostPayment
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostStatus
import com.carslab.crm.finances.domain.ports.fixedcosts.FixedCostRepository
import com.carslab.crm.finances.domain.ports.fixedcosts.OverduePayment
import com.carslab.crm.finances.domain.ports.fixedcosts.PaymentStatistics
import com.carslab.crm.finances.domain.ports.fixedcosts.UpcomingPayment
import com.carslab.crm.finances.infrastructure.entity.FixedCostEntity
import com.carslab.crm.finances.infrastructure.entity.FixedCostPaymentEntity
import com.carslab.crm.finances.infrastructure.repository.fixedcosts.FixedCostJpaRepository
import com.carslab.crm.finances.infrastructure.repository.fixedcosts.FixedCostPaymentJpaRepository
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class JpaFixedCostRepositoryAdapter(
    private val fixedCostJpaRepository: FixedCostJpaRepository,
    private val paymentJpaRepository: FixedCostPaymentJpaRepository
) : FixedCostRepository {

    @Transactional
    override fun save(fixedCost: FixedCost): FixedCost {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val entity = if (fixedCostJpaRepository.findByCompanyIdAndId(companyId, fixedCost.id.value).isPresent) {
            val existingEntity = fixedCostJpaRepository.findByCompanyIdAndId(companyId, fixedCost.id.value).get()

            // Aktualizacja podstawowych pól
            existingEntity.name = fixedCost.name
            existingEntity.description = fixedCost.description
            existingEntity.category = fixedCost.category
            existingEntity.monthlyAmount = fixedCost.monthlyAmount
            existingEntity.frequency = fixedCost.frequency
            existingEntity.startDate = fixedCost.startDate
            existingEntity.endDate = fixedCost.endDate
            existingEntity.status = fixedCost.status
            existingEntity.autoRenew = fixedCost.autoRenew
            existingEntity.supplierName = fixedCost.supplierInfo?.name
            existingEntity.supplierTaxId = fixedCost.supplierInfo?.taxId
            existingEntity.contractNumber = fixedCost.contractNumber
            existingEntity.notes = fixedCost.notes
            existingEntity.updatedAt = LocalDateTime.now()

            // Czyszczenie istniejących płatności
            existingEntity.payments.clear()

            existingEntity
        } else {
            FixedCostEntity.Companion.fromDomain(fixedCost)
        }

        // Zapisanie kosztu stałego aby uzyskać identyfikator (lub aktualizować istniejący)
        val savedEntity = fixedCostJpaRepository.save(entity)

        // Dodanie płatności
        fixedCost.payments.forEach { payment ->
            val paymentEntity = FixedCostPaymentEntity.Companion.fromDomain(payment, savedEntity)
            savedEntity.payments.add(paymentEntity)
        }

        // Zapisanie kosztu stałego z płatnościami
        val finalEntity = fixedCostJpaRepository.save(savedEntity)
        return finalEntity.toDomain()
    }

    override fun findById(id: FixedCostId): FixedCost? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return fixedCostJpaRepository.findByCompanyIdAndId(companyId, id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAll(filter: FixedCostFilterDTO?, page: Int, size: Int): PaginatedResult<FixedCost> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))

        val result = if (filter == null) {
            fixedCostJpaRepository.findByCompanyId(companyId, pageable)
        } else {
            fixedCostJpaRepository.searchFixedCosts(
                name = filter.name,
                category = filter.category,
                status = filter.status,
                frequency = filter.frequency,
                supplierName = filter.supplierName,
                contractNumber = filter.contractNumber,
                startDateFrom = filter.startDateFrom,
                startDateTo = filter.startDateTo,
                endDateFrom = filter.endDateFrom,
                endDateTo = filter.endDateTo,
                minAmount = filter.minAmount,
                maxAmount = filter.maxAmount,
                companyId = companyId,
                pageable = pageable
            )
        }

        return PaginatedResult(
            data = result.content.map { it.toDomain() },
            page = result.number,
            size = result.size,
            totalItems = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @Transactional
    override fun deleteById(id: FixedCostId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val entity = fixedCostJpaRepository.findByCompanyIdAndId(companyId, id.value).orElse(null) ?: return false
        fixedCostJpaRepository.delete(entity)
        return true
    }

    override fun findActiveInPeriod(startDate: LocalDate, endDate: LocalDate): List<FixedCost> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return fixedCostJpaRepository.findActiveInPeriod(startDate, endDate, companyId)
            .map { it.toDomain() }
    }

    override fun findByCategory(category: FixedCostCategory): List<FixedCost> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return fixedCostJpaRepository.findByCompanyIdAndCategory(companyId, category)
            .map { it.toDomain() }
    }

    override fun findByStatus(status: FixedCostStatus): List<FixedCost> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return fixedCostJpaRepository.findByCompanyIdAndStatus(companyId, status)
            .map { it.toDomain() }
    }

    override fun calculateTotalFixedCostsForPeriod(startDate: LocalDate, endDate: LocalDate): BigDecimal {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return fixedCostJpaRepository.calculateTotalFixedCostsForPeriod(startDate, endDate, companyId)
            ?: BigDecimal.ZERO
    }

    override fun getCategorySummary(period: LocalDate): Map<FixedCostCategory, BigDecimal> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val startOfMonth = period.withDayOfMonth(1)
        val endOfMonth = period.withDayOfMonth(period.lengthOfMonth())

        val results = fixedCostJpaRepository.getCategorySummary(startOfMonth, endOfMonth, companyId)

        return results.associate { row ->
            val category = row[0] as FixedCostCategory
            val total = row[1] as BigDecimal
            category to total
        }
    }

    override fun findUpcomingPayments(days: Int): List<UpcomingPayment> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val results = fixedCostJpaRepository.findCostsForUpcomingPayments(companyId)

        // Symulacja nadchodzących płatności na podstawie częstotliwości
        val upcomingPayments = mutableListOf<UpcomingPayment>()
        val currentDate = LocalDate.now()
        val targetDate = currentDate.plusDays(days.toLong())

        results.forEach { row ->
            val costId = row[0] as String
            val costName = row[1] as String
            val category = row[2] as FixedCostCategory
            val amount = row[3] as BigDecimal
            val supplierName = row[4] as String?
            val frequency = row[5] as CostFrequency

            // Generowanie dat płatności na podstawie częstotliwości
            val nextPaymentDate = calculateNextPaymentDate(currentDate, frequency)
            if (nextPaymentDate != null && !nextPaymentDate.isAfter(targetDate)) {
                upcomingPayments.add(
                    UpcomingPayment(
                        fixedCostId = FixedCostId(costId),
                        fixedCostName = costName,
                        category = category,
                        dueDate = nextPaymentDate,
                        amount = amount,
                        supplierName = supplierName
                    )
                )
            }
        }

        return upcomingPayments.sortedBy { it.dueDate }
    }

    override fun findOverduePayments(): List<OverduePayment> {
        // TODO: Implement based on actual payment tracking
        // For now, return empty list as we need to track actual payment due dates
        return emptyList()
    }

    @Transactional
    override fun savePayment(fixedCostId: FixedCostId, payment: FixedCostPayment): FixedCostPayment {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val fixedCostEntity = fixedCostJpaRepository.findByCompanyIdAndId(companyId, fixedCostId.value)
            .orElseThrow { IllegalArgumentException("Fixed cost not found: ${fixedCostId.value}") }

        val paymentEntity = FixedCostPaymentEntity(
            id = payment.id,
            fixedCost = fixedCostEntity,
            paymentDate = payment.paymentDate,
            amount = payment.amount,
            plannedAmount = payment.plannedAmount,
            status = payment.status,
            paymentMethod = payment.paymentMethod,
            documentId = payment.documentId,
            notes = payment.notes,
            createdAt = payment.createdAt
        )

        val savedPaymentEntity = paymentJpaRepository.save(paymentEntity)
        return savedPaymentEntity.toDomain()
    }

    override fun getPaymentsForPeriod(
        fixedCostId: FixedCostId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<FixedCostPayment> {
        return paymentJpaRepository.findByFixedCostIdAndPaymentDateBetween(fixedCostId.value, startDate, endDate)
            .map { it.toDomain() }
    }

    override fun getPaymentStatistics(fixedCostId: FixedCostId): PaymentStatistics {
        val results = paymentJpaRepository.getPaymentStatistics(fixedCostId.value)

        val totalPaid = results[0] as BigDecimal
        val totalPlanned = results[1] as BigDecimal
        val totalPayments = (results[2] as Long).toInt()
        val onTimePayments = (results[3] as Long).toInt()

        val averageVariance = if (totalPayments > 0) {
            totalPaid.subtract(totalPlanned).divide(BigDecimal(totalPayments), 2, BigDecimal.ROUND_HALF_UP)
        } else BigDecimal.ZERO

        val lastPaymentDate = paymentJpaRepository.findByFixedCostIdOrderByPaymentDateDesc(fixedCostId.value)
            .firstOrNull()?.paymentDate

        return PaymentStatistics(
            totalPaid = totalPaid,
            totalPlanned = totalPlanned,
            averageVariance = averageVariance,
            onTimePayments = onTimePayments,
            latePayments = totalPayments - onTimePayments,
            lastPaymentDate = lastPaymentDate
        )
    }

    // Helper method to calculate next payment date based on frequency
    private fun calculateNextPaymentDate(currentDate: LocalDate, frequency: CostFrequency): LocalDate? {
        return when (frequency) {
            CostFrequency.WEEKLY -> currentDate.plusWeeks(1)
            CostFrequency.MONTHLY -> currentDate.plusMonths(1)
            CostFrequency.QUARTERLY -> currentDate.plusMonths(3)
            CostFrequency.YEARLY -> currentDate.plusYears(1)
            CostFrequency.ONE_TIME -> null
        }
    }
}