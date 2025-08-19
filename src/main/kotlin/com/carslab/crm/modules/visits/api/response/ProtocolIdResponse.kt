package com.carslab.crm.modules.visits.api.response

import com.carslab.crm.domain.model.view.protocol.ProtocolDocumentType
import com.carslab.crm.domain.model.view.protocol.ProtocolDocumentView
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.format.DateTimeFormatter

data class ProtocolIdResponse(val id: String)

data class ProtocolDocumentDto(
    @JsonProperty("storage_id")
    val storageId: String,

    @JsonProperty("protocol_id")
    val protocolId: String,

    @JsonProperty("original_name")
    val originalName: String,

    @JsonProperty("file_size")
    val fileSize: Long,

    @JsonProperty("content_type")
    val contentType: String,

    @JsonProperty("document_type")
    val documentType: String,

    @JsonProperty("document_type_display")
    val documentTypeDisplay: String,

    @JsonProperty("description")
    val description: String?,

    @JsonProperty("created_at")
    val createdAt: String,

    @JsonProperty("uploaded_by")
    val uploadedBy: String,

    @JsonProperty("download_url")
    val downloadUrl: String
) {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

        fun fromDomain(domain: ProtocolDocumentView): ProtocolDocumentDto {
            val documentType = ProtocolDocumentType.fromString(domain.documentType)

            return ProtocolDocumentDto(
                storageId = domain.storageId,
                protocolId = domain.protocolId.value,
                originalName = domain.originalName,
                fileSize = domain.fileSize,
                contentType = domain.contentType,
                documentType = domain.documentType,
                documentTypeDisplay = domain.documentType,
                description = domain.description,
                createdAt = domain.createdAt.format(DATE_FORMATTER),
                uploadedBy = domain.uploadedBy,
                downloadUrl = "/api/receptions/document/${domain.storageId}"
            )
        }

        private fun getDocumentTypeDisplay(documentType: String): String {
            return ProtocolDocumentType.entries.firstOrNull { it.name == documentType }?.displayName ?: ProtocolDocumentType.OTHER.displayName
        }
    }
}