// src/main/kotlin/com/carslab/crm/modules/employees/domain/model/EmployeeDocument.kt
package com.carslab.crm.modules.employees.domain.model

import com.carslab.crm.modules.employees.domain.model.shared.AuditInfo

data class EmployeeDocumentId(val value: String) {
    companion object {
        fun generate(): EmployeeDocumentId = EmployeeDocumentId(java.util.UUID.randomUUID().toString())
        fun of(value: String): EmployeeDocumentId = EmployeeDocumentId(value)
    }
}

data class EmployeeDocument(
    val id: EmployeeDocumentId,
    val employeeId: EmployeeId,
    val companyId: Long,
    val name: String,
    val type: String,
    val fileUrl: String?,
    val fileSize: Long?,
    val mimeType: String?,
    val audit: AuditInfo = AuditInfo()
) {
    fun validateBusinessRules() {
        require(name.isNotBlank()) { "Document name cannot be blank" }
        require(type.isNotBlank()) { "Document type cannot be blank" }

        fileSize?.let { size ->
            require(size >= 0) { "File size cannot be negative" }
            require(size <= 50 * 1024 * 1024) { "File size cannot exceed 50MB" }
        }
    }
}

data class CreateEmployeeDocument(
    val employeeId: EmployeeId,
    val companyId: Long,
    val name: String,
    val type: String,
    val fileUrl: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null
)