package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.domain.model.ContactAttempt
import com.carslab.crm.domain.model.ContactAttemptAudit
import com.carslab.crm.domain.model.ContactAttemptId
import com.carslab.crm.domain.model.ContactAttemptResult
import com.carslab.crm.domain.model.ContactAttemptType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "contact_attempts")
class ContactAttemptEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @Column(name = "client_id", nullable = false)
    var clientId: String,

    @Column(nullable = false)
    var date: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: ContactAttemptType,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var result: ContactAttemptResult,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): ContactAttempt {
        return ContactAttempt(
            id = ContactAttemptId(id),
            clientId = clientId,
            date = date,
            type = type,
            description = description,
            result = result,
            audit = ContactAttemptAudit(
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        )
    }

    companion object {
        fun fromDomain(domain: ContactAttempt): ContactAttemptEntity {
            return ContactAttemptEntity(
                id = domain.id.value,
                clientId = domain.clientId,
                date = domain.date,
                type = domain.type,
                description = domain.description,
                result = domain.result,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }
    }
}