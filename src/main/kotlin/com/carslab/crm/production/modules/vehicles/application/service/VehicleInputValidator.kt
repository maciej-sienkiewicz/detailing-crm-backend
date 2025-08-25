package com.carslab.crm.production.modules.vehicles.application.service

import com.carslab.crm.production.modules.vehicles.application.dto.CreateVehicleRequest
import com.carslab.crm.production.modules.vehicles.application.dto.UpdateVehicleRequest
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class VehicleInputValidator {
    fun validateCreateRequest(request: CreateVehicleRequest) {
        if (request.make.isBlank()) {
            throw BusinessException("Make cannot be blank")
        }
        if (request.model.isBlank()) {
            throw BusinessException("Model cannot be blank")
        }
        if (request.licensePlate.isBlank()) {
            throw BusinessException("License plate cannot be blank")
        }
        if (request.ownerIds.isEmpty()) {
            throw BusinessException("Vehicle must have at least one owner")
        }
    }

    fun validateUpdateRequest(request: UpdateVehicleRequest) {
        if (request.make.isBlank()) {
            throw BusinessException("Make cannot be blank")
        }
        if (request.model.isBlank()) {
            throw BusinessException("Model cannot be blank")
        }
        if (request.licensePlate.isBlank()) {
            throw BusinessException("License plate cannot be blank")
        }
    }
}