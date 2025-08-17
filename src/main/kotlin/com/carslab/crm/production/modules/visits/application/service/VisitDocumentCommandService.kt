package com.carslab.crm.production.modules.visits.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.UploadDocumentRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitDocumentResponse
import com.carslab.crm.production.modules.visits.domain.command.UploadDocumentCommand
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitDocumentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VisitDocumentCommandService(
    private val documentService: VisitDocumentService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitDocumentCommandService::class.java)

    fun uploadDocument(visitId: String, request: UploadDocumentRequest): VisitDocumentResponse {
        val companyId = securityContext.getCurrentCompanyId()
        val uploadedBy = securityContext.getCurrentUserName() ?: "System"

        logger.info("Uploading document to visit: {} for company: {}", visitId, companyId)

        val command = UploadDocumentCommand(
            visitId = VisitId.of(visitId),
            file = request.file,
            documentType = request.documentType,
            description = request.description
        )

        val document = documentService.uploadDocument(command, companyId, uploadedBy)
        logger.info("Document uploaded successfully to visit: {}", visitId)

        return VisitDocumentResponse.from(document)
    }
}