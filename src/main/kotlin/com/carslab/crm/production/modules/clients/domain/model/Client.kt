package com.carslab.crm.production.modules.clients.domain.model

import java.time.LocalDateTime

@JvmInline
value class ClientId(val value: Long) {
    companion object {
        fun of(value: Long): ClientId = ClientId(value)
    }
}

data class Client(
    val id: ClientId,
    val companyId: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String?,
    val company: String?,
    val taxId: String?,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    val fullName: String get() = "$firstName $lastName"

    fun canBeAccessedBy(companyId: Long): Boolean {
        return this.companyId == companyId
    }

    fun update(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        address: String?,
        company: String?,
        taxId: String?,
        notes: String?
    ): Client {
        return copy(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            address = address,
            company = company,
            taxId = taxId,
            notes = notes,
            updatedAt = LocalDateTime.now()
        )
    }
}