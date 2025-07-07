package com.carslab.crm.modules.employees.infrastructure.persistence.adapter

import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.ports.EmployeeDocumentRepository
import com.carslab.crm.modules.employees.infrastructure.persistence.entity.EmployeeDocumentEntity
import com.carslab.crm.modules.employees.infrastructure.persistence.repository.EmployeeDocumentJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaEmployeeDocumentRepositoryAdapter(
    private val documentJpaRepository: EmployeeDocumentJpaRepository
) : EmployeeDocumentRepository {

    override fun save(document: EmployeeDocument): EmployeeDocument {
        val createDocument = CreateEmployeeDocument(
            employeeId = document.employeeId,
            companyId = document.companyId,
            name = document.name,
            type = document.type,
            description = document.description,
            storageId = document.storageId,
            fileSize = document.fileSize,
            mimeType = document.mimeType
        )

        val entity = EmployeeDocumentEntity.fromCreateDomain(createDocument, document.id.value)
        val savedEntity = documentJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun saveNew(createDocument: CreateEmployeeDocument): EmployeeDocument {
        val documentId = EmployeeDocumentId.generate().value
        val entity = EmployeeDocumentEntity.fromCreateDomain(createDocument, documentId)
        val savedEntity = documentJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: EmployeeDocumentId): EmployeeDocument? {
        return documentJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByEmployeeId(employeeId: EmployeeId): List<EmployeeDocument> {
        return documentJpaRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId.value)
            .map { it.toDomain() }
    }

    override fun deleteById(id: EmployeeDocumentId): Boolean {
        return if (documentJpaRepository.existsById(id.value)) {
            documentJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }
}