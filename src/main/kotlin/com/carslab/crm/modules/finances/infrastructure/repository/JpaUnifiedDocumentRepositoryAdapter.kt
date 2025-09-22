package com.carslab.crm.finances.infrastructure.repository

import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.api.model.DocumentType
import com.carslab.crm.api.model.FinancialSummaryResponse
import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.api.model.UnifiedDocumentFilterDTO
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.finances.domain.PaginatedResult
import com.carslab.crm.finances.domain.ports.CounterpartyUsage
import com.carslab.crm.finances.domain.ports.DocumentStatistics
import com.carslab.crm.finances.domain.ports.PaymentMethodUsage
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.finances.infrastructure.entitiy.DocumentAttachmentEntity
import com.carslab.crm.finances.infrastructure.entitiy.DocumentItemEntity
import com.carslab.crm.finances.infrastructure.entitiy.UnifiedDocumentEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.finances.infrastructure.entitiy.CashHistoryBalanceEntity
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrElse

@Repository
class JpaUnifiedDocumentRepositoryAdapter(
    private val documentJpaRepository: UnifiedDocumentJpaRepository,
    private val cashBalancesRepository: CashBalancesRepository,
    private val bankAccountBalanceRepository: BankAccountBalanceRepository,
    private val cashHistoryFlowRepository: JpaCashHistoryFlowRepository,
    private val securityContext: SecurityContext,
) : UnifiedDocumentRepository {

    @Transactional
    override fun save(document: UnifiedFinancialDocument, authContext: AuthContext?): UnifiedFinancialDocument {
        val companyId = authContext?.companyId?.value ?: (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val entity = if (documentJpaRepository.findByCompanyIdAndId(companyId, document.id.value).isPresent) {
            val existingEntity = documentJpaRepository.findByCompanyIdAndId(companyId, document.id.value).get()

            // Aktualizacja podstawowych pól
            existingEntity.type = document.type
            existingEntity.title = document.title
            existingEntity.description = document.description
            existingEntity.issuedDate = document.issuedDate
            existingEntity.dueDate = document.dueDate
            existingEntity.sellerName = document.sellerName
            existingEntity.sellerTaxId = document.sellerTaxId
            existingEntity.sellerAddress = document.sellerAddress
            existingEntity.buyerName = document.buyerName
            existingEntity.buyerTaxId = document.buyerTaxId
            existingEntity.buyerAddress = document.buyerAddress
            existingEntity.status = document.status
            existingEntity.direction = document.direction
            existingEntity.paymentMethod = document.paymentMethod
            existingEntity.totalNet = document.totalNet
            existingEntity.totalTax = document.totalTax
            existingEntity.totalGross = document.totalGross
            existingEntity.paidAmount = document.paidAmount
            existingEntity.currency = document.currency
            existingEntity.notes = document.notes
            existingEntity.protocolId = document.protocolId
            existingEntity.protocolNumber = document.protocolNumber
            existingEntity.visitId = document.visitId
            existingEntity.updatedAt = LocalDateTime.now()

            // Czyszczenie istniejących pozycji
            existingEntity.items.clear()

            existingEntity
        } else {
            val newEntity = UnifiedDocumentEntity.fromDomain(document, authContext)
            // Jeśli numer nie został wygenerowany, generujemy go
            if (newEntity.number.isBlank()) {
                newEntity.number = generateDocumentNumber(
                    document.issuedDate.year,
                    document.issuedDate.month.value,
                    document.type.name,
                    document.direction.name,
                    authContext
                )
            }
            newEntity
        }

        // Zapisanie dokumentu aby uzyskać identyfikator (lub aktualizować istniejący)
        val savedEntity = documentJpaRepository.save(entity)

        // Dodanie pozycji
        document.items.forEach { item ->
            val itemEntity = DocumentItemEntity.fromDomain(item, savedEntity)
            savedEntity.items.add(itemEntity)
        }

        // Dodanie załącznika (jeśli istnieje)
        document.attachment?.let { attachment ->
            val attachmentEntity = DocumentAttachmentEntity.fromDomain(attachment, savedEntity)
            savedEntity.attachment = attachmentEntity
        }

        // Zapisanie dokumentu z pozycjami i załącznikiem
        val finalEntity = documentJpaRepository.save(savedEntity)
        return finalEntity.toDomain()
    }

    override fun findById(id: UnifiedDocumentId, authContext: AuthContext): UnifiedFinancialDocument? {
        return documentJpaRepository.findByCompanyIdAndId(authContext.companyId.value, id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAll(filter: UnifiedDocumentFilterDTO?, page: Int, size: Int): PaginatedResult<UnifiedFinancialDocument> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val pageable: Pageable = PageRequest.of(page, size)

        val result = if (filter == null) {
            documentJpaRepository.findByCompanyId(companyId, pageable)
        } else {
            val status = filter.status?.let { try { DocumentStatus.valueOf(it) } catch (e: IllegalArgumentException) { null } }
            val type = filter.type?.let { try { DocumentType.valueOf(it) } catch (e: IllegalArgumentException) { null } }
            val direction = filter.direction?.let { try { TransactionDirection.valueOf(it) } catch (e: IllegalArgumentException) { null } }
            val paymentMethod = filter.paymentMethod?.let { try { PaymentMethod.valueOf(it) } catch (e: IllegalArgumentException) { null } }

            documentJpaRepository.searchDocumentsAndCompanyId(
                number = filter.number,
                title = filter.title,
                buyerName = filter.buyerName,
                sellerName = filter.sellerName,
                status = status,
                type = type,
                direction = direction,
                paymentMethod = paymentMethod,
                dateFrom = filter.dateFrom,
                dateTo = filter.dateTo,
                protocolId = filter.protocolId,
                visitId = filter.visitId,
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
    override fun deleteById(id: UnifiedDocumentId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val entity = documentJpaRepository.findByCompanyIdAndId(companyId, id.value).orElse(null) ?: return false
        documentJpaRepository.delete(entity)
        return true
    }

    @Transactional
    override fun updateStatus(id: UnifiedDocumentId, status: String): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        try {
            val documentStatus = DocumentStatus.valueOf(status)
            return documentJpaRepository.updateStatusAndCompanyId(id.value, documentStatus, companyId) > 0
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid status: $status")
        }
    }

    @Transactional
    override fun updatePaidAmount(id: UnifiedDocumentId, paidAmount: BigDecimal, newStatus: String): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        try {
            val documentStatus = DocumentStatus.valueOf(newStatus)
            return documentJpaRepository.updatePaidAmountAndStatusAndCompanyId(id.value, paidAmount, documentStatus, companyId) > 0
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid status: $newStatus")
        }
    }

    override fun generateDocumentNumber(year: Int, month: Int, type: String, direction: String, authContext: AuthContext?): String {
        val companyId = authContext?.companyId?.value ?: (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        // Format numeru zależny od typu i kierunku
        val prefix = when {
            type == DocumentType.INVOICE.name && direction == TransactionDirection.INCOME.name -> "FV/$year/$month/"
            type == DocumentType.INVOICE.name && direction == TransactionDirection.EXPENSE.name -> "FZ/$year/$month/"
            type == DocumentType.RECEIPT.name && direction == TransactionDirection.INCOME.name -> "PAR/$year/$month/"
            type == DocumentType.RECEIPT.name && direction == TransactionDirection.EXPENSE.name -> "WYD/$year/$month/"
            else -> "DOK/$year/$month/"
        }

        // Pobierz ostatni numer dokumentu z podanym prefixem dla danej firmy
        val lastNumber = documentJpaRepository.findLastNumberByPrefixAndCompanyId(prefix, companyId)

        // Jeśli nie ma jeszcze dokumentów z tym prefixem, rozpocznij od 1
        if (lastNumber == null) {
            return "${prefix}0001"
        }

        // Wyciągnij numer z ostatniego dokumentu i zwiększ o 1
        val lastNumberValue = lastNumber.substring(prefix.length).toIntOrNull() ?: 0
        val newNumberValue = lastNumberValue + 1

        // Formatuj numer z wiodącymi zerami (np. 0001, 0012, 0123)
        return "$prefix${newNumberValue.toString().padStart(4, '0')}"
    }

    override fun findOverdueBefore(date: LocalDate): List<UnifiedFinancialDocument> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return documentJpaRepository.findByDueDateBeforeAndCompanyId(date, companyId)
            .filter { it.status == DocumentStatus.NOT_PAID || it.status == DocumentStatus.PARTIALLY_PAID }
            .map { it.toDomain() }
    }

    override fun findByProtocolId(protocolId: String): List<UnifiedFinancialDocument> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return documentJpaRepository.findByProtocolIdAndCompanyId(protocolId, companyId)
            .map { it.toDomain() }
    }

    override fun findByVisitId(visitId: String): List<UnifiedFinancialDocument> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return documentJpaRepository.findByVisitIdAndCompanyId(visitId, companyId)
            .map { it.toDomain() }
    }

    override fun getFinancialSummary(dateFrom: LocalDate?, dateTo: LocalDate?): FinancialSummaryResponse {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val totalIncome = documentJpaRepository.calculateTotalForPeriodAndCompanyId(
            dateFrom, dateTo, TransactionDirection.INCOME, null, companyId
        )

        val totalExpense = documentJpaRepository.calculateTotalForPeriodAndCompanyId(
            dateFrom, dateTo, TransactionDirection.EXPENSE, null, companyId
        )

        val totalPaid = documentJpaRepository.calculateTotalPaidForPeriodAndCompanyId(
            dateFrom, dateTo, companyId
        )

        val receivables = documentJpaRepository.calculateTotalForPeriodAndCompanyId(
            dateFrom, dateTo, TransactionDirection.INCOME, DocumentStatus.NOT_PAID, companyId
        ) + documentJpaRepository.calculateTotalForPeriodAndCompanyId(
            dateFrom, dateTo, TransactionDirection.INCOME, DocumentStatus.PARTIALLY_PAID, companyId
        )

        val liabilities = documentJpaRepository.calculateTotalForPeriodAndCompanyId(
            dateFrom, dateTo, TransactionDirection.EXPENSE, DocumentStatus.NOT_PAID, companyId
        ) + documentJpaRepository.calculateTotalForPeriodAndCompanyId(
            dateFrom, dateTo, TransactionDirection.EXPENSE, DocumentStatus.PARTIALLY_PAID, companyId
        )

        val today = LocalDate.now()
        val receivablesOverdue = documentJpaRepository.calculateOverdueStatusAndCompanyId(today, companyId)
        val liabilitiesOverdue = BigDecimal.ZERO // Można dodać osobne zapytanie dla zobowiązań przeterminowanych

        val profit = totalIncome - totalExpense
        val cashFlow = totalPaid

        // Można rozszerzyć o dodatkowe statystyki per metodę płatności
        val paymentMethodStats = documentJpaRepository.getPaymentMethodStatsAndCompanyId(companyId)
        val incomeByMethod = paymentMethodStats.associate {
            (it[0] as PaymentMethod).name to (it[2] as BigDecimal)
        }

        return FinancialSummaryResponse(
            cashBalance = cashBalancesRepository.findById(companyId).map { it.amount }.getOrElse { BigDecimal.ZERO },
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            bankAccountBalance = bankAccountBalanceRepository.findById(companyId).map { it.amount }
                .getOrElse { BigDecimal.ZERO },
            receivables = receivables,
            receivablesOverdue = receivablesOverdue,
            liabilities = liabilities,
            liabilitiesOverdue = liabilitiesOverdue,
            profit = profit,
            cashFlow = cashFlow,
            incomeByMethod = incomeByMethod,
            expenseByMethod = emptyMap(), // Można dodać osobne zapytanie
            receivablesByTimeframe = emptyMap(), // Można dodać osobne zapytanie
            liabilitiesByTimeframe = emptyMap() // Można dodać osobne zapytanie
        )
    }

    override fun getChartData(period: String): Map<String, Any> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val today = LocalDate.now()

        val (dateFrom, dateTo) = when (period.lowercase()) {
            "week" -> {
                val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                weekStart to today
            }
            "month" -> {
                val monthStart = today.withDayOfMonth(1)
                monthStart to today
            }
            "quarter" -> {
                val quarterStart = today.minusMonths(3).withDayOfMonth(1)
                quarterStart to today
            }
            "year" -> {
                val yearStart = today.withDayOfYear(1)
                yearStart to today
            }
            else -> {
                today.minusMonths(1) to today
            }
        }

        val chartData = when (period.lowercase()) {
            "week" -> documentJpaRepository.getDailyChartDataAndCompanyId(dateFrom, dateTo, companyId)
            "month" -> documentJpaRepository.getWeeklyChartDataAndCompanyId(dateFrom, dateTo, companyId)
            else -> documentJpaRepository.getMonthlyChartDataAndCompanyId(dateFrom, dateTo, companyId)
        }

        val incomeData = mutableListOf<Map<String, Any>>()
        val expenseData = mutableListOf<Map<String, Any>>()

        chartData.forEach { row ->
            val direction = row[row.size - 2] as TransactionDirection
            val amount = row[row.size - 1] as BigDecimal

            val dataPoint = when (period.lowercase()) {
                "week" -> {
                    val date = row[0] as LocalDate
                    mapOf("date" to date.toString(), "amount" to amount)
                }
                "month" -> {
                    val year = row[0] as Int
                    val week = row[1] as Int
                    mapOf("period" to "$year-W$week", "amount" to amount)
                }
                else -> {
                    val year = row[0] as Int
                    val month = row[1] as Int
                    mapOf("period" to "$year-${month.toString().padStart(2, '0')}", "amount" to amount)
                }
            }

            if (direction == TransactionDirection.INCOME) {
                incomeData.add(dataPoint)
            } else {
                expenseData.add(dataPoint)
            }
        }

        return mapOf(
            "income" to incomeData,
            "expense" to expenseData,
            "period" to period,
            "dateFrom" to dateFrom.toString(),
            "dateTo" to dateTo.toString()
        )
    }

    override fun findByStatus(status: String): List<UnifiedFinancialDocument> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val documentStatus = DocumentStatus.valueOf(status)
        return documentJpaRepository.findByStatusAndCompanyId(documentStatus, companyId)
            .map { it.toDomain() }
    }

    override fun findByTypeAndDirection(type: String, direction: String): List<UnifiedFinancialDocument> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val documentType = DocumentType.valueOf(type)
        val transactionDirection = TransactionDirection.valueOf(direction)
        return documentJpaRepository.findByTypeAndDirectionAndCompanyId(documentType, transactionDirection, companyId)
            .map { it.toDomain() }
    }

    override fun calculateTotalForPeriod(
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        direction: String?,
        status: String?
    ): BigDecimal {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val transactionDirection = direction?.let { TransactionDirection.valueOf(it) }
        val documentStatus = status?.let { DocumentStatus.valueOf(it) }

        return documentJpaRepository.calculateTotalForPeriodAndCompanyId(
            dateFrom, dateTo, transactionDirection, documentStatus, companyId
        )
    }

    override fun getDocumentStatistics(dateFrom: LocalDate?, dateTo: LocalDate?): DocumentStatistics {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val totalDocuments = documentJpaRepository.countDocumentsForPeriodAndCompanyId(dateFrom, dateTo, companyId)
        val totalIncome = calculateTotalForPeriod(dateFrom, dateTo, TransactionDirection.INCOME.name, null)
        val totalExpense = calculateTotalForPeriod(dateFrom, dateTo, TransactionDirection.EXPENSE.name, null)
        val totalPaid = documentJpaRepository.calculateTotalPaidForPeriodAndCompanyId(dateFrom, dateTo, companyId)
        val totalUnpaid = (totalIncome + totalExpense) - totalPaid

        val today = LocalDate.now()
        val overdueDocuments = documentJpaRepository.countOverdueDocumentsAndCompanyId(today, companyId)
        val overdueAmount = documentJpaRepository.calculateOverdueStatusAndCompanyId(today, companyId)

        val averageDocumentValue = if (totalDocuments > 0) {
            (totalIncome + totalExpense).divide(BigDecimal(totalDocuments), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return DocumentStatistics(
            totalDocuments = totalDocuments,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalPaid = totalPaid,
            totalUnpaid = totalUnpaid,
            overdueDocuments = overdueDocuments,
            overdueAmount = overdueAmount,
            averageDocumentValue = averageDocumentValue
        )
    }

    override fun getTopPaymentMethods(limit: Int): List<PaymentMethodUsage> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val stats = documentJpaRepository.getPaymentMethodStatsAndCompanyId(companyId)

        val totalCount = stats.sumOf { (it[1] as Long) }

        return stats.take(limit).map { row ->
            val method = row[0] as PaymentMethod
            val count = row[1] as Long
            val totalAmount = row[2] as BigDecimal
            val percentage = if (totalCount > 0) (count.toDouble() / totalCount.toDouble()) * 100 else 0.0

            PaymentMethodUsage(
                paymentMethod = method.name,
                count = count,
                totalAmount = totalAmount,
                percentage = percentage
            )
        }
    }

    override fun getTopCounterparties(limit: Int, type: String): List<CounterpartyUsage> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val pageable = PageRequest.of(0, limit)

        val stats = when (type.uppercase()) {
            "SELLER" -> documentJpaRepository.getSellerStatsAndCompanyId(companyId, pageable)
            else -> documentJpaRepository.getBuyerStatsAndCompanyId(companyId, pageable)
        }

        return stats.map { row ->
            val name = row[0] as String
            val taxId = row[1] as String?
            val count = row[2] as Long
            val totalAmount = row[3] as BigDecimal

            CounterpartyUsage(
                name = name,
                taxId = taxId,
                count = count,
                totalAmount = totalAmount,
                type = type
            )
        }
    }

    override fun addAmountToCashBalance(
        companyId: Long,
        amount: BigDecimal,
        lastUpdate: String
    ): Int {
        // Pobierz aktualny stan kasy
        val currentBalance = cashBalancesRepository.findByCompanyId(companyId)
            .map { it.amount }
            .orElse(BigDecimal.ZERO)

        // Oblicz nowy stan po operacji
        val newBalance = currentBalance + amount

        // Najpierw zapisz historię operacji
        val historyEntity = CashHistoryBalanceEntity(
            id = 0L, // ID będzie wygenerowane automatycznie przez bazę danych
            companyId = companyId,
            previousAmount = currentBalance,
            afterOperation = newBalance,
            lastUpdate = lastUpdate
        )
        cashHistoryFlowRepository.save(historyEntity)

        // Następnie zaktualizuj aktualny stan kasy
        return cashBalancesRepository.addAmountToBalance(companyId, amount, lastUpdate)
    }

    override fun subtractAmountFromCashBalance(
        companyId: Long,
        amount: BigDecimal,
        lastUpdate: String
    ): Int {
        // Pobierz aktualny stan kasy
        val currentBalance = cashBalancesRepository.findByCompanyId(companyId)
            .map { it.amount }
            .orElse(BigDecimal.ZERO)

        // Oblicz nowy stan po operacji
        val newBalance = currentBalance - amount

        // Najpierw zapisz historię operacji
        val historyEntity = CashHistoryBalanceEntity(
            id = 0L, // ID będzie wygenerowane automatycznie przez bazę danych
            companyId = companyId,
            previousAmount = currentBalance,
            afterOperation = newBalance,
            lastUpdate = lastUpdate
        )
        cashHistoryFlowRepository.save(historyEntity)

        // Następnie zaktualizuj aktualny stan kasy
        return cashBalancesRepository.subtractAmountFromBalance(companyId, amount, lastUpdate)
    }

    override fun findInvoicesByCompanyAndDateRange(
        companyId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<UnifiedFinancialDocument> =
        documentJpaRepository.findInvoicesByCompanyAndDateRange(companyId, startDate, endDate)
            .map { it.toDomain() }
}