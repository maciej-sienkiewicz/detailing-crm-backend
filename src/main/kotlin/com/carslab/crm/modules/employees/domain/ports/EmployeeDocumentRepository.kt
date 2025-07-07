// src/main/kotlin/com/carslab/crm/modules/employees/domain/ports/EmployeeDocumentRepository.kt
package com.carslab.crm.modules.employees.domain.ports

import com.carslab.crm.modules.employees.domain.model.*

interface EmployeeDocumentRepository {
    fun save(document: EmployeeDocument): EmployeeDocument
    fun saveNew(createDocument: CreateEmployeeDocument): EmployeeDocument
    fun findById(id: EmployeeDocumentId): EmployeeDocument?
    fun findByEmployeeId(employeeId: EmployeeId): List<EmployeeDocument>
    fun deleteById(id: EmployeeDocumentId): Boolean
}