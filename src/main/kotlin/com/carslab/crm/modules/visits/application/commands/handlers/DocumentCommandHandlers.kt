package com.carslab.crm.modules.visits.application.commands.handlers

import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.infrastructure.storage.ProtocolDocumentStorageService
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import com.carslab.crm.infrastructure.events.EventPublisher
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.modules.visits.domain.events.ProtocolDocumentUploadedEvent
import com.carslab.crm.modules.visits.domain.events.ProtocolDocumentDeletedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UploadProtocolDocumentCommandHandler(
    private val documentStorageService: ProtocolDocumentStorageService,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<UploadProtocolDocumentCommand, String> {

    private val logger = LoggerFactory.getLogger(UploadProtocolDocumentCommandHandler::class.java)

    @Transactional
    override fun handle(command: UploadProtocolDocumentCommand): String {
        logger.info("Uploading document for protocol: ${command.protocolId}, type: ${command.documentType}")

        val storageId = documentStorageService.storeDocument(
            file = command.file,
            protocolId = ProtocolId(command.protocolId),
            documentType = command.documentType,
            description = command.description
        )

        // Publish event
        eventPublisher.publish(
            ProtocolDocumentUploadedEvent(
                protocolId = command.protocolId,
                documentId = storageId,
                documentType = command.documentType,
                originalName = command.file.originalFilename ?: "document",
                fileSize = command.file.size,
                companyId = securityContext.getCurrentCompanyId(),
                userId = securityContext.getCurrentUserId(),
                userName = securityContext.getCurrentUserName()
            )
        )

        logger.info("Successfully uploaded document for protocol: ${command.protocolId}")
        return storageId
    }
}

@Service
class DeleteProtocolDocumentCommandHandler(
    private val documentStorageService: ProtocolDocumentStorageService,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<DeleteProtocolDocumentCommand, Unit> {

    private val logger = LoggerFactory.getLogger(DeleteProtocolDocumentCommandHandler::class.java)

    @Transactional
    override fun handle(command: DeleteProtocolDocumentCommand) {
        logger.info("Deleting document: ${command.documentId} for protocol: ${command.protocolId}")

        val metadata = documentStorageService.getDocumentMetadata(command.documentId)

        val deleted = documentStorageService.deleteDocument(command.documentId)

        if (!deleted) {
            throw IllegalArgumentException("Document not found or could not be deleted: ${command.documentId}")
        }

        // Publish event
        if (metadata != null) {
            eventPublisher.publish(
                ProtocolDocumentDeletedEvent(
                    protocolId = command.protocolId,
                    documentId = command.documentId,
                    documentType = metadata.documentType,
                    originalName = metadata.originalName,
                    companyId = securityContext.getCurrentCompanyId(),
                    userId = securityContext.getCurrentUserId(),
                    userName = securityContext.getCurrentUserName()
                )
            )
        }

        logger.info("Successfully deleted document: ${command.documentId}")
    }
}