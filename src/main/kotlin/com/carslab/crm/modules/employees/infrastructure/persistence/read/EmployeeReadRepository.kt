// src/main/kotlin/com/carslab/crm/modules/employees/infrastructure/persistence/read/EmployeeReadRepository.kt
package com.carslab.crm.modules.employees.infrastructure.persistence.read

import com.carslab.crm.modules.employees.application.queries.models.*
import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.api.model.response.PaginatedResponse

interface EmployeeReadRepository {
    fun findEmployees(
        searchQuery: String? = null,
        position: String? = null,
        role: UserRole? = null,
        isActive: Boolean? = null,
        contractType: ContractType? = null,
        sortBy: String = "fullName",
        sortOrder: String = "asc",
        page: Int = 0,
        size: Int = 20
    ): PaginatedResponse<EmployeeReadModel>

    fun findById(employeeId: String): EmployeeDetailReadModel?

    fun findDocumentsByEmployeeId(employeeId: String): List<EmployeeDocumentReadModel>

    fun getStatistics(): EmployeeStatisticsReadModel

    fun findEmployeesForExport(
        searchQuery: String? = null,
        position: String? = null,
        role: UserRole? = null,
        isActive: Boolean? = null,
        contractType: ContractType? = null
    ): List<EmployeeReadModel>
}