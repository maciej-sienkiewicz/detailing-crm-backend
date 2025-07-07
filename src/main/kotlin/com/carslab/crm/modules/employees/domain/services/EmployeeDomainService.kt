// src/main/kotlin/com/carslab/crm/modules/employees/domain/services/EmployeeDomainService.kt
package com.carslab.crm.modules.employees.domain.services

import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.ports.EmployeeRepository
import com.carslab.crm.domain.exception.DomainException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EmployeeDomainService(
    private val employeeRepository: EmployeeRepository
) {

    private val logger = LoggerFactory.getLogger(EmployeeDomainService::class.java)

    fun createEmployee(createEmployee: CreateEmployee): Employee {
        logger.info("Creating employee: ${createEmployee.fullName} for company: ${createEmployee.companyId}")

        // Sprawdź czy email już istnieje w firmie
        if (employeeRepository.existsByEmail(createEmployee.email, createEmployee.companyId)) {
            throw DomainException("Employee with email ${createEmployee.email} already exists in this company")
        }

        val employee = Employee(
            id = EmployeeId.generate(),
            companyId = createEmployee.companyId,
            fullName = createEmployee.fullName,
            birthDate = createEmployee.birthDate,
            hireDate = createEmployee.hireDate,
            position = createEmployee.position,
            email = createEmployee.email,
            phone = createEmployee.phone,
            role = createEmployee.role,
            hourlyRate = createEmployee.hourlyRate,
            bonusFromRevenue = createEmployee.bonusFromRevenue,
            isActive = createEmployee.isActive,
            workingHoursPerWeek = createEmployee.workingHoursPerWeek,
            contractType = createEmployee.contractType,
            emergencyContact = createEmployee.emergencyContact,
            notes = createEmployee.notes
        )

        employee.validateBusinessRules()

        val savedEmployee = employeeRepository.saveNew(createEmployee)
        logger.info("Successfully created employee: ${savedEmployee.id.value}")

        return savedEmployee
    }

    fun updateEmployee(employee: Employee): Employee {
        logger.info("Updating employee: ${employee.id.value}")

        val existingEmployee = employeeRepository.findById(employee.id)
            ?: throw DomainException("Employee not found: ${employee.id.value}")

        // Sprawdź czy email nie koliduje z innym pracownikiem
        if (existingEmployee.email != employee.email) {
            if (employeeRepository.existsByEmail(employee.email, employee.companyId)) {
                throw DomainException("Employee with email ${employee.email} already exists in this company")
            }
        }

        employee.validateBusinessRules()

        val updatedEmployee = employee.copy(
            audit = existingEmployee.audit.updated()
        )

        val savedEmployee = employeeRepository.save(updatedEmployee)
        logger.info("Successfully updated employee: ${savedEmployee.id.value}")

        return savedEmployee
    }

    fun getEmployee(employeeId: EmployeeId): Employee? {
        return employeeRepository.findById(employeeId)
    }

    fun getEmployeesByCompany(companyId: Long): List<Employee> {
        return employeeRepository.findByCompanyId(companyId)
    }

    fun deleteEmployee(employeeId: EmployeeId): Boolean {
        logger.info("Deleting employee: ${employeeId.value}")

        val employee = employeeRepository.findById(employeeId)
            ?: throw DomainException("Employee not found: ${employeeId.value}")

        val deleted = employeeRepository.deleteById(employeeId)

        if (deleted) {
            logger.info("Successfully deleted employee: ${employeeId.value}")
        }

        return deleted
    }
}