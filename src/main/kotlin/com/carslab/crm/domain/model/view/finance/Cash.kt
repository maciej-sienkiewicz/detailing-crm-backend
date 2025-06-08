package com.carslab.crm.domain.model.view.finance

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.UserId
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Unikalny identyfikator transakcji gotówkowej.
 */
data class TransactionId(val value: String) {
    companion object {
        fun generate(): TransactionId = TransactionId(UUID.randomUUID().toString())
    }
}

/**
 * Typ transakcji gotówkowej.
 */
enum class TransactionType {
    INCOME,     // Wpłata do kasy
    EXPENSE     // Wypłata z kasy
}

/**
 * Model domenowy transakcji gotówkowej.
 */
data class CashTransaction(
    val id: TransactionId,
    val type: TransactionType,
    val description: String,
    val date: LocalDate,
    val amount: BigDecimal,
    val visitId: String?,             // Powiązanie z wizytą (opcjonalne)
    val createdBy: UserId,            // Użytkownik, który wprowadził transakcję
    val audit: Audit
)

/**
 * Statystyki gotówkowe dla określonego okresu.
 */
data class CashStatistics(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val income: BigDecimal,           // Suma wpłat
    val expense: BigDecimal,          // Suma wypłat
    val balance: BigDecimal,          // Różnica (income - expense)
    val transactionCount: Int         // Liczba transakcji
)