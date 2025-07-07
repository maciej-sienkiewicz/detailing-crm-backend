// src/main/kotlin/com/carslab/crm/modules/employees/application/queries/models/EmployeeQueries.kt
package com.carslab.crm.modules.employees.application.queries.models

import com.carslab.crm.infrastructure.cqrs.Query
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.employees.domain.model.*

data class GetEmployeesQuery(
    val searchQuery: String? = null,
    val position: String? = null,
    val role: UserRole? = null,
    val isActive: Boolean? = null,
    val contractType: ContractType? = null,
    val sortBy: String = "fullName",
    val sortOrder: String = "asc",
    val page: Int = 0,
    val size: Int = 20
) : Query<PaginatedResponse<EmployeeReadModel>>

data class GetEmployeeByIdQuery(
    val employeeId: String
) : Query<EmployeeDetailReadModel?>

data class GetEmployeeDocumentsQuery(
    val employeeId: String
) : Query<List<EmployeeDocumentReadModel>>

data class GetEmployeeStatisticsQuery(
    val companyId: Long? = null
) : Query<EmployeeStatisticsReadModel>

data class ExportEmployeesQuery(
    val format: String,
    val searchQuery: String? = null,
    val position: String? = null,
    val role: UserRole? = null,
    val isActive: Boolean? = null,
    val contractType: ContractType? = null
) : Query<ByteArray>