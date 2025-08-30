// src/main/kotlin/com/carslab/crm/modules/visits/infrastructure/storage/ProtocolDocumentStorageService.kt
package com.carslab.crm.modules.visits.infrastructure.storage

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.view.protocol.ProtocolDocumentType
import com.carslab.crm.domain.model.view.protocol.ProtocolDocumentView
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.production.modules.visits.application.dto.UploadDocumentRequest
import com.carslab.crm.production.modules.visits.application.service.command.VisitDocumentCommandService
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.application.service.query.VisitDocumentQueryService
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
class ProtocolDocumentStorageService(
    private val universalStorageService: UniversalStorageService,
    private val visitDocumentCommandService: VisitDocumentCommandService,
    private val visitDocumentQueryService: VisitDocumentQueryService,
    private val visitDetailQueryService: VisitDetailQueryService,
) {
    private val logger = LoggerFactory.getLogger(ProtocolDocumentStorageService::class.java)

    /**
     * Przechowuje dokument protokołu
     */
    @Transactional
    fun storeDocument(
        file: MultipartFile,
        protocolId: ProtocolId,
        documentType: String,
        description: String? = null
    ): String {
        try {
            if (file.isEmpty) {
                throw RuntimeException("Cannot store empty file")
            }

            val companyId = getCurrentCompanyId()
            val protocolIdLong = protocolId.value.toLong()

            visitDetailQueryService.getSimpleDetails(protocolIdLong.toString())

            // Przechowaj plik używając UniversalStorageService
            val storageId = universalStorageService.storeFile(
                UniversalStoreRequest(
                    file = file,
                    originalFileName = file.originalFilename ?: "document.pdf",
                    contentType = file.contentType ?: "application/pdf",
                    companyId = companyId,
                    entityId = protocolId.value,
                    entityType = "protocol",
                    category = "protocols",
                    subCategory = "documents",
                    description = description,
                    tags = mapOf(
                        "documentType" to documentType,
                        "protocolId" to protocolId.value
                    )
                )
            )
            
            visitDocumentCommandService.uploadDocument(protocolId.value, UploadDocumentRequest(
                file = file,
                documentType = documentType,
                description = description,
            ))
            
            logger.info("Stored document for protocol ${protocolId.value}: $storageId")
            return storageId

        } catch (e: Exception) {
            logger.error("Failed to store document for protocol ${protocolId.value}", e)
            throw RuntimeException("Failed to store document", e)
        }
    }
    
    fun findAcceptanceProtocol(protocolId: ProtocolId): ByteArray? {
        return visitDocumentQueryService.findByVisitIdAndDocumentType(
            protocolId.value, ProtocolDocumentType.ACCEPTANCE_PROTOCOL.toString()
        )
    }

    private fun getCurrentCompanyId(): Long {
        return (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
    }
}