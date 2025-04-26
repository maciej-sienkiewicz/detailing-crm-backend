package com.carslab.crm.domain.port

import com.carslab.crm.api.model.CashTransactionFilterDTO
import com.carslab.crm.domain.model.view.finance.CashStatistics
import com.carslab.crm.domain.model.view.finance.CashTransaction
import com.carslab.crm.domain.model.view.finance.TransactionId
import com.carslab.crm.domain.model.view.finance.TransactionType
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Port repozytorium dla transakcji gotówkowych.
 */
interface CashRepository {
    /**
     * Zapisuje nową transakcję lub aktualizuje istniejącą.
     */
    fun save(transaction: CashTransaction): CashTransaction

    /**
     * Znajduje transakcję po jej identyfikatorze.
     */
    fun findById(id: TransactionId): CashTransaction?

    /**
     * Znajduje wszystkie transakcje spełniające podane kryteria filtrowania z paginacją.
     */
    fun findAll(
        filter: CashTransactionFilterDTO? = null,
        page: Int = 0,
        size: Int = 20
    ): Pair<List<CashTransaction>, Long>

    /**
     * Usuwa transakcję o podanym identyfikatorze.
     */
    fun deleteById(id: TransactionId): Boolean

    /**
     * Oblicza aktualny stan kasy.
     */
    fun getCurrentBalance(): BigDecimal

    /**
     * Pobiera statystyki gotówkowe za podany okres.
     */
    fun getStatisticsForPeriod(startDate: LocalDate, endDate: LocalDate): CashStatistics

    /**
     * Pobiera statystyki gotówkowe za bieżący miesiąc.
     */
    fun getCurrentMonthStatistics(): CashStatistics

    /**
     * Znajduje transakcje dla powiązanej wizyty.
     */
    fun findByVisitId(visitId: String): List<CashTransaction>

    /**
     * Znajduje transakcje dla powiązanej faktury.
     */
    fun findByInvoiceId(invoiceId: String): List<CashTransaction>

    /**
     * Znajduje transakcje według typu.
     */
    fun findByType(type: TransactionType): List<CashTransaction>
}