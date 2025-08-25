package com.carslab.crm.production.modules.clients.domain.service

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.repository.ClientRepository
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class ClientUniquenessValidator(
    private val clientRepository: ClientRepository
) {
    fun validateForCreation(email: String, phone: String, companyId: Long) {
        if (clientRepository.existsByEmail(email, companyId)) {
            throw BusinessException("Client with email $email already exists")
        }

        if (clientRepository.existsByPhone(phone, companyId)) {
            throw BusinessException("Client with phone $phone already exists")
        }
    }

    fun validateForUpdate(
        email: String,
        phone: String,
        companyId: Long,
        excludeClientId: ClientId
    ) {
        val existingByEmail = clientRepository.findByEmail(email, companyId)
        if (existingByEmail != null && existingByEmail.id != excludeClientId) {
            throw BusinessException("Client with email $email already exists")
        }

        val existingByPhone = clientRepository.findByPhone(phone, companyId)
        if (existingByPhone != null && existingByPhone.id != excludeClientId) {
            throw BusinessException("Client with phone $phone already exists")
        }
    }
}