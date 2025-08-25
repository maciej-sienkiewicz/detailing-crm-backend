package com.carslab.crm.production.modules.clients.domain.service

import com.carslab.crm.production.modules.clients.domain.command.CreateClientCommand
import com.carslab.crm.production.modules.clients.domain.model.Client
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ClientFactory {
    fun create(command: CreateClientCommand): Client {
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