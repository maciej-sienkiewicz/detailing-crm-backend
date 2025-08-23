package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.production.modules.visits.domain.command.UploadDocumentCommand
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitDocument
import com.carslab.crm.production.modules.visits.domain.models.enums.DocumentType
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitDocumentRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VisitDocumentService(
    private val documentRepository: VisitDocumentRepository,
    private val storageService: UniversalStorageService
) {
    fun uploadDocument(command: UploadDocumentCommand, companyId: Long, uploadedBy: String): VisitDocument {
        if (!documentRepository.existsVisitByIdAndCompanyId(command.visitId, companyId)) {
            throw EntityNotFoundException("Visit not found: ${command.visitId.value}")
        }

        validateFile(command.file)
        
        val storageId = storageService.storeFile(
            UniversalStoreRequest(
                file = command.file,
                originalFileName = command.file.originalFilename ?: "document.pdf",
                contentType = command.file.contentType ?: "application/pdf",
                companyId = companyId,
                entityId = command.visitId.value.toString(),
                entityType = "visit",
                category = "visits",
                subCategory = "documents",
                description = command.description,
                tags = mapOf("documentType" to command.documentType.name)
            )
        )

        val document = VisitDocument(
            id = storageId,
            visitId = command.visitId,
            name = command.file.originalFilename ?: "document",
            type = command.documentType,
            size = command.file.size,
            contentType = command.file.contentType ?: "application/pdf",
            description = command.description,
            createdAt = LocalDateTime.now(),
            uploadedBy = uploadedBy
        )

        return documentRepository.save(document)
    }

    fun getDocumentsForVisit(visitId: VisitId): List<VisitDocument> {
        return documentRepository.findByVisitId(visitId)
    }

    fun getDocument(documentId: String): VisitDocument? {
        return documentRepository.findById(documentId)
    }

    fun getDocumentData(documentId: String): ByteArray? {
        return documentRepository.getFileData(documentId)
    }

    fun deleteDocument(documentId: String): Boolean {
        return documentRepository.deleteById(documentId)
    }

    private fun validateFile(file: org.springframework.web.multipart.MultipartFile) {
        if (file.isEmpty) {
            throw BusinessException("File cannot be empty")
        }

        val allowedTypes = setOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/jpg",
            "image/png"
        )

        if (file.contentType !in allowedTypes) {
            throw BusinessException("Only PDF, DOC, JPG, JPEG, PNG and DOCX files are allowed")
        }

        if (file.size > 10 * 1024 * 1024) {
            throw BusinessException("File size cannot exceed 10MB")
        }
    }

    fun findDocumentsByVisitIdAndType(visitId: VisitId, documentType: String): VisitDocument {
        val type = try {
            DocumentType.valueOf(documentType.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BusinessException("Invalid document type: $documentType")
        }

        return documentRepository.findByVisitId(visitId).filter { it.type == type }.maxBy { it.createdAt }
    }
}