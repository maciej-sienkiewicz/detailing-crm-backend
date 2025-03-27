package com.carslab.crm.api.mapper

import com.carslab.crm.domain.model.ContactAttempt
import com.carslab.crm.domain.model.ContactAttemptId
import com.carslab.crm.domain.model.ContactAttemptResult
import com.carslab.crm.domain.model.ContactAttemptType
import com.carslab.crm.api.model.request.ContactAttemptRequest
import com.carslab.crm.api.model.response.ContactAttemptResponse

object ContactAttemptMapper {
    /**
     * Konwertuje żądanie (DTO) na model domenowy.
     */
    fun toDomain(request: ContactAttemptRequest): ContactAttempt {
        return ContactAttempt(
            id = request.id?.let { ContactAttemptId(it) } ?: ContactAttemptId(),
            clientId = request.clientId,
            date = request.date,
            type = mapStringToType(request.type),
            description = request.description,
            result = mapStringToResult(request.result)
        )
    }

    /**
     * Konwertuje model domenowy na odpowiedź (DTO).
     */
    fun toResponse(contactAttempt: ContactAttempt): ContactAttemptResponse {
        return ContactAttemptResponse(
            id = contactAttempt.id.value,
            clientId = contactAttempt.clientId,
            date = contactAttempt.date,
            type = contactAttempt.type.name,
            description = contactAttempt.description,
            result = contactAttempt.result.name,
            createdAt = contactAttempt.audit.createdAt,
            updatedAt = contactAttempt.audit.updatedAt
        )
    }

    /**
     * Mapuje string na typ kontaktu.
     */
    fun mapStringToType(type: String): ContactAttemptType {
        return try {
            ContactAttemptType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid contact attempt type: $type")
        }
    }

    /**
     * Mapuje string na rezultat kontaktu.
     */
    fun mapStringToResult(result: String): ContactAttemptResult {
        return try {
            ContactAttemptResult.valueOf(result.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid contact attempt result: $result")
        }
    }
}