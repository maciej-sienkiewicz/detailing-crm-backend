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
class EmployeeEventHandler(
    private val salaryFixedCostService: SalaryFixedCostService
) {

    private val logger = LoggerFactory.getLogger(EmployeeEventHandler::class.java)

    @EventListener
    @Async("salaryProcessingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleEmployeeCreated(event: EmployeeCreatedEvent) {
        val startTime = System.currentTimeMillis()

        try {
            logger.info("Processing EmployeeCreatedEvent for employee: ${event.employeeId}, company: ${event.companyId}")

            salaryFixedCostService.createSalaryFixedCost(event)

            val processingTime = System.currentTimeMillis() - startTime
            logger.info("Successfully processed EmployeeCreatedEvent for employee: ${event.employeeId} in ${processingTime}ms")

        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            logger.error("Failed to process EmployeeCreatedEvent for employee: ${event.employeeId} after ${processingTime}ms", e)

            logger.warn("Salary fixed cost creation failed for employee ${event.employeeId}, but employee creation succeeded")
        }
    }

    @EventListener
    @Async("salaryProcessingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleEmployeeUpdated(event: EmployeeUpdatedEvent) {
        val startTime = System.currentTimeMillis()

        try {
            logger.info("Processing EmployeeUpdatedEvent for employee: ${event.employeeId}, company: ${event.companyId}")

            salaryFixedCostService.updateSalaryFixedCost(event)

            val processingTime = System.currentTimeMillis() - startTime
            logger.info("Successfully processed EmployeeUpdatedEvent for employee: ${event.employeeId} in ${processingTime}ms")

        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            logger.error("Failed to process EmployeeUpdatedEvent for employee: ${event.employeeId} after ${processingTime}ms", e)

            logger.warn("Salary fixed cost update failed for employee ${event.employeeId}, but employee update succeeded")
        }
    }

    @EventListener
    @Async("salaryProcessingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleEmployeeDeactivated(event: EmployeeDeactivatedEvent) {
        val startTime = System.currentTimeMillis()

        try {
            logger.info("Processing EmployeeDeactivatedEvent for employee: ${event.employeeId}, company: ${event.companyId}")

            salaryFixedCostService.deactivateSalaryFixedCost(event)

            val processingTime = System.currentTimeMillis() - startTime
            logger.info("Successfully processed EmployeeDeactivatedEvent for employee: ${event.employeeId} in ${processingTime}ms")

        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            logger.error("Failed to process EmployeeDeactivatedEvent for employee: ${event.employeeId} after ${processingTime}ms", e)

            logger.warn("Salary fixed cost deactivation failed for employee ${event.employeeId}, but employee deactivation succeeded")
        }
    }
}