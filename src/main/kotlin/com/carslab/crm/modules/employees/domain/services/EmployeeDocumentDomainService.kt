package com.carslab.crm.modules.employees.domain.services

import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.ports.*
import com.carslab.crm.domain.exception.DomainException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional
class EmployeeDocumentDomainService(
    private val employeeRepository: EmployeeRepository,
    private val documentRepository: EmployeeDocumentRepository,
    private val documentStorageService: EmployeeDocumentStorageService
) {

    private val logger = LoggerFactory.getLogger(EmployeeDocumentDomainService::class.java)

    fun createDocument(
        file: MultipartFile,
        employeeId: EmployeeId,
        companyId: Long,
        name: String,
        type: String,
        fileUrl: String? = null,
        fileSize: Long? = null,
        mimeType: String? = null
    ): EmployeeDocument {
        logger.info("Creating document for employee: ${employeeId.value}")

        // Sprawdź czy pracownik istnieje i należy do firmy
        val employee = employeeRepository.findById(employeeId)
            ?: throw DomainException("Employee not found: ${employeeId.value}")

        if (employee.companyId != companyId) {
            throw DomainException("Employee does not belong to current company")
        }

        // Przechowaj plik w uniwersalnym storage
        val storageId = documentStorageService.storeEmployeeDocument(
            file = file,
            employeeId = employeeId,
            companyId = companyId,
            documentType = type
        )

        val document = EmployeeDocument(
            id = EmployeeDocumentId.generate(),
            employeeId = employeeId,
            companyId = companyId,
            name = name,
            type = type,
            storageId = storageId,
            fileUrl = generateFileUrl(storageId), // Generuj URL dla kompatybilności
            fileSize = file.size,
            mimeType = file.contentType
        )

        document.validateBusinessRules()

        val savedDocument = documentRepository.save(document)
        logger.info("Successfully created document: ${savedDocument.id.value}")

        return savedDocument
    }

    fun getDocumentsByEmployee(employeeId: EmployeeId, companyId: Long): List<EmployeeDocument> {
        // Sprawdź czy pracownik należy do firmy
        val employee = employeeRepository.findById(employeeId)
        if (employee?.companyId != companyId) {
            logger.warn("Attempt to access documents for employee from different company: ${employeeId.value}")
            return emptyList()
        }

        return documentRepository.findByEmployeeId(employeeId)
    }

    fun deleteDocument(documentId: EmployeeDocumentId, companyId: Long): Boolean {
        logger.info("Deleting document: ${documentId.value}")

        val document = documentRepository.findById(documentId)
            ?: throw DomainException("Document not found: ${documentId.value}")

        // Sprawdź czy dokument należy do firmy
        if (document.companyId != companyId) {
            throw DomainException("Document does not belong to current company")
        }

        // Usuń plik z storage
        documentStorageService.deleteEmployeeDocument(document.storageId)

        // Usuń rekord z bazy
        val deleted = documentRepository.deleteById(documentId)

        if (deleted) {
            logger.info("Successfully deleted document: ${documentId.value}")
        }

        return deleted
    }

    fun getDocumentContent(documentId: EmployeeDocumentId, companyId: Long): ByteArray? {
        val document = documentRepository.findById(documentId)

        // Sprawdź czy dokument należy do firmy
        if (document?.companyId != companyId) {
            logger.warn("Attempt to access document from different company: ${documentId.value}")
            return null
        }

        return documentStorageService.retrieveEmployeeDocument(document.storageId)
    }

    private fun generateFileUrl(storageId: String): String {
        // Generuj URL który będzie używany przez kontroler do pobierania
        return "/api/files/employee-documents/$storageId"
    }
}