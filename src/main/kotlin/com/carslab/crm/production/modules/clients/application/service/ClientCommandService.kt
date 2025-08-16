package com.carslab.crm.production.modules.clients.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.clients.application.dto.CreateClientRequest
import com.carslab.crm.production.modules.clients.application.dto.UpdateClientRequest
import com.carslab.crm.production.modules.clients.domain.command.CreateClientCommand
import com.carslab.crm.production.modules.clients.domain.command.UpdateClientCommand
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.service.ClientDomainService
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ClientCommandService(
    private val clientDomainService: ClientDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ClientCommandService::class.java)

    fun createClient(request: CreateClientRequest): ClientResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating client '{}' for company: {}", request.firstName, companyId)

        validateCreateRequest(request)

        val command = CreateClientCommand(
            companyId = companyId,
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email ?: "",
            phone = request.phone ?: "",
            address = request.address,
            company = request.company,
            taxId = request.taxId,
            notes = request.notes
        )

        val client = clientDomainService.createClient(command)
        logger.info("Client created successfully: {}", client.id.value)

        return ClientResponse.from(client)
            .also {  }
    }

    fun updateClient(clientId: String, request: UpdateClientRequest): ClientResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating client: {} for company: {}", clientId, companyId)

        validateUpdateRequest(request)

        val command = UpdateClientCommand(
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            phone = request.phone,
            address = request.address,
            company = request.company,
            taxId = request.taxId,
            notes = request.notes
        )

        val client = clientDomainService.updateClient(ClientId.of(clientId.toLong()), command, companyId)
        logger.info("Client updated successfully: {}", clientId)

        return ClientResponse.from(client)
    }

    fun deleteClient(clientId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting client: {} for company: {}", clientId, companyId)

        val deleted = clientDomainService.deleteClient(ClientId.of(clientId.toLong()), companyId)
        if (deleted) {
            logger.info("Client deleted successfully: {}", clientId)
        } else {
            logger.warn("Client not found for deletion: {}", clientId)
        }
    }

    private fun validateCreateRequest(request: CreateClientRequest) {
        if (request.firstName.isBlank()) {
            throw BusinessException("First name cannot be blank")
        }
        if (request.lastName.isBlank()) {
            throw BusinessException("Last name cannot be blank")
        }
        if (request.email.isNullOrBlank() && request.phone.isNullOrBlank()) {
            throw BusinessException("Either email or phone must be provided")
        }
        if (!request.email.isNullOrBlank() && !isValidEmail(request.email)) {
            throw BusinessException("Invalid email format")
        }
    }

    private fun validateUpdateRequest(request: UpdateClientRequest) {
        if (request.firstName.isBlank()) {
            throw BusinessException("First name cannot be blank")
        }
        if (request.lastName.isBlank()) {
            throw BusinessException("Last name cannot be blank")
        }
        if (request.email.isBlank() && request.phone.isBlank()) {
            throw BusinessException("Either email or phone must be provided")
        }
        if (request.email.isNotBlank() && !isValidEmail(request.email)) {
            throw BusinessException("Invalid email format")
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }
}