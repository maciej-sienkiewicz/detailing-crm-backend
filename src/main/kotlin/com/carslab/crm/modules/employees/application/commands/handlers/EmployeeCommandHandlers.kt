// src/main/kotlin/com/carslab/crm/modules/employees/application/commands/handlers/EmployeeCommandHandlers.kt
package com.carslab.crm.modules.employees.application.commands.handlers

import com.carslab.crm.modules.employees.application.commands.models.*
import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.services.*
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import com.carslab.crm.infrastructure.security.SecurityContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateEmployeeCommandHandler(
    private val employeeDomainService: EmployeeDomainService,
    private val securityContext: SecurityContext
) : CommandHandler<CreateEmployeeCommand, String> {

    private val logger = LoggerFactory.getLogger(CreateEmployeeCommandHandler::class.java)

    @Transactional
    override fun handle(command: CreateEmployeeCommand): String {
        logger.info("Processing create employee command for: ${command.fullName}")

        try {
            val companyId = securityContext.getCurrentCompanyId()

            val createEmployee = CreateEmployee(
                companyId = companyId,
                fullName = command.fullName,
                birthDate = command.birthDate,
                hireDate = command.hireDate,
                position = command.position,
                email = command.email,
                phone = command.phone,
                role = command.role,
                hourlyRate = command.hourlyRate,
                bonusFromRevenue = command.bonusFromRevenue,
                isActive = command.isActive,
                workingHoursPerWeek = command.workingHoursPerWeek,
                contractType = command.contractType,
                emergencyContact = command.emergencyContact,
                notes = command.notes
            )

            val employee = employeeDomainService.createEmployee(createEmployee)

            logger.info("Successfully processed create employee command, employeeId: ${employee.id.value}")
            return employee.id.value
        } catch (e: Exception) {
            logger.error("Failed to create employee: ${command.fullName}", e)
            throw e
        }
    }
}

@Service
class UpdateEmployeeCommandHandler(
    private val employeeDomainService: EmployeeDomainService,
    private val securityContext: SecurityContext
) : CommandHandler<UpdateEmployeeCommand, String> {

    private val logger = LoggerFactory.getLogger(UpdateEmployeeCommandHandler::class.java)

    @Transactional
    override fun handle(command: UpdateEmployeeCommand): String {
        logger.info("Processing update employee command for: ${command.id}")

        try {
            val companyId = securityContext.getCurrentCompanyId()

            val existingEmployee = employeeDomainService.getEmployee(EmployeeId.of(command.id))
                ?: throw IllegalArgumentException("Employee not found: ${command.id}")

            // Sprawdź czy pracownik należy do tej firmy
            if (existingEmployee.companyId != companyId) {
                throw IllegalArgumentException("Employee does not belong to current company")
            }

            val updatedEmployee = existingEmployee.copy(
                fullName = command.fullName,
                birthDate = command.birthDate,
                hireDate = command.hireDate,
                position = command.position,
                email = command.email,
                phone = command.phone,
                role = command.role,
                hourlyRate = command.hourlyRate,
                bonusFromRevenue = command.bonusFromRevenue,
                isActive = command.isActive,
                workingHoursPerWeek = command.workingHoursPerWeek,
                contractType = command.contractType,
                emergencyContact = command.emergencyContact,
                notes = command.notes
            )

            val employee = employeeDomainService.updateEmployee(updatedEmployee)

            logger.info("Successfully processed update employee command, employeeId: ${employee.id.value}")
            return employee.id.value
        } catch (e: Exception) {
            logger.error("Failed to update employee: ${command.id}", e)
            throw e
        }
    }
}

@Service
class DeleteEmployeeCommandHandler(
    private val employeeDomainService: EmployeeDomainService,
    private val securityContext: SecurityContext
) : CommandHandler<DeleteEmployeeCommand, Boolean> {

    private val logger = LoggerFactory.getLogger(DeleteEmployeeCommandHandler::class.java)

    @Transactional
    override fun handle(command: DeleteEmployeeCommand): Boolean {
        logger.info("Processing delete employee command for: ${command.id}")

        try {
            val companyId = securityContext.getCurrentCompanyId()
            val employeeId = EmployeeId.of(command.id)

            val existingEmployee = employeeDomainService.getEmployee(employeeId)
                ?: throw IllegalArgumentException("Employee not found: ${command.id}")

            // Sprawdź czy pracownik należy do tej firmy
            if (existingEmployee.companyId != companyId) {
                throw IllegalArgumentException("Employee does not belong to current company")
            }

            val deleted = employeeDomainService.deleteEmployee(employeeId)

            logger.info("Successfully processed delete employee command, employeeId: ${command.id}, deleted: $deleted")
            return deleted
        } catch (e: Exception) {
            logger.error("Failed to delete employee: ${command.id}", e)
            throw e
        }
    }
}



@Service
class DeleteEmployeeDocumentCommandHandler(
    private val documentDomainService: EmployeeDocumentDomainService,
    private val securityContext: SecurityContext
) : CommandHandler<DeleteEmployeeDocumentCommand, Boolean> {

    private val logger = LoggerFactory.getLogger(DeleteEmployeeDocumentCommandHandler::class.java)

    @Transactional
    override fun handle(command: DeleteEmployeeDocumentCommand): Boolean {
        logger.info("Processing delete employee document command for: ${command.id}")

        try {
            val companyId = securityContext.getCurrentCompanyId()
            val documentId = EmployeeDocumentId.of(command.id)

            val deleted = documentDomainService.deleteDocument(documentId, companyId)

            logger.info("Successfully processed delete document command, documentId: ${command.id}, deleted: $deleted")
            return deleted
        } catch (e: Exception) {
            logger.error("Failed to delete employee document: ${command.id}", e)
            throw e
        }
    }
}