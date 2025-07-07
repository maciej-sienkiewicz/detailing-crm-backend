package com.carslab.crm.modules.employees.application.queries.models

import com.carslab.crm.modules.employees.domain.model.EmployeeDocumentType
import java.time.LocalDateTime

data class EmployeeDocumentReadModel(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val name: String,
    val type: EmployeeDocumentType,
    val originalFileName: String,
    val fileSize: Long,
    val mimeType: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val isConfidential: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    val createdBy: String? = null
)

data class EmployeeDocumentStatsReadModel(
    val totalDocuments: Int,
    val documentsPerType: Map<EmployeeDocumentType, Int>,
    val confidentialDocuments: Int,
    val totalSizeBytes: Long,
    val averageSizeBytes: Long
)