package com.carslab.crm.modules.employees.application.commands.handlers

import com.carslab.crm.modules.employees.application.commands.models.*
import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.services.*
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateEmployeeCommandHandler(
    private val employeeService: EmployeeService,
    private val securityContext: SecurityContext
) : CommandHandler<CreateEmployeeCommand, String> {

    private val logger = LoggerFactory.getLogger(CreateEmployeeCommandHandler::class.java)

    @Transactional
    override fun handle(command: CreateEmployeeCommand): String {
        logger.info("Processing create employee command for: ${command.fullName}")

        try {
            val companyId = securityContext.getCurrentCompanyId()
            val userId = securityContext.getCurrentUserId()
            val userName = securityContext.getCurrentUserName()

            val createEmployee = CreateEmployee(
                companyId = companyId,
                fullName = command.fullName,
                birthDate = command.birthDate,
                hireDate = command.hireDate,
                position = command.position,
                email = command.email,
                phone = command.phone,
                role = command.role,
                hourlyRate = command.hourlyRate,
                bonusFromRevenue = command.bonusFromRevenue,
                isActive = command.isActive,
                workingHoursPerWeek = command.workingHoursPerWeek,
                contractType = command.contractType,
                emergencyContact = command.emergencyContact,
                notes = command.notes
            )

            val employee = employeeService.createEmployee(createEmployee, userId, userName)

            logger.info("Successfully processed create employee command, employeeId: ${employee.id.value}")
            return employee.id.value
        } catch (e: Exception) {
            logger.error("Failed to create employee: ${command.fullName}", e)
            throw e
        }
    }
}

@Service
class UpdateEmployeeCommandHandler(
    private val employeeService: EmployeeService,
    private val securityContext: SecurityContext
) : CommandHandler<UpdateEmployeeCommand, String> {

    private val logger = LoggerFactory.getLogger(UpdateEmployeeCommandHandler::class.java)

    @Transactional
    override fun handle(command: UpdateEmployeeCommand): String {
        logger.info("Processing update employee command for: ${command.id}")

        try {
            val companyId = securityContext.getCurrentCompanyId()
            val userId = securityContext.getCurrentUserId()
            val userName = securityContext.getCurrentUserName()

            val existingEmployee = employeeService.getEmployee(EmployeeId.of(command.id))
                ?: throw IllegalArgumentException("Employee not found: ${command.id}")

            if (existingEmployee.companyId != companyId) {
                throw IllegalArgumentException("Employee does not belong to current company")
            }

            val updatedEmployee = existingEmployee.copy(
                fullName = command.fullName,
                birthDate = command.birthDate,
                hireDate = command.hireDate,
                position = command.position,
                email = command.email,
                phone = command.phone,
                role = command.role,
                hourlyRate = command.hourlyRate,
                bonusFromRevenue = command.bonusFromRevenue,
                isActive = command.isActive,
                workingHoursPerWeek = command.workingHoursPerWeek,
                contractType = command.contractType,
                emergencyContact = command.emergencyContact,
                notes = command.notes,
                audit = existingEmployee.audit.updated(userId)
            )

            val employee = employeeService.updateEmployee(updatedEmployee, userId, userName)

            logger.info("Successfully processed update employee command, employeeId: ${employee.id.value}")
            return employee.id.value
        } catch (e: Exception) {
            logger.error("Failed to update employee: ${command.id}", e)
            throw e
        }
    }
}

@Service
class DeleteEmployeeCommandHandler(
    private val employeeService: EmployeeService,
    private val securityContext: SecurityContext
) : CommandHandler<DeleteEmployeeCommand, Boolean> {

    private val logger = LoggerFactory.getLogger(DeleteEmployeeCommandHandler::class.java)

    @Transactional
    override fun handle(command: DeleteEmployeeCommand): Boolean {
        logger.info("Processing delete employee command for: ${command.id}")

        try {
            val companyId = securityContext.getCurrentCompanyId()
            val userId = securityContext.getCurrentUserId()
            val userName = securityContext.getCurrentUserName()

            val employee = employeeService.getEmployee(EmployeeId.of(command.id))
                ?: throw IllegalArgumentException("Employee not found: ${command.id}")

            if (employee.companyId != companyId) {
                throw IllegalArgumentException("Employee does not belong to current company")
            }

            val result = if (employee.isActive) {
                employeeService.deactivateEmployee(EmployeeId.of(command.id), userId, userName)
            } else {
                employeeService.deleteEmployee(EmployeeId.of(command.id))
            }

            logger.info("Successfully processed delete employee command, employeeId: ${command.id}")
            return result
        } catch (e: Exception) {
            logger.error("Failed to delete employee: ${command.id}", e)
            throw e
        }
    }
}

@Service
class CreateEmployeeDocumentCommandHandler(
    private val employeeDocumentService: EmployeeDocumentService,
    private val universalStorageService: UniversalStorageService,
    private val securityContext: SecurityContext
) : CommandHandler<CreateEmployeeDocumentCommand, String> {

    private val logger = LoggerFactory.getLogger(CreateEmployeeDocumentCommandHandler::class.java)

    @Transactional
    override fun handle(command: CreateEmployeeDocumentCommand): String {
        logger.info("Processing create employee document command for employee: ${command.employeeId}")

        try {
            val companyId = securityContext.getCurrentCompanyId()

            validateFileType(command.file.originalFilename, command.file.contentType)

            val storageId = universalStorageService.storeFile(
                UniversalStoreRequest(
                    file = command.file,
                    originalFileName = command.file.originalFilename ?: "document",
                    contentType = command.file.contentType ?: "application/octet-stream",
                    companyId = companyId,
                    entityId = command.employeeId,
                    entityType = "employee",
                    category = "employees",
                    subCategory = "documents",
                    description = command.description,
                    tags = mapOf(
                        "document_type" to command.type,
                        "document_name" to command.name,
                        "employee_id" to command.employeeId
                    )
                )
            )

            val createDocument = CreateEmployeeDocument(
                employeeId = EmployeeId.of(command.employeeId),
                companyId = companyId,
                name = command.name,
                type = command.type,
                description = command.description,
                storageId = storageId,
                fileSize = command.file.size,
                mimeType = command.file.contentType
            )

            val document = employeeDocumentService.createDocument(createDocument)

            logger.info("Successfully processed create document command, documentId: ${document.id.value}")
            return document.id.value
        } catch (e: Exception) {
            logger.error("Failed to create employee document for employee: ${command.employeeId}", e)
            throw e
        }
    }

    private fun validateFileType(filename: String?, contentType: String?) {
        val allowedExtensions = listOf("pdf", "doc", "docx", "jpg", "jpeg", "png", "txt")
        val allowedMimeTypes = listOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png",
            "text/plain"
        )

        filename?.let { name ->
            val extension = name.substringAfterLast('.', "").lowercase()
            if (extension.isNotEmpty() && extension !in allowedExtensions) {
                throw IllegalArgumentException("File type not allowed: $extension")
            }
        }

        contentType?.let { type ->
            if (type !in allowedMimeTypes) {
                throw IllegalArgumentException("MIME type not allowed: $type")
            }
        }
    }
}

@Service
class DeleteEmployeeDocumentCommandHandler(
    private val employeeDocumentService: EmployeeDocumentService,
    private val universalStorageService: UniversalStorageService,
    private val securityContext: SecurityContext
) : CommandHandler<DeleteEmployeeDocumentCommand, Boolean> {

    private val logger = LoggerFactory.getLogger(DeleteEmployeeDocumentCommandHandler::class.java)

    @Transactional
    override fun handle(command: DeleteEmployeeDocumentCommand): Boolean {
        logger.info("Processing delete employee document command for: ${command.id}")

        try {
            val companyId = securityContext.getCurrentCompanyId()
            val documentId = EmployeeDocumentId.of(command.id)

            val document = employeeDocumentService.getDocument(documentId)
                ?: throw IllegalArgumentException("Document not found: ${command.id}")

            if (document.companyId != companyId) {
                throw IllegalArgumentException("Document does not belong to current company")
            }

            universalStorageService.deleteFile(document.storageId)

            val deleted = employeeDocumentService.deleteDocument(documentId)

            logger.info("Successfully processed delete document command, documentId: ${command.id}, deleted: $deleted")
            return deleted
        } catch (e: Exception) {
            logger.error("Failed to delete employee document: ${command.id}", e)
            throw e
        }
    }
}

@Service
class DownloadEmployeeDocumentCommandHandler(
    private val employeeDocumentService: EmployeeDocumentService,
    private val universalStorageService: UniversalStorageService,
    private val securityContext: SecurityContext
) : CommandHandler<DownloadEmployeeDocumentCommand, ByteArray> {

    private val logger = LoggerFactory.getLogger(DownloadEmployeeDocumentCommandHandler::class.java)

    override fun handle(command: DownloadEmployeeDocumentCommand): ByteArray {
        logger.debug("Processing download employee document command for: ${command.documentId}")

        try {
            val companyId = securityContext.getCurrentCompanyId()
            val documentId = EmployeeDocumentId.of(command.documentId)

            val document = employeeDocumentService.getDocument(documentId)
                ?: throw IllegalArgumentException("Document not found: ${command.documentId}")

            if (document.companyId != companyId) {
                throw IllegalArgumentException("Document does not belong to current company")
            }

            val fileData = universalStorageService.retrieveFile(document.storageId)
                ?: throw IllegalArgumentException("File not found in storage: ${document.storageId}")

            logger.debug("Successfully retrieved document: ${command.documentId}")
            return fileData
        } catch (e: Exception) {
            logger.error("Failed to download employee document: ${command.documentId}", e)
            throw e
        }
    }
}

@Service
class GetEmployeeDocumentUrlCommandHandler(
    private val employeeDocumentService: EmployeeDocumentService,
    private val universalStorageService: UniversalStorageService,
    private val securityContext: SecurityContext
) : CommandHandler<GetEmployeeDocumentUrlCommand, String> {

    private val logger = LoggerFactory.getLogger(GetEmployeeDocumentUrlCommandHandler::class.java)

    override fun handle(command: GetEmployeeDocumentUrlCommand): String {
        logger.debug("Processing get document URL command for: ${command.documentId}")

        try {
            val companyId = securityContext.getCurrentCompanyId()
            val documentId = EmployeeDocumentId.of(command.documentId)

            val document = employeeDocumentService.getDocument(documentId)
                ?: throw IllegalArgumentException("Document not found: ${command.documentId}")

            if (document.companyId != companyId) {
                throw IllegalArgumentException("Document does not belong to current company")
            }

            val downloadUrl = universalStorageService.generateDownloadUrl(
                document.storageId,
                command.expirationMinutes
            ) ?: throw IllegalArgumentException("Cannot generate download URL for: ${document.storageId}")

            logger.debug("Successfully generated download URL for document: ${command.documentId}")
            return downloadUrl
        } catch (e: Exception) {
            logger.error("Failed to generate download URL for document: ${command.documentId}", e)
            throw e
        }
    }
}