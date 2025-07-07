package com.carslab.crm.modules.employees.application.queries.models

import com.carslab.crm.infrastructure.cqrs.Query
import com.carslab.crm.modules.employees.domain.model.EmployeeDocumentType
import com.carslab.crm.api.model.response.PaginatedResponse

data class GetEmployeeDocumentsQuery(
    val employeeId: String,
    val type: EmployeeDocumentType? = null,
    val isConfidential: Boolean? = null,
    val searchQuery: String? = null
) : Query<List<EmployeeDocumentReadModel>>

data class GetEmployeeDocumentByIdQuery(
    val documentId: String
) : Query<EmployeeDocumentReadModel?>

data class GetCompanyDocumentsQuery(
    val type: EmployeeDocumentType? = null,
    val isConfidential: Boolean? = null,
    val searchQuery: String? = null,
    val sortBy: String = "createdAt",
    val sortOrder: String = "desc",
    val page: Int = 0,
    val size: Int = 20
) : Query<PaginatedResponse<EmployeeDocumentReadModel>>

data class DownloadEmployeeDocumentQuery(
    val documentId: String
) : Query<DocumentDownloadData?>

data class DocumentDownloadData(
    val fileName: String,
    val contentType: String,
    val data: ByteArray,
    val size: Long
)