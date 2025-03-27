package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.ContactAttempt
import com.carslab.crm.domain.model.ContactAttemptId
import com.carslab.crm.domain.model.ContactAttemptResult
import com.carslab.crm.domain.model.ContactAttemptType
import com.carslab.crm.domain.port.ContactAttemptRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementacja repozytorium prób kontaktu w pamięci
 */
@Repository
class InMemoryContactAttemptRepository : ContactAttemptRepository {

    // Używamy ConcurrentHashMap dla thread safety
    private val contactAttempts = ConcurrentHashMap<String, ContactAttempt>()

    override fun save(contactAttempt: ContactAttempt): ContactAttempt {
        contactAttempts[contactAttempt.id.value] = contactAttempt
        return contactAttempt
    }

    override fun findById(id: ContactAttemptId): ContactAttempt? {
        return contactAttempts[id.value]
    }

    override fun findAll(): List<ContactAttempt> {
        return contactAttempts.values.toList()
    }

    override fun deleteById(id: ContactAttemptId): Boolean {
        return contactAttempts.remove(id.value) != null
    }

    override fun findByClientId(clientId: String): List<ContactAttempt> {
        return contactAttempts.values.filter { it.clientId == clientId }
    }

    override fun findByType(type: ContactAttemptType): List<ContactAttempt> {
        return contactAttempts.values.filter { it.type == type }
    }

    override fun findByResult(result: ContactAttemptResult): List<ContactAttempt> {
        return contactAttempts.values.filter { it.result == result }
    }

    override fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<ContactAttempt> {
        return contactAttempts.values.filter {
            it.date in startDate..endDate
        }
    }

    override fun findContactAttemptsByClientId(clientId: String): List<ContactAttempt> {
        return contactAttempts.values
            .filter { it.clientId == clientId }
            .sortedByDescending { it.date }
    }
}