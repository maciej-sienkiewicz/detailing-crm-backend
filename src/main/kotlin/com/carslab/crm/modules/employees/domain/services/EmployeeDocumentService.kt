package com.carslab.crm.modules.employees.domain.services

import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.ports.*
import com.carslab.crm.domain.exception.DomainException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EmployeeDocumentService(
    private val employeeRepository: EmployeeRepository,
    private val documentRepository: EmployeeDocumentRepository
) {

    private val logger = LoggerFactory.getLogger(EmployeeDocumentService::class.java)

    fun createDocument(createDocument: CreateEmployeeDocument): EmployeeDocument {
        logger.info("Creating document for employee: ${createDocument.employeeId.value}")

        val employee = employeeRepository.findById(createDocument.employeeId)
            ?: throw DomainException("Employee not found: ${createDocument.employeeId.value}")

        if (employee.companyId != createDocument.companyId) {
            throw DomainException("Employee does not belong to the specified company")
        }

        val savedDocument = documentRepository.saveNew(createDocument)
        logger.info("Successfully created document: ${savedDocument.id.value}")

        return savedDocument
    }

    fun getDocument(documentId: EmployeeDocumentId): EmployeeDocument? {
        return documentRepository.findById(documentId)
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