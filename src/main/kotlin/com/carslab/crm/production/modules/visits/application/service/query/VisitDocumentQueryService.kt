package com.carslab.crm.production.modules.visits.application.service.query

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

    fun getVisitDocuments(visitId: String): List<VisitDocumentResponse> {
        logger.debug("Fetching documents for visit: {}", visitId)

        val documents = documentService.getDocumentsForVisit(VisitId.of(visitId))
        return documents.map { VisitDocumentResponse.from(it) }
    }

    fun getDocumentFile(documentId: String): ByteArray? {
        logger.debug("Fetching document file: {}", documentId)
        return documentService.getDocumentData(documentId)
    }
}