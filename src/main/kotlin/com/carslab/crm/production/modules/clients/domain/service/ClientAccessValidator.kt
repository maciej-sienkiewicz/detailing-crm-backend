package com.carslab.crm.production.modules.clients.domain.service

import com.carslab.crm.production.modules.clients.domain.model.Client
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.repository.ClientRepository
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class ClientAccessValidator(
    private val clientRepository: ClientRepository
) {
    fun getClientForCompany(clientId: ClientId, companyId: Long): Client {
        val client = clientRepository.findById(clientId)
            ?: throw BusinessException("Client not found: ${clientId.value}")

        if (!client.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to client: ${clientId.value}")
        }

        return client
    }
}