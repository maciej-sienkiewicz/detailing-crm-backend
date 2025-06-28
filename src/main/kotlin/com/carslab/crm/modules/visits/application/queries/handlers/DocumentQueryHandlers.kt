package com.carslab.crm.modules.visits.application.queries.handlers

import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.modules.visits.infrastructure.storage.ProtocolDocumentStorageService
import com.carslab.crm.infrastructure.cqrs.QueryHandler
import com.carslab.crm.domain.model.ProtocolId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class GetProtocolDocumentsQueryHandler(
    private val documentStorageService: ProtocolDocumentStorageService
) : QueryHandler<GetProtocolDocumentsQuery, List<ProtocolDocumentResponse>> {

    private val logger = LoggerFactory.getLogger(GetProtocolDocumentsQueryHandler::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

    override fun handle(query: GetProtocolDocumentsQuery): List<ProtocolDocumentResponse> {
        logger.debug("Getting documents for protocol: ${query.protocolId}")

        val documents = documentStorageService.getDocumentsByProtocol(ProtocolId(query.protocolId))

        return documents.map { document ->
            ProtocolDocumentResponse(
                storageId = document.storageId,
                protocolId = document.protocolId.value,
                originalName = document.originalName,
                fileSize = document.fileSize,
                contentType = document.contentType,
                documentType = document.documentType,
                description = document.description,
                createdAt = document.createdAt.format(dateFormatter),
                uploadedBy = document.uploadedBy,
                downloadUrl = "/api/v1/protocols/document/${document.storageId}"
            )
        }
    }
}

@Service
class GetProtocolDocumentQueryHandler(
    private val documentStorageService: ProtocolDocumentStorageService
) : QueryHandler<GetProtocolDocumentQuery, ProtocolDocumentDataModel?> {

    private val logger = LoggerFactory.getLogger(GetProtocolDocumentQueryHandler::class.java)

    override fun handle(query: GetProtocolDocumentQuery): ProtocolDocumentDataModel? {
        logger.debug("Getting document data: ${query.documentId}")

        val metadata = documentStorageService.getDocumentMetadata(query.documentId)
            ?: return null

        val data = documentStorageService.getDocumentData(query.documentId)
            ?: return null

        return ProtocolDocumentDataModel(
            data = data,
            contentType = metadata.contentType,
            originalName = metadata.originalName,
            size = metadata.fileSize
        )
    }
}