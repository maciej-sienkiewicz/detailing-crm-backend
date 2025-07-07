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
        val entity = if (documentJpaRepository.existsById(document.id.value)) {
            val existing = documentJpaRepository.getReferenceById(document.id.value)
            existing.updateFromDomain(document)
            existing
        } else {
            EmployeeDocumentEntity.fromDomain(document)
        }

        val savedEntity = documentJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun saveNew(createDocument: CreateEmployeeDocument): EmployeeDocument {
        throw UnsupportedOperationException("Use save(EmployeeDocument) with storageId instead")
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

    @Transactional(readOnly = true)
    fun findByEmployeeIdAndCompanyId(employeeId: EmployeeId, companyId: Long): List<EmployeeDocument> {
        return documentJpaRepository.findByEmployeeIdAndCompanyIdOrderByCreatedAtDesc(employeeId.value, companyId)
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