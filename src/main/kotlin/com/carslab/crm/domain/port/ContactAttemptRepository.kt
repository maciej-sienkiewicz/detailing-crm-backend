package com.carslab.crm.domain.port

import com.carslab.crm.domain.model.ContactAttempt
import com.carslab.crm.domain.model.ContactAttemptId
import com.carslab.crm.domain.model.ContactAttemptResult
import com.carslab.crm.domain.model.ContactAttemptType
import java.time.LocalDateTime

/**
 * Interfejs repozytorium prób kontaktu
 */
interface ContactAttemptRepository {
    /**
     * Zapisuje próbę kontaktu w repozytorium
     */
    fun save(contactAttempt: ContactAttempt): ContactAttempt

    /**
     * Znajduje próbę kontaktu po ID
     */
    fun findById(id: ContactAttemptId): ContactAttempt?

    /**
     * Zwraca wszystkie próby kontaktu
     */
    fun findAll(): List<ContactAttempt>

    /**
     * Usuwa próbę kontaktu po ID
     * @return true, jeśli próba kontaktu została usunięta, false w przeciwnym razie
     */
    fun deleteById(id: ContactAttemptId): Boolean

    /**
     * Wyszukuje próby kontaktu dla określonego klienta
     */
    fun findByClientId(clientId: String): List<ContactAttempt>

    /**
     * Wyszukuje próby kontaktu według typu
     */
    fun findByType(type: ContactAttemptType): List<ContactAttempt>

    /**
     * Wyszukuje próby kontaktu według rezultatu
     */
    fun findByResult(result: ContactAttemptResult): List<ContactAttempt>

    /**
     * Wyszukuje próby kontaktu w określonym przedziale dat
     */
    fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<ContactAttempt>

    fun findContactAttemptsByClientId(clientId: String): List<ContactAttempt>
}