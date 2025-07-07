package com.carslab.crm.modules.employees.infrastructure.persistence.read

import com.carslab.crm.modules.employees.application.queries.models.*
import com.carslab.crm.modules.employees.domain.model.EmployeeDocumentType
import com.carslab.crm.api.model.response.PaginatedResponse

interface EmployeeDocumentReadRepository {

    fun findDocumentsByEmployee(
        employeeId: String,
        companyId: Long,
        type: EmployeeDocumentType? = null,
        isConfidential: Boolean? = null,
        searchQuery: String? = null
    ): List<EmployeeDocumentReadModel>

    fun findById(documentId: String, companyId: Long): EmployeeDocumentReadModel?

    fun findCompanyDocuments(
        companyId: Long,
        type: EmployeeDocumentType? = null,
        isConfidential: Boolean? = null,
        searchQuery: String? = null,
        sortBy: String = "createdAt",
        sortOrder: String = "desc",
        page: Int = 0,
        size: Int = 20
    ): PaginatedResponse<EmployeeDocumentReadModel>

    fun getDocumentStats(companyId: Long): EmployeeDocumentStatsReadModel
}