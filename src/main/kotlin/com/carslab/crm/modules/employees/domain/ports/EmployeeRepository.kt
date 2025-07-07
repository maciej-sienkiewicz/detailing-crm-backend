// src/main/kotlin/com/carslab/crm/modules/employees/domain/ports/EmployeeRepository.kt
package com.carslab.crm.modules.employees.domain.ports

import com.carslab.crm.modules.employees.domain.model.*

interface EmployeeRepository {
    fun save(employee: Employee): Employee
    fun saveNew(createEmployee: CreateEmployee): Employee
    fun findById(id: EmployeeId): Employee?
    fun findByCompanyId(companyId: Long): List<Employee>
    fun findByEmail(email: String, companyId: Long): Employee?
    fun existsByEmail(email: String, companyId: Long): Boolean
    fun deleteById(id: EmployeeId): Boolean
    fun count(companyId: Long): Long
}