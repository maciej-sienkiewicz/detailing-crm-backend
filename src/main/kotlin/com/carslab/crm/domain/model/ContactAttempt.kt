package com.carslab.crm.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Typ kontaktu z klientem
 */
enum class ContactAttemptType {
    PHONE, EMAIL, SMS, OTHER
}

/**
 * Rezultat próby kontaktu
 */
enum class ContactAttemptResult {
    SUCCESS, NO_ANSWER, CALLBACK_REQUESTED, REJECTED
}

/**
 * Unikalny identyfikator próby kontaktu
 */
data class ContactAttemptId(val value: String = UUID.randomUUID().toString())

/**
 * Model domenowy próby kontaktu
 */
data class ContactAttempt(
    val id: ContactAttemptId,
    val clientId: String,
    val date: LocalDateTime,
    val type: ContactAttemptType,
    val description: String,
    val result: ContactAttemptResult,
    val audit: ContactAttemptAudit = ContactAttemptAudit()
)

/**
 * Informacje audytowe dla próby kontaktu
 */
data class ContactAttemptAudit(
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)