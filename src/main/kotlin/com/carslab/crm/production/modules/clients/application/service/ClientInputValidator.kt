package com.carslab.crm.production.modules.clients.application.service

import com.carslab.crm.production.modules.clients.application.dto.CreateClientRequest
import com.carslab.crm.production.modules.clients.application.dto.UpdateClientRequest
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class ClientInputValidator {
    fun validateCreateRequest(request: CreateClientRequest) {
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

    fun validateUpdateRequest(request: UpdateClientRequest) {
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