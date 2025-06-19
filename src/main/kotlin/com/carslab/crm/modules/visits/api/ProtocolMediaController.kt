// src/main/kotlin/com/carslab/crm/modules/visits/api/ProtocolMediaController.kt
package com.carslab.crm.modules.visits.api

import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.infrastructure.cqrs.CommandBus
import com.carslab.crm.infrastructure.cqrs.QueryBus
import com.carslab.crm.modules.visits.infrastructure.processor.RequestProcessorFactory
import com.carslab.crm.modules.visits.infrastructure.processor.exceptions.DomainException
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

@RestController
@RequestMapping("/api/v1/protocols")
@Tag(name = "Protocol Media Management", description = "CQRS-based protocol media management endpoints")
class ProtocolMediaController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
    private val objectMapper: ObjectMapper,
    private val requestProcessorFactory: RequestProcessorFactory,
) {
    private val logger = LoggerFactory.getLogger(ProtocolMediaController::class.java)

    @PostMapping("/{protocolId}/media", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadMedia(
        @PathVariable protocolId: String,
        request: MultipartHttpServletRequest
    ): ResponseEntity<MediaUploadResponse> {

        return try {
            // Single Responsibility: get right processor
            val processor = requestProcessorFactory.getProcessor(request)

            // Single Responsibility: extract data
            val mediaData = processor.extractMediaData(request)

            // Single Responsibility: execute business logic
            val command = UploadVisitMediaCommand(
                visitId = protocolId,
                file = mediaData.fileData,
                mediaDetails = MediaDetailsCommand(
                    name = mediaData.metadata.name,
                    description = mediaData.metadata.description,
                    location = mediaData.metadata.location,
                    tags = mediaData.metadata.tags.toList(),
                    type = "PHOTO",
                )
            )

            val mediaId = commandBus.execute(command)

            ResponseEntity.status(HttpStatus.CREATED)
                .body(MediaUploadResponse(mediaId, protocolId))

        } catch (e: DomainException) {
            logger.warn("Domain error uploading media to protocol $protocolId: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Unexpected error uploading media to protocol $protocolId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/{protocolId}/media/bulk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload multiple media files", description = "Upload multiple image files to a protocol with metadata")
    fun uploadMultipleMedia(
        @Parameter(description = "Protocol ID", required = true)
        @PathVariable protocolId: String,
        request: MultipartHttpServletRequest
    ): ResponseEntity<BulkMediaUploadResponse> {
        return try {
            logger.info("Uploading multiple media files for protocol: $protocolId")

            val uploadedFiles = mutableListOf<MediaUploadResult>()
            val errors = mutableListOf<MediaUploadError>()

            // Process each file in the request
            request.fileMap.forEach { (paramName, file) ->
                try {
                    // Extract metadata for this file (if provided)
                    val mediaDetailsJson = request.getParameter("${paramName}_details")
                    val mediaDetails = if (mediaDetailsJson != null) {
                        objectMapper.readValue(mediaDetailsJson, MediaDetailsCommand::class.java)
                    } else {
                        MediaDetailsCommand(
                            name = file.originalFilename ?: "image_${System.currentTimeMillis()}",
                            description = null,
                            location = null,
                            tags = emptyList()
                        )
                    }

                    val command = UploadVisitMediaCommand(
                        visitId = protocolId,
                        file = file,
                        mediaDetails = mediaDetails
                    )

                    val mediaId = commandBus.execute(command)
                    uploadedFiles.add(MediaUploadResult(
                        mediaId = mediaId,
                        originalFileName = file.originalFilename ?: "unknown",
                        size = file.size
                    ))

                } catch (e: Exception) {
                    logger.warn("Failed to upload file $paramName for protocol $protocolId", e)
                    errors.add(MediaUploadError(
                        fileName = file.originalFilename ?: paramName,
                        error = e.message ?: "Unknown error"
                    ))
                }
            }

            logger.info("Bulk upload completed for protocol $protocolId: ${uploadedFiles.size} successful, ${errors.size} errors")

            ResponseEntity.ok(BulkMediaUploadResponse(
                protocolId = protocolId,
                uploadedFiles = uploadedFiles,
                errors = errors
            ))

        } catch (e: Exception) {
            logger.error("Error in bulk media upload for protocol $protocolId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PatchMapping("/{protocolId}/media/{mediaId}")
    @Operation(summary = "Update media metadata", description = "Update metadata for a specific media item")
    fun updateMediaMetadata(
        @Parameter(description = "Protocol ID", required = true)
        @PathVariable protocolId: String,
        @Parameter(description = "Media ID", required = true)
        @PathVariable mediaId: String,
        @RequestBody updateRequest: UpdateMediaRequest
    ): ResponseEntity<Unit> {
        return try {
            logger.info("Updating media metadata for visit: $protocolId, media: $mediaId")

            val command = UpdateVisitMediaCommand(
                visitId = protocolId,
                mediaId = mediaId,
                name = updateRequest.name,
                description = updateRequest.description,
                location = updateRequest.location,
                tags = updateRequest.tags
            )

            commandBus.execute(command)

            logger.info("Successfully updated media $mediaId for protocol $protocolId")
            ResponseEntity.ok().build()

        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid request for updating media $mediaId in protocol $protocolId: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error updating media $mediaId for protocol $protocolId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @DeleteMapping("/{protocolId}/media/{mediaId}")
    @Operation(summary = "Delete media", description = "Delete a specific media item from protocol")
    fun deleteMedia(
        @Parameter(description = "Protocol ID", required = true)
        @PathVariable protocolId: String,
        @Parameter(description = "Media ID", required = true)
        @PathVariable mediaId: String
    ): ResponseEntity<Unit> {
        return try {
            logger.info("Deleting media $mediaId from protocol: $protocolId")

            val command = DeleteVisitMediaCommand(
                visitId = protocolId,
                mediaId = mediaId
            )

            commandBus.execute(command)

            logger.info("Successfully deleted media $mediaId from protocol $protocolId")
            ResponseEntity.noContent().build()

        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid request for deleting media $mediaId from protocol $protocolId: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error deleting media $mediaId from protocol $protocolId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/{protocolId}/media")
    @Operation(summary = "Get protocol media", description = "Get all media items for a specific protocol")
    fun getProtocolMedia(
        @Parameter(description = "Protocol ID", required = true)
        @PathVariable protocolId: String
    ): ResponseEntity<List<MediaReadModel>> {
        return try {
            logger.debug("Getting media for protocol: $protocolId")

            val query = GetVisitMediaQuery(protocolId)
            val mediaItems = queryBus.execute(query)

            ResponseEntity.ok(mediaItems)

        } catch (e: Exception) {
            logger.error("Error getting media for protocol $protocolId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/media/{mediaId}")
    @Operation(summary = "Get media details", description = "Get metadata for a specific media item")
    fun getMediaDetails(
        @Parameter(description = "Media ID", required = true)
        @PathVariable mediaId: String
    ): ResponseEntity<MediaReadModel> {
        return try {
            logger.debug("Getting media details for: $mediaId")

            val query = GetMediaByIdQuery(mediaId)
            val media = queryBus.execute(query)

            if (media != null) {
                ResponseEntity.ok(media)
            } else {
                ResponseEntity.notFound().build()
            }

        } catch (e: Exception) {
            logger.error("Error getting media details for $mediaId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/media/{mediaId}/download")
    @Operation(summary = "Download media file", description = "Download the actual media file")
    fun downloadMedia(
        @Parameter(description = "Media ID", required = true)
        @PathVariable mediaId: String
    ): ResponseEntity<Resource> {
        return try {
            logger.debug("Downloading media file: $mediaId")

            val query = GetMediaFileQuery(mediaId)
            val mediaFile = queryBus.execute(query)

            if (mediaFile != null) {
                val resource = ByteArrayResource(mediaFile.data)

                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mediaFile.contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${mediaFile.originalName}\"")
                    .contentLength(mediaFile.size)
                    .body(resource)
            } else {
                ResponseEntity.notFound().build()
            }

        } catch (e: Exception) {
            logger.error("Error downloading media file $mediaId", e)
            ResponseEntity.internalServerError().build()
        }
    }
}

// Data classes for API requests and responses

data class MediaUploadResponse(
    val mediaId: String,
    val protocolId: String,
    val message: String = "Media uploaded successfully"
)

data class BulkMediaUploadResponse(
    val protocolId: String,
    val uploadedFiles: List<MediaUploadResult>,
    val errors: List<MediaUploadError>
)

data class MediaUploadResult(
    val mediaId: String,
    val originalFileName: String,
    val size: Long
)

data class MediaUploadError(
    val fileName: String,
    val error: String
)

data class UpdateMediaRequest(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList()
)