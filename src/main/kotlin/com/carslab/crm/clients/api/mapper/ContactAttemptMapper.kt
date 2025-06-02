package com.carslab.crm.clients.api.mapper

import com.carslab.crm.api.model.request.ContactAttemptRequest
import com.carslab.crm.api.model.response.ContactAttemptResponse
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.ContactAttempt
import com.carslab.crm.domain.model.ContactAttemptId
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ContactAttemptMapper {

    fun toDomain(request: ContactAttemptRequest): ContactAttempt {
        return ContactAttempt(
            id = if (request.id != null) ContactAttemptId(request.id!!) else ContactAttemptId(),
            clientId = request.clientId,
            type = request.type,
            description = request.description,
            result = request.result,
            date = if (request.date != null) LocalDateTime.parse(request.date) else LocalDateTime.now(),
            notes = request.notes,
            audit = Audit()
        )
    }

    fun toResponse(contactAttempt: ContactAttempt): ContactAttemptResponse {
        return ContactAttemptResponse(
            id = contactAttempt.id.value,
            clientId = contactAttempt.clientId,
            type = contactAttempt.type,
            description = contactAttempt.description,
            result = contactAttempt.result,
            date = contactAttempt.date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            notes = contactAttempt.notes
        )
    }
}