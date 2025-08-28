package com.carslab.crm.production.modules.visits.domain.validator

import com.carslab.crm.production.modules.visits.domain.command.CreateServiceCommand
import com.carslab.crm.production.modules.visits.domain.command.CreateVisitCommand
import com.carslab.crm.production.modules.visits.domain.command.UpdateServiceCommand
import com.carslab.crm.production.modules.visits.domain.command.UpdateVisitCommand
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class VisitCommandValidator {

    fun validateCreateCommand(command: CreateVisitCommand) {
        if (command.title.isBlank()) {
            throw BusinessException("Visit title cannot be blank")
        }

        if (command.startDate.isAfter(command.endDate)) {
            throw BusinessException("Start date cannot be after end date")
        }

        if (command.calendarColorId.isBlank()) {
            throw BusinessException("Calendar color ID cannot be blank")
        }

        if (command.companyId <= 0) {
            throw BusinessException("Company ID must be positive")
        }

        validateServices(command.services)
    }

    fun validateUpdateCommand(command: UpdateVisitCommand, existingVisit: Visit) {
        if (command.title.isBlank()) {
            throw BusinessException("Visit title cannot be blank")
        }

        if (command.startDate.isAfter(command.endDate)) {
            throw BusinessException("Start date cannot be after end date")
        }

        if (existingVisit.status == VisitStatus.COMPLETED) {
            throw BusinessException("Cannot update completed visit")
        }

        validateServices(command.services)
    }

    private fun validateServices(services: List<Any>) {
        services.forEach { service ->
            when (service) {
                is CreateServiceCommand -> {
                    if (service.name.isBlank()) {
                        throw BusinessException("Service name cannot be blank")
                    }
                    if (service.basePrice < BigDecimal.ZERO) {
                        throw BusinessException("Service price cannot be negative")
                    }
                }
                is UpdateServiceCommand -> {
                    if (service.name.isBlank()) {
                        throw BusinessException("Service name cannot be blank")
                    }
                    if (service.basePrice < BigDecimal.ZERO) {
                        throw BusinessException("Service price cannot be negative")
                    }
                }
            }
        }
    }
}