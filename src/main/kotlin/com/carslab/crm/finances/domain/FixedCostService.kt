// src/main/kotlin/com/carslab/crm/finances/domain/FixedCostService.kt
package com.carslab.crm.finances.domain

import com.carslab.crm.api.model.*
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.finances.domain.model.fixedcosts.CostFrequency
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCost
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostId
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostPayment
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostStatus
import com.carslab.crm.finances.domain.model.fixedcosts.PaymentStatus
import com.carslab.crm.finances.domain.model.fixedcosts.SupplierInfo
import com.carslab.crm.finances.domain.model.fixedcosts.TrendDirection
import com.carslab.crm.finances.domain.ports.fixedcosts.FixedCostRepository
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class FixedCostService(
    private val fixedCostRepository: FixedCostRepository
) {

    private val logger = LoggerFactory.getLogger(FixedCostService::class.java)

    /**
     * Tworzy nowy koszt stały
     */
    @Transactional
    fun createFixedCost(request: CreateFixedCostRequest): FixedCost {
        logger.info("Creating new fixed cost: {}", request.name)

        validateFixedCostRequest(request)

        val fixedCost = FixedCost(
            id = FixedCostId.generate(),
            name = request.name,
            description = request.description,
            category = request.category,
            monthlyAmount = request.monthlyAmount,
            frequency = request.frequency,
            startDate = request.startDate,
            endDate = request.endDate,
            status = request.status,
            autoRenew = request.autoRenew,
            supplierInfo = request.supplierInfo?.let {
                SupplierInfo(name = it.name, taxId = it.taxId)
            },
            contractNumber = request.contractNumber,
            notes = request.notes,
            payments = emptyList(),
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val savedFixedCost = fixedCostRepository.save(fixedCost)
        logger.info("Created fixed cost with ID: {}", savedFixedCost.id.value)

        return savedFixedCost
    }

    /**
     * Aktualizuje istniejący koszt stały
     */
    @Transactional
    fun updateFixedCost(id: String, request: UpdateFixedCostRequest): FixedCost {
        logger.info("Updating fixed cost with ID: {}", id)

        val existingFixedCost = fixedCostRepository.findById(FixedCostId(id))
            ?: throw ResourceNotFoundException("FixedCost", id)

        validateFixedCostRequest(request)

        val updatedFixedCost = existingFixedCost.copy(
            name = request.name,
            description = request.description,
            category = request.category,
            monthlyAmount = request.monthlyAmount,
            frequency = request.frequency,
            startDate = request.startDate,
            endDate = request.endDate,
            status = request.status,
            autoRenew = request.autoRenew,
            supplierInfo = request.supplierInfo?.let {
                SupplierInfo(name = it.name, taxId = it.taxId)
            },
            contractNumber = request.contractNumber,
            notes = request.notes,
            audit = existingFixedCost.audit.copy(updatedAt = LocalDateTime.now())
        )

        val savedFixedCost = fixedCostRepository.save(updatedFixedCost)
        logger.info("Updated fixed cost with ID: {}", savedFixedCost.id.value)

        return savedFixedCost
    }

    /**
     * Pobiera koszt stały po ID
     */
    fun getFixedCostById(id: String): FixedCost {
        logger.debug("Getting fixed cost by ID: {}", id)
        return fixedCostRepository.findById(FixedCostId(id))
            ?: throw ResourceNotFoundException("FixedCost", id)
    }

    /**
     * Pobiera wszystkie koszty stałe z filtrowaniem i paginacją
     */
    fun getAllFixedCosts(
        filter: FixedCostFilterDTO? = null,
        page: Int = 0,
        size: Int = 20
    ): PaginatedResult<FixedCost> {
        logger.debug("Getting all fixed costs with filter: {}, page: {}, size: {}", filter, page, size)
        return fixedCostRepository.findAll(filter, page, size)
    }

    /**
     * Usuwa koszt stały
     */
    @Transactional
    fun deleteFixedCost(id: String): Boolean {
        logger.info("Deleting fixed cost with ID: {}", id)

        if (fixedCostRepository.findById(FixedCostId(id)) == null) {
            throw ResourceNotFoundException("FixedCost", id)
        }

        return fixedCostRepository.deleteById(FixedCostId(id))
    }

    /**
     * Rejestruje płatność dla kosztu stałego
     */
    @Transactional
    fun recordPayment(fixedCostId: String, request: RecordPaymentRequest): FixedCostPayment {
        logger.info("Recording payment for fixed cost ID: {}", fixedCostId)

        val fixedCost = fixedCostRepository.findById(FixedCostId(fixedCostId))
            ?: throw ResourceNotFoundException("FixedCost", fixedCostId)

        validatePaymentRequest(request)

        val payment = FixedCostPayment(
            id = UUID.randomUUID().toString(),
            paymentDate = request.paymentDate,
            amount = request.amount,
            plannedAmount = request.plannedAmount,
            status = request.status,
            paymentMethod = request.paymentMethod?.let { PaymentMethod.valueOf(it) },
            documentId = request.documentId,
            notes = request.notes,
            createdAt = LocalDateTime.now()
        )

        val savedPayment = fixedCostRepository.savePayment(FixedCostId(fixedCostId), payment)
        logger.info("Recorded payment with ID: {}", savedPayment.id)

        return savedPayment
    }

    /**
     * Pobiera podsumowanie kosztów według kategorii
     */
    fun getCategorySummary(period: LocalDate? = null): CategorySummaryResponse {
        logger.debug("Getting category summary for period: {}", period)

        val summaryPeriod = period ?: LocalDate.now()
        val startOfMonth = summaryPeriod.withDayOfMonth(1)
        val endOfMonth = summaryPeriod.withDayOfMonth(summaryPeriod.lengthOfMonth())

        val categorySummary = fixedCostRepository.getCategorySummary(summaryPeriod)
        val activeCosts = fixedCostRepository.findActiveInPeriod(startOfMonth, endOfMonth)

        val totalFixedCosts = categorySummary.values.sumOf { it }
        val activeCostsCount = activeCosts.size
        val inactiveCostsCount = fixedCostRepository.findByStatus(FixedCostStatus.INACTIVE).size

        val categoryBreakdown = categorySummary.map { (category, amount) ->
            val categoryActiveCosts = activeCosts.filter { it.category == category }
            val percentage = if (totalFixedCosts > BigDecimal.ZERO) {
                amount.divide(totalFixedCosts, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal(100))
            } else BigDecimal.ZERO

            val topCosts = categoryActiveCosts
                .sortedByDescending { it.calculateMonthlyAmount() }
                .take(3)
                .map { cost ->
                    TopCostItemResponse(
                        id = cost.id.value,
                        name = cost.name,
                        amount = cost.calculateMonthlyAmount(),
                        percentage = if (amount > BigDecimal.ZERO) {
                            cost.calculateMonthlyAmount().divide(amount, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal(100))
                        } else BigDecimal.ZERO,
                        status = cost.status
                    )
                }

            CategoryBreakdownItem(
                category = category,
                categoryDisplay = category.displayName,
                totalAmount = amount,
                percentage = percentage,
                activeCosts = categoryActiveCosts.size,
                trend = TrendDirection.STABLE, // TODO: Implement trend calculation
                topCosts = topCosts
            )
        }

        return CategorySummaryResponse(
            categories = categoryBreakdown,
            totalFixedCosts = totalFixedCosts,
            period = summaryPeriod,
            activeCostsCount = activeCostsCount,
            inactiveCostsCount = inactiveCostsCount
        )
    }

    /**
     * Pobiera nadchodzące płatności
     */
    fun getUpcomingPayments(days: Int = 30): UpcomingPaymentsResponse {
        logger.debug("Getting upcoming payments for next {} days", days)

        val upcomingPayments = fixedCostRepository.findUpcomingPayments(days)
        val overduePayments = fixedCostRepository.findOverduePayments()

        val totalAmount = upcomingPayments.sumOf { it.amount }
        val overdueAmount = overduePayments.sumOf { it.amount }

        val paymentItems = upcomingPayments.map { payment ->
            UpcomingPaymentItem(
                fixedCostId = payment.fixedCostId.value,
                fixedCostName = payment.fixedCostName,
                category = payment.category,
                categoryDisplay = payment.category.displayName,
                dueDate = payment.dueDate,
                amount = payment.amount,
                status = PaymentStatus.PLANNED,
                statusDisplay = PaymentStatus.PLANNED.displayName,
                isOverdue = false,
                daysOverdue = null,
                supplierName = payment.supplierName
            )
        } + overduePayments.map { payment ->
            UpcomingPaymentItem(
                fixedCostId = payment.fixedCostId.value,
                fixedCostName = payment.fixedCostName,
                category = payment.category,
                categoryDisplay = payment.category.displayName,
                dueDate = payment.dueDate,
                amount = payment.amount,
                status = PaymentStatus.OVERDUE,
                statusDisplay = PaymentStatus.OVERDUE.displayName,
                isOverdue = true,
                daysOverdue = payment.daysOverdue,
                supplierName = payment.supplierName
            )
        }

        return UpcomingPaymentsResponse(
            period = "Next $days days",
            totalAmount = totalAmount,
            paymentsCount = upcomingPayments.size,
            overdueCount = overduePayments.size,
            overdueAmount = overdueAmount,
            payments = paymentItems.sortedBy { it.dueDate }
        )
    }

    /**
     * Pobiera płatności dla kosztu stałego w określonym okresie
     */
    fun getPaymentsForPeriod(
        fixedCostId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<FixedCostPayment> {
        logger.debug("Getting payments for fixed cost {} in period {} to {}", fixedCostId, startDate, endDate)

        if (fixedCostRepository.findById(FixedCostId(fixedCostId)) == null) {
            throw ResourceNotFoundException("FixedCost", fixedCostId)
        }

        return fixedCostRepository.getPaymentsForPeriod(FixedCostId(fixedCostId), startDate, endDate)
    }

    /**
     * Pobiera aktywne koszty stałe w okresie
     */
    fun getActiveFixedCostsInPeriod(startDate: LocalDate, endDate: LocalDate): List<FixedCost> {
        logger.debug("Getting active fixed costs in period {} to {}", startDate, endDate)
        return fixedCostRepository.findActiveInPeriod(startDate, endDate)
    }

    /**
     * Oblicza łączne koszty stałe dla okresu
     */
    fun calculateTotalFixedCostsForPeriod(startDate: LocalDate, endDate: LocalDate): BigDecimal {
        logger.debug("Calculating total fixed costs for period {} to {}", startDate, endDate)
        return fixedCostRepository.calculateTotalFixedCostsForPeriod(startDate, endDate)
    }

    // ============ PRIVATE METHODS ============

    private fun validateFixedCostRequest(request: Any) {
        when (request) {
            is CreateFixedCostRequest -> {
                if (request.monthlyAmount <= BigDecimal.ZERO) {
                    throw ValidationException("Monthly amount must be greater than zero")
                }

                if (request.endDate != null && request.endDate.isBefore(request.startDate)) {
                    throw ValidationException("End date cannot be before start date")
                }

                if (request.frequency == CostFrequency.ONE_TIME && request.endDate == null) {
                    throw ValidationException("One-time costs must have an end date")
                }
            }

            is UpdateFixedCostRequest -> {
                if (request.monthlyAmount <= BigDecimal.ZERO) {
                    throw ValidationException("Monthly amount must be greater than zero")
                }

                if (request.endDate != null && request.endDate.isBefore(request.startDate)) {
                    throw ValidationException("End date cannot be before start date")
                }

                if (request.frequency == CostFrequency.ONE_TIME && request.endDate == null) {
                    throw ValidationException("One-time costs must have an end date")
                }
            }
        }
    }

    private fun validatePaymentRequest(request: RecordPaymentRequest) {
        if (request.amount <= BigDecimal.ZERO) {
            throw ValidationException("Payment amount must be greater than zero")
        }

        if (request.plannedAmount <= BigDecimal.ZERO) {
            throw ValidationException("Planned amount must be greater than zero")
        }

        if (request.paymentDate.isAfter(LocalDate.now().plusDays(30))) {
            throw ValidationException("Payment date cannot be more than 30 days in the future")
        }

        request.paymentMethod?.let { method ->
            try {
                PaymentMethod.valueOf(method)
            } catch (e: IllegalArgumentException) {
                throw ValidationException("Invalid payment method: $method")
            }
        }
    }
}