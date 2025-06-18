// src/main/kotlin/com/carslab/crm/modules/visits/application/commands/handlers/MediaCommandHandlers.kt
package com.carslab.crm.modules.visits.application.commands.handlers

import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.domain.services.VisitValidationService
import com.carslab.crm.modules.visits.domain.services.VisitMediaService
import com.carslab.crm.modules.visits.domain.ports.ProtocolCommentsRepository
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolComment
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class UploadVisitMediaCommandHandler(
    private val visitValidationService: VisitValidationService,
    private val visitMediaService: VisitMediaService,
    private val visitCommentsRepository: ProtocolCommentsRepository,
    private val securityContext: SecurityContext
) : CommandHandler<UploadVisitMediaCommand, String> {

    private val logger = LoggerFactory.getLogger(UploadVisitMediaCommandHandler::class.java)

    @Transactional
    override fun handle(command: UploadVisitMediaCommand): String {
        val protocolId = ProtocolId(command.visitId)

        logger.info("Uploading media for visit: {} (size: {} bytes)",
            command.visitId, command.file.size)

        val validationResult = visitValidationService.validateVisitAccess(protocolId)
        validationResult.throwIfInvalid()

        validateFile(command.file)

        val mediaId = visitMediaService.uploadMedia(
            visitId = protocolId,
            file = command.file,
            name = command.mediaDetails.name,
            description = command.mediaDetails.description,
            location = command.mediaDetails.location,
            tags = command.mediaDetails.tags
        )

        // Add system comment (non-blocking)
        addSystemComment(protocolId, "Dodano nowe zdjęcie: ${command.mediaDetails.name}")

        logger.info("Successfully uploaded media {} for protocol {}", mediaId, command.visitId)
        return mediaId
    }

    private fun validateFile(file: org.springframework.web.multipart.MultipartFile) {
        if (file.isEmpty) {
            throw IllegalArgumentException("File cannot be empty")
        }

        val allowedTypes = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
        if (file.contentType !in allowedTypes) {
            throw IllegalArgumentException("Only image files are allowed (JPEG, PNG, GIF, WebP)")
        }

        if (file.size > 10 * 1024 * 1024) {
            throw IllegalArgumentException("File size cannot exceed 10MB")
        }

        val fileName = file.originalFilename
        if (fileName.isNullOrBlank() || fileName.length > 255) {
            throw IllegalArgumentException("Invalid filename")
        }
    }

    private fun addSystemComment(protocolId: ProtocolId, content: String) {
        try {
            visitCommentsRepository.save(
                ProtocolComment(
                    protocolId = protocolId,
                    author = securityContext.getCurrentUserName() ?: "System",
                    content = content,
                    timestamp = Instant.now().toString(),
                    type = "system"
                )
            )
        } catch (e: Exception) {
            logger.warn("Failed to add system comment for protocol {} - continuing", protocolId.value, e)
        }
    }
}

@Service
class UpdateVisitMediaCommandHandler(
    private val visitValidationService: VisitValidationService,
    private val visitMediaService: VisitMediaService,
    private val visitCommentsRepository: ProtocolCommentsRepository,
    private val securityContext: SecurityContext
) : CommandHandler<UpdateVisitMediaCommand, Unit> {

    private val logger = LoggerFactory.getLogger(UpdateVisitMediaCommandHandler::class.java)

    @Transactional
    override fun handle(command: UpdateVisitMediaCommand) {
        val visitId = ProtocolId(command.visitId)

        logger.info("Updating media {} for visit: {}", command.mediaId, command.visitId)

        // Fast validation with cache
        val validationResult = visitValidationService.validateVisitAccess(visitId)
        validationResult.throwIfInvalid()

        // Validate input
        validateUpdateRequest(command)

        // Update media metadata
        visitMediaService.updateMediaMetadata(
            visitId = visitId,
            mediaId = command.mediaId,
            name = command.name,
            description = command.description,
            location = command.location,
            tags = command.tags
        )

        // Add system comment
        addSystemComment(visitId, "Zaktualizowano metadane zdjęcia: ${command.name}")

        logger.info("Successfully updated media {} for protocol {}", command.mediaId, command.visitId)
    }

    private fun validateUpdateRequest(command: UpdateVisitMediaCommand) {
        if (command.name.isBlank() || command.name.length > 255) {
            throw IllegalArgumentException("Invalid media name")
        }

        if (command.description != null && command.description.length > 1000) {
            throw IllegalArgumentException("Description too long (max 1000 characters)")
        }

        if (command.location != null && command.location.length > 100) {
            throw IllegalArgumentException("Location too long (max 100 characters)")
        }

        if (command.tags.size > 20) {
            throw IllegalArgumentException("Too many tags (max 20)")
        }

        command.tags.forEach { tag ->
            if (tag.length > 50) {
                throw IllegalArgumentException("Tag too long: $tag (max 50 characters)")
            }
        }
    }

    private fun addSystemComment(visitId: ProtocolId, content: String) {
        try {
            visitCommentsRepository.save(
                ProtocolComment(
                    protocolId = visitId,
                    author = securityContext.getCurrentUserName() ?: "System",
                    content = content,
                    timestamp = Instant.now().toString(),
                    type = "system"
                )
            )
        } catch (e: Exception) {
            logger.warn("Failed to add system comment for protocol {} - continuing", visitId.value, e)
        }
    }
}

@Service
class DeleteVisitMediaCommandHandler(
    private val visitValidationService: VisitValidationService,
    private val visitMediaService: VisitMediaService,
    private val visitCommentsRepository: ProtocolCommentsRepository,
    private val securityContext: SecurityContext
) : CommandHandler<DeleteVisitMediaCommand, Unit> {

    private val logger = LoggerFactory.getLogger(DeleteVisitMediaCommand::class.java)

    @Transactional
    override fun handle(command: DeleteVisitMediaCommand) {
        val visitId = ProtocolId(command.visitId)

        logger.info("Deleting media {} from visit: {}", command.mediaId, command.visitId)

        // Fast validation with cache
        val validationResult = visitValidationService.validateVisitAccess(visitId)
        validationResult.throwIfInvalid()

        // Get media name before deletion for comment
        val mediaName = try {
            visitMediaService.getMediaMetadata(command.mediaId)?.originalName ?: command.mediaId
        } catch (e: Exception) {
            logger.warn("Could not get media metadata for {}, using ID", command.mediaId)
            command.mediaId
        }

        // Delete media
        val deleted = visitMediaService.deleteMedia(visitId, command.mediaId)

        if (!deleted) {
            throw IllegalArgumentException("Media not found or could not be deleted: ${command.mediaId}")
        }

        // Add system comment
        addSystemComment(visitId, "Usunięto zdjęcie: $mediaName")

        logger.info("Successfully deleted media {} from visit {}", command.mediaId, command.visitId)
    }

    private fun addSystemComment(visitId: ProtocolId, content: String) {
        try {
            visitCommentsRepository.save(
                ProtocolComment(
                    protocolId = visitId,
                    author = securityContext.getCurrentUserName() ?: "System",
                    content = content,
                    timestamp = Instant.now().toString(),
                    type = "system"
                )
            )
        } catch (e: Exception) {
            logger.warn("Failed to add system comment for visit {} - continuing", visitId.value, e)
        }
    }
}