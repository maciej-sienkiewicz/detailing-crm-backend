// src/main/kotlin/com/carslab/crm/modules/employees/infrastructure/persistence/adapter/JpaEmployeeRepositoryAdapter.kt
package com.carslab.crm.modules.employees.infrastructure.persistence.adapter

import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.ports.EmployeeRepository
import com.carslab.crm.modules.employees.infrastructure.persistence.entity.EmployeeEntity
import com.carslab.crm.modules.employees.infrastructure.persistence.repository.EmployeeJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaEmployeeRepositoryAdapter(
    private val employeeJpaRepository: EmployeeJpaRepository
) : EmployeeRepository {

    override fun save(employee: Employee): Employee {
        val existingEntity = employeeJpaRepository.findById(employee.id.value)
            .orElseThrow { IllegalArgumentException("Employee not found: ${employee.id.value}") }

        existingEntity.updateFromDomain(employee)
        val savedEntity = employeeJpaRepository.save(existingEntity)
        return savedEntity.toDomain()
    }

    override fun saveNew(createEmployee: CreateEmployee): Employee {
        val employeeId = EmployeeId.generate().value
        val entity = EmployeeEntity.fromCreateDomain(createEmployee, employeeId)
        val savedEntity = employeeJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: EmployeeId): Employee? {
        return employeeJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByCompanyId(companyId: Long): List<Employee> {
        return employeeJpaRepository.findByCompanyId(companyId)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByEmail(email: String, companyId: Long): Employee? {
        return employeeJpaRepository.findByEmailAndCompanyId(email, companyId)?.toDomain()
    }

    @Transactional(readOnly = true)
    override fun existsByEmail(email: String, companyId: Long): Boolean {
        return employeeJpaRepository.existsByEmailAndCompanyId(email, companyId)
    }

    override fun deleteById(id: EmployeeId): Boolean {
        return if (employeeJpaRepository.existsById(id.value)) {
            employeeJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }

    @Transactional(readOnly = true)
    override fun count(companyId: Long): Long {
        return employeeJpaRepository.countByCompanyId(companyId)
    }
}