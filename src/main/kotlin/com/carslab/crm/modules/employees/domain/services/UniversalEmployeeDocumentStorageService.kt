package com.carslab.crm.modules.employees.infrastructure.storage

import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.ports.EmployeeDocumentStorageService
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class UniversalEmployeeDocumentStorageService(
    private val universalStorageService: UniversalStorageService
) : EmployeeDocumentStorageService {

    private val logger = LoggerFactory.getLogger(UniversalEmployeeDocumentStorageService::class.java)

    override fun storeEmployeeDocument(
        file: MultipartFile,
        employeeId: EmployeeId,
        companyId: Long,
        documentType: String
    ): String {
        try {
            val storageRequest = UniversalStoreRequest(
                file = file,
                originalFileName = file.originalFilename ?: "document",
                contentType = file.contentType ?: "application/octet-stream",
                companyId = companyId,
                entityId = employeeId.value,
                entityType = "employee",
                category = "employees",
                subCategory = "documents",
                description = "Employee document: $documentType",
                tags = mapOf(
                    "employee_id" to employeeId.value,
                    "document_type" to documentType,
                    "module" to "employees"
                )
            )

            val storageId = universalStorageService.storeFile(storageRequest)

            logger.info(
                "Stored employee document: employee={}, type={}, storageId={}",
                employeeId.value, documentType, storageId
            )

            return storageId

        } catch (e: Exception) {
            logger.error("Failed to store employee document for employee: ${employeeId.value}", e)
            throw RuntimeException("Failed to store document", e)
        }
    }

    override fun retrieveEmployeeDocument(storageId: String): ByteArray? {
        return try {
            universalStorageService.retrieveFile(storageId)
        } catch (e: Exception) {
            logger.error("Failed to retrieve employee document: $storageId", e)
            null
        }
    }

    override fun deleteEmployeeDocument(storageId: String): Boolean {
        return try {
            universalStorageService.deleteFile(storageId)
        } catch (e: Exception) {
            logger.error("Failed to delete employee document: $storageId", e)
            false
        }
    }

    override fun documentExists(storageId: String): Boolean {
        return try {
            universalStorageService.exists(storageId)
        } catch (e: Exception) {
            logger.error("Failed to check employee document existence: $storageId", e)
            false
        }
    }
}