// src/main/kotlin/com/carslab/crm/modules/employees/domain/services/EmployeeDocumentDomainService.kt
package com.carslab.crm.modules.employees.domain.services

import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.ports.*
import com.carslab.crm.domain.exception.DomainException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EmployeeDocumentDomainService(
    private val employeeRepository: EmployeeRepository,
    private val documentRepository: EmployeeDocumentRepository
) {

    private val logger = LoggerFactory.getLogger(EmployeeDocumentDomainService::class.java)

    fun createDocument(createDocument: CreateEmployeeDocument): EmployeeDocument {
        logger.info("Creating document for employee: ${createDocument.employeeId.value}")

        // Sprawdź czy pracownik istnieje
        employeeRepository.findById(createDocument.employeeId)
            ?: throw DomainException("Employee not found: ${createDocument.employeeId.value}")

        val document = EmployeeDocument(
            id = EmployeeDocumentId.generate(),
            employeeId = createDocument.employeeId,
            companyId = createDocument.companyId,
            name = createDocument.name,
            type = createDocument.type,
            fileUrl = createDocument.fileUrl,
            fileSize = createDocument.fileSize,
            mimeType = createDocument.mimeType
        )

        document.validateBusinessRules()

        val savedDocument = documentRepository.saveNew(createDocument)
        logger.info("Successfully created document: ${savedDocument.id.value}")

        return savedDocument
    }

    fun getDocumentsByEmployee(employeeId: EmployeeId): List<EmployeeDocument> {
        return documentRepository.findByEmployeeId(employeeId)
    }

    fun deleteDocument(documentId: EmployeeDocumentId): Boolean {
        logger.info("Deleting document: ${documentId.value}")

        val document = documentRepository.findById(documentId)
            ?: throw DomainException("Document not found: ${documentId.value}")

        val deleted = documentRepository.deleteById(documentId)

        if (deleted) {
            logger.info("Successfully deleted document: ${documentId.value}")
        }

        return deleted
    }
}