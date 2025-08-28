package com.carslab.crm.production.modules.visits.api.details

import com.carslab.crm.production.modules.visits.application.dto.MediaUploadResponse
import com.carslab.crm.production.modules.visits.application.dto.VisitMediaResponse
import com.carslab.crm.production.modules.visits.application.service.command.VisitMediaCommandService
import com.carslab.crm.production.modules.visits.application.service.query.VisitMediaQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartHttpServletRequest

@RestController
@RequestMapping("/api/v1/protocols")
@Tag(name = "Visit Media", description = "Media operations for visits")
class VisitMediaController(
    private val visitMediaCommandService: VisitMediaCommandService,
    private val visitMediaQueryService: VisitMediaQueryService
) {

    @PostMapping("/{visitId}/media", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload media to visit")
    fun uploadMedia(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        request: MultipartHttpServletRequest
    ): ResponseEntity<MediaUploadResponse> {
        val media = visitMediaCommandService.uploadMedia(visitId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(media)
    }

    @GetMapping("/{visitId}/images")
    @Operation(summary = "Get visit media")
    fun getVisitMedia(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<List<VisitMediaResponse>> {
        val media = visitMediaQueryService.getVisitMedia(visitId)
        return ResponseEntity.ok(media)
    }

    @GetMapping("/media/{mediaId}/download")
    @Operation(summary = "Download media file")
    fun downloadMedia(
        @Parameter(description = "Media ID") @PathVariable mediaId: String
    ): ResponseEntity<Resource> {
        val mediaData = visitMediaQueryService.getMediaFile(mediaId)
            ?: return ResponseEntity.notFound().build()

        val resource = ByteArrayResource(mediaData)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"media_$mediaId\"")
            .body(resource)
    }

    @GetMapping("/image/{fileId}")
    @Operation(summary = "Get image file")
    fun getImage(
        @Parameter(description = "File ID") @PathVariable fileId: String
    ): ResponseEntity<Resource> {
        val imageData = visitMediaQueryService.getImageWithMetadata(fileId)
            ?: return ResponseEntity.notFound().build()

        val resource = ByteArrayResource(imageData.data)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(imageData.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${imageData.originalName}\"")
            .contentLength(imageData.size)
            .body(resource)
    }

    @DeleteMapping("/{visitId}/image/{mediaId}")
    @Operation(summary = "Delete image file")
    fun deleteMedia(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @Parameter(description = "Media ID") @PathVariable mediaId: String
    ): ResponseEntity<Void> {
        visitMediaCommandService.deleteMedia(mediaId)
        return ResponseEntity.noContent().build()
    }
}