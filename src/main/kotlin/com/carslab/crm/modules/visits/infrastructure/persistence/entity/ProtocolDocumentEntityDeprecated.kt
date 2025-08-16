// src/main/kotlin/com/carslab/crm/modules/visits/infrastructure/persistence/entity/ProtocolDocumentEntity.kt
package com.carslab.crm.modules.visits.infrastructure.persistence.entity

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.view.protocol.ProtocolDocumentView
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "protocol_documents_deprecated",
)
class ProtocolDocumentEntityDeprecated(
    @Id
    @Column(nullable = false)
    val storageId: String, // UUID z UniversalStorageService

    @Column(nullable = false)
    val companyId: Long,

    @Column(name = "protocol_id", nullable = false)
    val protocolId: Long,

    @Column(nullable = false)
    val originalName: String,

    @Column(nullable = false)
    val fileSize: Long,

    @Column(nullable = false)
    val contentType: String,

    @Column(nullable = false)
    val documentType: String, // "MARKETING_CONSENT", "SERVICE_CONSENT", "OTHER"

    @Column(nullable = true)
    val description: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val uploadedBy: String // Nazwa użytkownika, który wgrał dokument
) {
    fun toDomain(): ProtocolDocumentView {
        return ProtocolDocumentView(
            storageId = storageId,
            protocolId = ProtocolId(protocolId.toString()),
            originalName = originalName,
            fileSize = fileSize,
            contentType = contentType,
            documentType = documentType,
            description = description,
            createdAt = createdAt,
            uploadedBy = uploadedBy
        )
    }

    companion object {
        fun fromDomain(
            domain: ProtocolDocumentView,
            companyId: Long
        ): ProtocolDocumentEntityDeprecated {
            return ProtocolDocumentEntityDeprecated(
                storageId = domain.storageId,
                companyId = companyId,
                protocolId = domain.protocolId.value.toLong(),
                originalName = domain.originalName,
                fileSize = domain.fileSize,
                contentType = domain.contentType,
                documentType = domain.documentType,
                description = domain.description,
                createdAt = domain.createdAt,
                uploadedBy = domain.uploadedBy
            )
        }
    }
}