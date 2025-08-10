package com.carslab.crm.modules.employees.domain.services

import com.carslab.crm.infrastructure.events.EventPublisher
import com.carslab.crm.modules.employees.domain.events.EmployeeCreatedEvent
import com.carslab.crm.modules.employees.domain.events.EmployeeDeactivatedEvent
import com.carslab.crm.modules.employees.domain.events.EmployeeUpdatedEvent
import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.ports.EmployeeRepository
import com.carslab.crm.domain.exception.DomainException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val eventPublisher: EventPublisher
) {

    private val logger = LoggerFactory.getLogger(EmployeeService::class.java)

    fun createEmployee(createEmployee: CreateEmployee, userId: String? = null, userName: String? = null): Employee {
        logger.info("Creating employee: ${createEmployee.fullName} for company: ${createEmployee.companyId}")

        validateCreateEmployeeRequest(createEmployee)

        val employee = employeeRepository.saveNew(createEmployee)
        logger.info("Successfully created employee: ${employee.id.value}")

        val event = EmployeeCreatedEvent(
            employeeId = employee.id.value,
            fullName = employee.fullName,
            position = employee.position,
            email = employee.email,
            role = employee.role,
            hireDate = employee.hireDate,
            hourlyRate = employee.hourlyRate,
            bonusFromRevenue = employee.bonusFromRevenue,
            workingHoursPerWeek = employee.workingHoursPerWeek,
            contractType = employee.contractType,
            companyId = employee.companyId,
            userId = userId,
            userName = userName
        )

        eventPublisher.publish(event)
        logger.debug("Published EmployeeCreatedEvent for employee: ${employee.id.value}")

        return employee
    }

    fun updateEmployee(employee: Employee, userId: String? = null, userName: String? = null): Employee {
        logger.info("Updating employee: ${employee.id.value}")

        val existingEmployee = employeeRepository.findById(employee.id)
            ?: throw DomainException("Employee not found: ${employee.id.value}")

        employee.validateBusinessRules()

        val updatedEmployee = employeeRepository.save(employee)
        logger.info("Successfully updated employee: ${updatedEmployee.id.value}")

        if (hasSalaryChanged(existingEmployee, updatedEmployee)) {
            val event = EmployeeUpdatedEvent(
                employeeId = updatedEmployee.id.value,
                fullName = updatedEmployee.fullName,
                position = updatedEmployee.position,
                previousHourlyRate = existingEmployee.hourlyRate,
                newHourlyRate = updatedEmployee.hourlyRate,
                previousWorkingHours = existingEmployee.workingHoursPerWeek,
                newWorkingHours = updatedEmployee.workingHoursPerWeek,
                contractType = updatedEmployee.contractType,
                companyId = updatedEmployee.companyId,
                userId = userId,
                userName = userName
            )

            eventPublisher.publish(event)
            logger.debug("Published EmployeeUpdatedEvent for employee: ${updatedEmployee.id.value}")
        }

        return updatedEmployee
    }

    fun deactivateEmployee(employeeId: EmployeeId, userId: String? = null, userName: String? = null): Boolean {
        logger.info("Deactivating employee: ${employeeId.value}")

        val employee = employeeRepository.findById(employeeId)
            ?: throw DomainException("Employee not found: ${employeeId.value}")

        if (!employee.isActive) {
            logger.warn("Employee ${employeeId.value} is already inactive")
            return false
        }

        val deactivatedEmployee = employee.copy(isActive = false)
        employeeRepository.save(deactivatedEmployee)

        val event = EmployeeDeactivatedEvent(
            employeeId = employee.id.value,
            fullName = employee.fullName,
            position = employee.position,
            hourlyRate = employee.hourlyRate,
            workingHoursPerWeek = employee.workingHoursPerWeek,
            companyId = employee.companyId,
            userId = userId,
            userName = userName
        )

        eventPublisher.publish(event)
        logger.info("Successfully deactivated employee: ${employeeId.value}")

        return true
    }

    fun getEmployee(employeeId: EmployeeId): Employee? {
        return employeeRepository.findById(employeeId)
    }

    fun getEmployeesByCompany(companyId: Long): List<Employee> {
        return employeeRepository.findByCompanyId(companyId)
    }

    fun deleteEmployee(employeeId: EmployeeId): Boolean {
        logger.info("Deleting employee: ${employeeId.value}")
        return employeeRepository.deleteById(employeeId)
    }

    private fun validateCreateEmployeeRequest(createEmployee: CreateEmployee) {
        if (employeeRepository.existsByEmail(createEmployee.email, createEmployee.companyId)) {
            throw DomainException("Employee with email ${createEmployee.email} already exists in company ${createEmployee.companyId}")
        }

        createEmployee.hourlyRate?.let { rate ->
            if (rate < 0) {
                throw DomainException("Hourly rate cannot be negative")
            }
        }

        createEmployee.bonusFromRevenue?.let { bonus ->
            if (bonus < 0 || bonus > 100) {
                throw DomainException("Bonus from revenue must be between 0 and 100 percent")
            }
        }

        createEmployee.workingHoursPerWeek?.let { hours ->
            if (hours <= 0 || hours > 168) {
                throw DomainException("Working hours per week must be between 0 and 168")
            }
        }
    }

    private fun hasSalaryChanged(existing: Employee, updated: Employee): Boolean {
        return existing.hourlyRate != updated.hourlyRate ||
                existing.workingHoursPerWeek != updated.workingHoursPerWeek
    }
}