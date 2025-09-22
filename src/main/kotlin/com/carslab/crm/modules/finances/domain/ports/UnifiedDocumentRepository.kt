package com.carslab.crm.finances.domain.ports

import com.carslab.crm.api.model.FinancialSummaryResponse
import com.carslab.crm.api.model.UnifiedDocumentFilterDTO
import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.finances.domain.PaginatedResult
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Port repozytorium dla zunifikowanych dokumentów finansowych.
 */
interface UnifiedDocumentRepository {

    /**
     * Zapisuje nowy dokument lub aktualizuje istniejący.
     */
    fun save(document: UnifiedFinancialDocument, authContext: AuthContext? = null): UnifiedFinancialDocument

    /**
     * Znajduje dokument po jego identyfikatorze.
     */
    fun findById(id: UnifiedDocumentId, authContext: AuthContext): UnifiedFinancialDocument?

    /**
     * Znajduje wszystkie dokumenty spełniające podane kryteria filtrowania z paginacją.
     */
    fun findAll(filter: UnifiedDocumentFilterDTO? = null, page: Int = 0, size: Int = 10): PaginatedResult<UnifiedFinancialDocument>

    /**
     * Usuwa dokument o podanym identyfikatorze.
     */
    fun deleteById(id: UnifiedDocumentId): Boolean

    /**
     * Aktualizuje status dokumentu.
     */
    fun updateStatus(id: UnifiedDocumentId, status: String): Boolean

    /**
     * Aktualizuje kwotę zapłaconą i status dokumentu.
     */
    fun updatePaidAmount(id: UnifiedDocumentId, paidAmount: BigDecimal, newStatus: String): Boolean

    /**
     * Generuje numer dokumentu na podstawie roku, miesiąca i typu.
     */
    fun generateDocumentNumber(year: Int, month: Int, type: String, direction: String, authContext: AuthContext? = null): String

    /**
     * Znajduje przeterminowane dokumenty.
     */
    fun findOverdueBefore(date: LocalDate): List<UnifiedFinancialDocument>

    /**
     * Znajduje dokumenty powiązane z protokołem.
     */
    fun findByProtocolId(protocolId: String): List<UnifiedFinancialDocument>

    /**
     * Znajduje dokumenty powiązane z wizytą.
     */
    fun findByVisitId(visitId: String): List<UnifiedFinancialDocument>

    /**
     * Pobiera podsumowanie finansowe za określony okres.
     */
    fun getFinancialSummary(dateFrom: LocalDate?, dateTo: LocalDate?): FinancialSummaryResponse

    /**
     * Pobiera dane do wykresów za określony okres.
     */
    fun getChartData(period: String): Map<String, Any>

    /**
     * Znajduje dokumenty według statusu.
     */
    fun findByStatus(status: String): List<UnifiedFinancialDocument>

    /**
     * Znajduje dokumenty według typu i kierunku.
     */
    fun findByTypeAndDirection(type: String, direction: String): List<UnifiedFinancialDocument>

    /**
     * Oblicza sumę dla dokumentów w określonym okresie.
     */
    fun calculateTotalForPeriod(
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        direction: String? = null,
        status: String? = null
    ): BigDecimal

    /**
     * Pobiera statystyki dokumentów za dany okres.
     */
    fun getDocumentStatistics(dateFrom: LocalDate?, dateTo: LocalDate?): DocumentStatistics

    /**
     * Znajduje najczęściej używane metody płatności.
     */
    fun getTopPaymentMethods(limit: Int = 5): List<PaymentMethodUsage>

    /**
     * Znajduje najczęstszych kontrahentów.
     */
    fun getTopCounterparties(limit: Int = 10, type: String = "BUYER"): List<CounterpartyUsage>

    fun addAmountToCashBalance(companyId: Long, amount: BigDecimal, lastUpdate: String): Int

    fun subtractAmountFromCashBalance(companyId: Long, amount: BigDecimal, lastUpdate: String): Int

    fun findInvoicesByCompanyAndDateRange(
        companyId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<UnifiedFinancialDocument>
}

/**
 * Statystyki dokumentów.
 */
data class DocumentStatistics(
    val totalDocuments: Long,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val totalPaid: BigDecimal,
    val totalUnpaid: BigDecimal,
    val overdueDocuments: Long,
    val overdueAmount: BigDecimal,
    val averageDocumentValue: BigDecimal
)

/**
 * Statystyki użycia metod płatności.
 */
data class PaymentMethodUsage(
    val paymentMethod: String,
    val count: Long,
    val totalAmount: BigDecimal,
    val percentage: Double
)

/**
 * Statystyki użycia kontrahentów.
 */
data class CounterpartyUsage(
    val name: String,
    val taxId: String?,
    val count: Long,
    val totalAmount: BigDecimal,
    val type: String // BUYER lub SELLER
)