package com.carslab.crm.domain.port

import com.carslab.crm.api.model.InvoiceFilterDTO
import com.carslab.crm.domain.model.view.finance.Invoice
import com.carslab.crm.domain.model.view.finance.InvoiceId
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Port repozytorium dla faktur.
 */
interface InvoiceRepository {
    /**
     * Zapisuje nową fakturę lub aktualizuje istniejącą.
     */
    fun save(invoice: Invoice): Invoice

    /**
     * Znajduje fakturę po jej identyfikatorze.
     */
    fun findById(id: InvoiceId): Invoice?

    /**
     * Znajduje wszystkie faktury spełniające podane kryteria filtrowania.
     */
    fun findAll(filter: InvoiceFilterDTO? = null): List<Invoice>

    /**
     * Usuwa fakturę o podanym identyfikatorze.
     */
    fun deleteById(id: InvoiceId): Boolean

    /**
     * Aktualizuje status faktury.
     */
    fun updateStatus(id: InvoiceId, status: String): Boolean

    /**
     * Generuje numer faktury na podstawie roku i typu.
     */
    fun generateInvoiceNumber(year: Int, month: Int, type: String): String

    /**
     * Znajduje przeterminowane faktury.
     */
    fun findOverdueBefore(date: LocalDate): List<Invoice>
}