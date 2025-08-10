package com.carslab.crm.modules.employees.infrastructure.events

import com.carslab.crm.modules.employees.domain.events.EmployeeCreatedEvent
import com.carslab.crm.modules.employees.domain.events.EmployeeDeactivatedEvent
import com.carslab.crm.modules.employees.domain.events.EmployeeUpdatedEvent
import com.carslab.crm.modules.employees.domain.services.SalaryFixedCostService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class SecureEmployeeEventHandler(
    private val salaryFixedCostService: SalaryFixedCostService
) {

    private val logger = LoggerFactory.getLogger(SecureEmployeeEventHandler::class.java)

    @EventListener
    @Async("salaryProcessingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleEmployeeCreated(event: EmployeeCreatedEvent) {
        val startTime = System.currentTimeMillis()

        try {
            logger.info("Processing EmployeeCreatedEvent for employee: ${event.employeeId}, company: ${event.companyId}")
            validateEventData(event)

            salaryFixedCostService.createSalaryFixedCost(event)

            val processingTime = System.currentTimeMillis() - startTime
            logger.info("Successfully processed EmployeeCreatedEvent for employee: ${event.employeeId}, company: ${event.companyId} in ${processingTime}ms")

        } catch (e: IllegalArgumentException) {
            val processingTime = System.currentTimeMillis() - startTime
            logger.warn("Invalid data in EmployeeCreatedEvent for employee: ${event.employeeId}, company: ${event.companyId} after ${processingTime}ms: ${e.message}")

        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            logger.error("Failed to process EmployeeCreatedEvent for employee: ${event.employeeId}, company: ${event.companyId} after ${processingTime}ms", e)

            logger.warn("Salary fixed cost creation failed for employee ${event.employeeId} in company ${event.companyId}, but employee creation succeeded")
        }
    }

    @EventListener
    @Async("salaryProcessingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleEmployeeUpdated(event: EmployeeUpdatedEvent) {
        val startTime = System.currentTimeMillis()

        try {
            logger.info("Processing EmployeeUpdatedEvent for employee: ${event.employeeId}, company: ${event.companyId}")
            validateEventData(event)

            salaryFixedCostService.updateSalaryFixedCost(event)

            val processingTime = System.currentTimeMillis() - startTime
            logger.info("Successfully processed EmployeeUpdatedEvent for employee: ${event.employeeId}, company: ${event.companyId} in ${processingTime}ms")

        } catch (e: IllegalArgumentException) {
            val processingTime = System.currentTimeMillis() - startTime
            logger.warn("Invalid data in EmployeeUpdatedEvent for employee: ${event.employeeId}, company: ${event.companyId} after ${processingTime}ms: ${e.message}")

        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            logger.error("Failed to process EmployeeUpdatedEvent for employee: ${event.employeeId}, company: ${event.companyId} after ${processingTime}ms", e)

            logger.warn("Salary fixed cost update failed for employee ${event.employeeId} in company ${event.companyId}, but employee update succeeded")
        }
    }

    @EventListener
    @Async("salaryProcessingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleEmployeeDeactivated(event: EmployeeDeactivatedEvent) {
        val startTime = System.currentTimeMillis()

        try {
            logger.info("Processing EmployeeDeactivatedEvent for employee: ${event.employeeId}, company: ${event.companyId}")
            validateEventData(event)

            salaryFixedCostService.deactivateSalaryFixedCost(event)

            val processingTime = System.currentTimeMillis() - startTime
            logger.info("Successfully processed EmployeeDeactivatedEvent for employee: ${event.employeeId}, company: ${event.companyId} in ${processingTime}ms")

        } catch (e: IllegalArgumentException) {
            val processingTime = System.currentTimeMillis() - startTime
            logger.warn("Invalid data in EmployeeDeactivatedEvent for employee: ${event.employeeId}, company: ${event.companyId} after ${processingTime}ms: ${e.message}")

        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            logger.error("Failed to process EmployeeDeactivatedEvent for employee: ${event.employeeId}, company: ${event.companyId} after ${processingTime}ms", e)

            logger.warn("Salary fixed cost deactivation failed for employee ${event.employeeId} in company ${event.companyId}, but employee deactivation succeeded")
        }
    }

    private fun validateEventData(event: EmployeeCreatedEvent) {
        require(event.employeeId.isNotBlank()) { "Employee ID cannot be blank" }
        require(event.companyId > 0) { "Company ID must be positive" }
        require(event.fullName.isNotBlank()) { "Employee name cannot be blank" }

        event.hourlyRate?.let { rate ->
            require(rate > 0) { "Hourly rate must be positive" }
        }

        event.workingHoursPerWeek?.let { hours ->
            require(hours > 0 && hours <= 168) { "Working hours per week must be between 0 and 168" }
        }
    }

    private fun validateEventData(event: EmployeeUpdatedEvent) {
        require(event.employeeId.isNotBlank()) { "Employee ID cannot be blank" }
        require(event.companyId > 0) { "Company ID must be positive" }
        require(event.fullName.isNotBlank()) { "Employee name cannot be blank" }

        event.newHourlyRate?.let { rate ->
            require(rate > 0) { "New hourly rate must be positive" }
        }

        event.newWorkingHours?.let { hours ->
            require(hours > 0 && hours <= 168) { "New working hours per week must be between 0 and 168" }
        }
    }

    private fun validateEventData(event: EmployeeDeactivatedEvent) {
        require(event.employeeId.isNotBlank()) { "Employee ID cannot be blank" }
        require(event.companyId > 0) { "Company ID must be positive" }
        require(event.fullName.isNotBlank()) { "Employee name cannot be blank" }
    }
}