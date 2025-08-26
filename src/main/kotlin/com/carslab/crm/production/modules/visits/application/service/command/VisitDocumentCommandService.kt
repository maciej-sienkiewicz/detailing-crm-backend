package com.carslab.crm.production.modules.visits.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.api.response.ProtocolDocumentDto
import com.carslab.crm.production.modules.visits.application.dto.UploadDocumentRequest
import com.carslab.crm.production.modules.visits.domain.command.UploadDocumentCommand
import com.carslab.crm.production.modules.visits.domain.models.enums.DocumentType
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.details.DocumentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VisitDocumentCommandService(
    private val documentService: DocumentService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitDocumentCommandService::class.java)

    fun uploadDocument(visitId: String, request: UploadDocumentRequest): ProtocolDocumentDto {
        val companyId = securityContext.getCurrentCompanyId()
        val uploadedBy = securityContext.getCurrentUserName() ?: "System"

        logger.info("Uploading document to visit: {} for company: {}", visitId, companyId)

        val command = UploadDocumentCommand(
            visitId = VisitId.of(visitId),
            file = request.file,
            documentType = request.documentType.uppercase().let { DocumentType.valueOf(it) },
            description = request.description
        )

        val document = documentService.uploadDocument(command, companyId, uploadedBy)
        logger.info("Document uploaded successfully to visit: {}", visitId)

        return ProtocolDocumentDto(
            storageId = document.id,
            protocolId = document.visitId.value.toString(),
            originalName = document.name,
            fileSize = document.size,
            contentType = document.contentType,
            documentType = document.type.toString(),
            documentTypeDisplay = document.type.toString(),
            description = document.description,
            createdAt = document.createdAt.toString(),
            uploadedBy = document.uploadedBy,
            downloadUrl =  "/api/v1/visits/documents/${document.id}/download",
        )
    }
}