package com.carslab.crm.modules.employees.infrastructure.persistence.entity

import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.model.shared.AuditInfo
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "employee_documents",
    indexes = [
        Index(name = "idx_employee_documents_employee_id", columnList = "employee_id"),
        Index(name = "idx_employee_documents_type", columnList = "type"),
        Index(name = "idx_employee_documents_company_storage", columnList = "company_id,storage_id")
    ]
)
class EmployeeDocumentEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "employee_id", nullable = false)
    val employeeId: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "type", nullable = false)
    var type: String,

    @Column(name = "storage_id", nullable = false)
    val storageId: String,

    @Column(name = "file_url")
    var fileUrl: String? = null,

    @Column(name = "file_size")
    var fileSize: Long? = null,

    @Column(name = "mime_type")
    var mimeType: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "updated_by")
    var updatedBy: String? = null,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
) {

    fun toDomain(): EmployeeDocument = EmployeeDocument(
        id = EmployeeDocumentId.of(id),
        employeeId = EmployeeId.of(employeeId),
        companyId = companyId,
        name = name,
        type = type,
        storageId = storageId,
        fileUrl = fileUrl,
        fileSize = fileSize,
        mimeType = mimeType,
        audit = AuditInfo(
            createdAt = createdAt,
            updatedAt = updatedAt,
            createdBy = createdBy,
            updatedBy = updatedBy,
            version = version
        )
    )

    fun updateFromDomain(document: EmployeeDocument) {
        name = document.name
        type = document.type
        fileUrl = document.fileUrl
        fileSize = document.fileSize
        mimeType = document.mimeType
        updatedAt = document.audit.updatedAt
        updatedBy = document.audit.updatedBy
        version = document.audit.version
    }

    companion object {
        fun fromDomain(document: EmployeeDocument): EmployeeDocumentEntity = EmployeeDocumentEntity(
            id = document.id.value,
            employeeId = document.employeeId.value,
            companyId = document.companyId,
            name = document.name,
            type = document.type,
            storageId = document.storageId,
            fileUrl = document.fileUrl,
            fileSize = document.fileSize,
            mimeType = document.mimeType,
            createdAt = document.audit.createdAt,
            updatedAt = document.audit.updatedAt,
            createdBy = document.audit.createdBy,
            updatedBy = document.audit.updatedBy,
            version = document.audit.version
        )

        fun fromCreateDomain(createDocument: CreateEmployeeDocument, documentId: String, storageId: String): EmployeeDocumentEntity =
            EmployeeDocumentEntity(
                id = documentId,
                employeeId = createDocument.employeeId.value,
                companyId = createDocument.companyId,
                name = createDocument.name,
                type = createDocument.type,
                storageId = storageId,
                fileUrl = createDocument.fileUrl,
                fileSize = createDocument.fileSize,
                mimeType = createDocument.mimeType
            )
    }
}