package com.carslab.crm.modules.employees.application.commands.handlers

import com.carslab.crm.modules.employees.application.commands.models.*
import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.services.*
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import com.carslab.crm.infrastructure.security.SecurityContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UploadEmployeeDocumentCommandHandler(
    private val documentDomainService: EmployeeDocumentDomainService,
    private val securityContext: SecurityContext
) : CommandHandler<UploadEmployeeDocumentCommand, String> {

    private val logger = LoggerFactory.getLogger(UploadEmployeeDocumentCommandHandler::class.java)

    @Transactional
    override fun handle(command: UploadEmployeeDocumentCommand): String {
        logger.info("Processing upload document command for employee: ${command.employeeId}")

        try {
            val companyId = securityContext.getCurrentCompanyId()
            val employeeId = EmployeeId.of(command.employeeId)

            val document = documentDomainService.uploadDocument(
                file = command.file,
                employeeId = employeeId,
                companyId = companyId,
                name = command.name,
                type = command.type,
                description = command.description,
                tags = command.tags,
                isConfidential = command.isConfidential
            )

            logger.info("Successfully uploaded document: ${document.id.value}")
            return document.id.value
        } catch (e: Exception) {
            logger.error("Failed to upload document for employee: ${command.employeeId}", e)
            throw e
        }
    }
}

@Service
class UpdateEmployeeDocumentCommandHandler(
    private val documentDomainService: EmployeeDocumentDomainService,
    private val securityContext: SecurityContext
) : CommandHandler<UpdateEmployeeDocumentCommand, String> {

    private val logger = LoggerFactory.getLogger(UpdateEmployeeDocumentCommandHandler::class.java)

    @Transactional
    override fun handle(command: UpdateEmployeeDocumentCommand): String {
        logger.info("Processing update document command: ${command.documentId}")

        try {
            val companyId = securityContext.getCurrentCompanyId()
            val documentId = EmployeeDocumentId.of(command.documentId)

            val document = documentDomainService.updateDocumentMetadata(
                documentId = documentId,
                companyId = companyId,
                name = command.name,
                description = command.description,
                tags = command.tags,
                isConfidential = command.isConfidential
            )

            logger.info("Successfully updated document: ${document.id.value}")
            return document.id.value
        } catch (e: Exception) {
            logger.error("Failed to update document: ${command.documentId}", e)
            throw e
        }
    }
}

@Service
class DeleteEmployeeDocumentCommandHandler(
    private val documentDomainService: EmployeeDocumentDomainService,
    private val securityContext: SecurityContext
) : CommandHandler<DeleteEmployeeDocumentCommand, Boolean> {

    private val logger = LoggerFactory.getLogger(DeleteEmployeeDocumentCommandHandler::class.java)

    @Transactional
    override fun handle(command: DeleteEmployeeDocumentCommand): Boolean {
        logger.info("Processing delete document command: ${command.id}")

        try {
            val companyId = securityContext.getCurrentCompanyId()
            val documentId = EmployeeDocumentId.of(command.id)

            val deleted = documentDomainService.deleteDocument(documentId, companyId)

            logger.info("Successfully deleted document: ${command.id}, result: $deleted")
            return deleted
        } catch (e: Exception) {
            logger.error("Failed to delete document: ${command.id}", e)
            throw e
        }
    }
}