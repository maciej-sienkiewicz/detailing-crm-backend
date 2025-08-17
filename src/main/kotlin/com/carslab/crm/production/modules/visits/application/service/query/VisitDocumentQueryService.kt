package com.carslab.crm.production.modules.visits.application.service.query

import com.carslab.crm.modules.visits.api.response.ProtocolDocumentDto
import com.carslab.crm.production.modules.visits.application.dto.VisitDocumentResponse
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitDocumentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitDocumentQueryService(
    private val documentService: VisitDocumentService
) {
    private val logger = LoggerFactory.getLogger(VisitDocumentQueryService::class.java)

    fun getVisitDocuments(visitId: String): List<ProtocolDocumentDto> {
        logger.debug("Fetching documents for visit: {}", visitId)

        val documents = documentService.getDocumentsForVisit(VisitId.of(visitId))
        return documents.map { document -> ProtocolDocumentDto(
            storageId = document.id,
            protocolId = document.id,
            originalName = document.name,
            fileSize = document.size,
            contentType = document.contentType,
            documentType = document.type.toString(),
            documentTypeDisplay = document.type.toString(),
            description = document.description,
            createdAt = document.createdAt.toString(),
            uploadedBy = document.uploadedBy,
            downloadUrl =  "/api/v1/visits/documents/${document.id}/download",
        ) }
    }

    fun getDocumentFile(documentId: String): ByteArray? {
        logger.debug("Fetching document file: {}", documentId)
        return documentService.getDocumentData(documentId)
    }
}