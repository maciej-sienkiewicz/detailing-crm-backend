package com.carslab.crm.production.modules.media.presentation

import com.carslab.crm.production.modules.media.application.dto.UpdateMediaTagsRequest
import com.carslab.crm.production.modules.media.application.service.MediaCommandService
import com.carslab.crm.production.modules.media.application.service.MediaQueryService
import com.carslab.crm.production.shared.observability.annotations.HttpMonitored
import com.carslab.crm.production.shared.presentation.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller dla ogólnych operacji na mediach
 * Głównie do pobierania plików i miniaturek
 */
@RestController
@RequestMapping("/api/media")
@Tag(name = "Media", description = "Media file operations")
class MediaController(
    private val mediaQueryService: MediaQueryService,
    private val mediaCommandService: MediaCommandService,
) : BaseController() {

    @GetMapping("/{mediaId}/download")
    @HttpMonitored(endpoint = "GET_/api/media/{mediaId}/download")
    @Operation(summary = "Download media file", description = "Downloads the full resolution media file")
    fun downloadMedia(
        @Parameter(description = "Media ID", required = true) @PathVariable mediaId: String
    ): ResponseEntity<Resource> {
        logger.info("Downloading media file: $mediaId")

        val mediaFile = mediaQueryService.getMediaWithMetadata(mediaId)
            ?: return ResponseEntity.notFound().build()

        val resource = ByteArrayResource(mediaFile.data)

        logger.info("Successfully prepared media file for download: $mediaId")

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(mediaFile.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${mediaFile.originalName}\"")
            .contentLength(mediaFile.size)
            .body(resource)
    }

    @GetMapping("/{mediaId}/thumbnail")
    @HttpMonitored(endpoint = "GET_/api/media/{mediaId}/thumbnail")
    @Operation(summary = "Get media thumbnail", description = "Gets media thumbnail for preview")
    fun getMediaThumbnail(
        @Parameter(description = "Media ID", required = true) @PathVariable mediaId: String
    ): ResponseEntity<Resource> {
        logger.info("Getting media thumbnail: $mediaId")

        val mediaFile = mediaQueryService.getMediaWithMetadata(mediaId)
            ?: return ResponseEntity.notFound().build()

        val resource = ByteArrayResource(mediaFile.data)

        logger.info("Successfully prepared media thumbnail: $mediaId")

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(mediaFile.contentType))
            .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
            .contentLength(mediaFile.size)
            .body(resource)
    }

    @GetMapping("/{mediaId}/info")
    @HttpMonitored(endpoint = "GET_/api/media/{mediaId}/info")
    @Operation(summary = "Get media information", description = "Gets media metadata without file content")
    fun getMediaInfo(
        @Parameter(description = "Media ID", required = true) @PathVariable mediaId: String
    ): ResponseEntity<Any> {
        logger.info("Getting media info: $mediaId")

        val mediaResponse = mediaQueryService.getMediaById(mediaId)
            ?: return ResponseEntity.notFound().build()

        logger.info("Successfully retrieved media info: $mediaId")
        return ok(mediaResponse)
    }

    @GetMapping("/{mediaId}/exists")
    @HttpMonitored(endpoint = "GET_/api/media/{mediaId}/exists")
    @Operation(summary = "Check if media exists", description = "Checks if media exists and is accessible")
    fun checkMediaExists(
        @Parameter(description = "Media ID", required = true) @PathVariable mediaId: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Checking if media exists: $mediaId")

        val exists = mediaQueryService.existsMedia(mediaId)

        return ok(mapOf(
            "mediaId" to mediaId,
            "exists" to exists
        ))
    }

    @PutMapping("/{mediaId}/tags")
    @HttpMonitored(endpoint = "PUT_/api/media/{mediaId}/tags")
    @Operation(summary = "Update media tags", description = "Updates tags for specified media")
    fun updateMediaTags(
        @Parameter(description = "Media ID", required = true) @PathVariable mediaId: String,
        @RequestBody @Valid request: UpdateMediaTagsRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Updating tags for media: $mediaId with tags: ${request.tags}")

        try {
            mediaCommandService.updateMediaTags(mediaId, request.tags)

            val updatedMedia = mediaQueryService.getMediaById(mediaId)
                ?: return ResponseEntity.notFound().build()

            logger.info("Successfully updated tags for media: $mediaId")

            return ok(mapOf(
                "mediaId" to mediaId,
                "tags" to updatedMedia.tags,
                "message" to "Tags updated successfully"
            ))

        } catch (e: IllegalStateException) {
            logger.warn("Failed to update tags for media: $mediaId - ${e.message}")
            return ResponseEntity.notFound().build()
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid tags for media: $mediaId - ${e.message}")
            return badRequest(e.message ?: "Unknown error occurred")
        }
    }
}