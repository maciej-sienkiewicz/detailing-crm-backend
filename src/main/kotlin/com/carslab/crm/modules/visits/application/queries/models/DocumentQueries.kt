package com.carslab.crm.modules.visits.application.queries.models

import com.carslab.crm.infrastructure.cqrs.Query

data class GetProtocolDocumentsQuery(
    val protocolId: String
) : Query<List<ProtocolDocumentResponse>>

data class GetProtocolDocumentQuery(
    val documentId: String
) : Query<ProtocolDocumentDataModel?>

data class ProtocolDocumentDataModel(
    val data: ByteArray,
    val contentType: String,
    val originalName: String,
    val size: Long
)

data class ProtocolDocumentResponse(
    val storageId: String,
    val protocolId: String,
    val originalName: String,
    val fileSize: Long,
    val contentType: String,
    val documentType: String,
    val description: String?,
    val createdAt: String,
    val uploadedBy: String,
    val downloadUrl: String
)
