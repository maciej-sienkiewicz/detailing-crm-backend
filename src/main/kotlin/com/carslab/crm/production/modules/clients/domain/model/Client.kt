package com.carslab.crm.production.modules.clients.domain.model

import com.carslab.crm.production.modules.clients.domain.command.CreateClientCommand
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
    
    companion object {
        fun from(command: CreateClientCommand): Client {
            return Client(
                id = ClientId(0),
                companyId = command.companyId,
                firstName = command.firstName.trim(),
                lastName = command.lastName.trim(),
                email = command.email.trim(),
                phone = command.phone.trim(),
                address = command.address?.trim(),
                company = command.company?.trim(),
                taxId = command.taxId?.trim(),
                notes = command.notes?.trim(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }
    }
    
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