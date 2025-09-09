package com.carslab.crm.production.modules.visits.application.service.query

import com.carslab.crm.modules.visits.api.response.ProtocolDocumentDto
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.details.DocumentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitDocumentQueryService(
    private val documentService: DocumentService
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
    
    fun findByVisitIdAndDocumentType(visitId: String, documentType: String): ByteArray? {
        logger.info("Finding documents for visit: {} with type: {}", visitId, documentType)
        
        return documentService.findDocumentsByVisitIdAndType(VisitId.of(visitId), documentType)
            ?.let { documentService.getDocumentData(it.id) }
    }

    fun getDocumentFile(documentId: String): ByteArray? {
        logger.debug("Fetching document file: {}", documentId)
        return documentService.getDocumentData(documentId)
    }

    fun deleteDocument(documentId: String) {
        logger.debug("Deleting document: {}", documentId)
        val deleted = documentService.deleteDocument(documentId)
        if (!deleted) {
            logger.warn("Failed to delete document: {}", documentId)
            throw IllegalStateException("Document not found or could not be deleted")
        }
        logger.info("Document deleted successfully: {}", documentId)
    }
}