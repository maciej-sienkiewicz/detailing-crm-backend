package com.carslab.crm.production.modules.visits.api.details

import com.carslab.crm.production.modules.visits.application.dto.GalleryFilterRequest
import com.carslab.crm.production.modules.visits.application.dto.GalleryImageResponse
import com.carslab.crm.production.modules.visits.application.dto.GalleryStatsResponse
import com.carslab.crm.production.modules.visits.application.dto.PaginatedResponse
import com.carslab.crm.production.modules.visits.application.service.query.VisitGalleryQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/gallery")
@Tag(name = "Gallery", description = "Image gallery management endpoints")
class VisitGalleryController(
    private val visitGalleryQueryService: VisitGalleryQueryService
) {

    @PostMapping("/search")
    @Operation(summary = "Search images in gallery", description = "Search images with various filters including tags")
    fun searchImages(@Valid @RequestBody request: GalleryFilterRequest): ResponseEntity<PaginatedResponse<GalleryImageResponse>> {
        val result = visitGalleryQueryService.searchImages(request)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/stats")
    @Operation(summary = "Get gallery statistics", description = "Get statistics about images and available tags")
    fun getGalleryStats(): ResponseEntity<GalleryStatsResponse> {
        val stats = visitGalleryQueryService.getGalleryStats()
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/images/{imageId}/download")
    @Operation(summary = "Download image", description = "Download full resolution image")
    fun downloadImage(
        @Parameter(description = "Image ID", required = true) @PathVariable imageId: String
    ): ResponseEntity<Resource> {
        val result = visitGalleryQueryService.downloadImage(imageId)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(result.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${result.originalName}\"")
            .body(result.resource)
    }

    @GetMapping("/images/{imageId}/thumbnail")
    @Operation(summary = "Get image thumbnail", description = "Get image thumbnail for preview")
    fun getImageThumbnail(
        @Parameter(description = "Image ID", required = true) @PathVariable imageId: String
    ): ResponseEntity<Resource> {
        val result = visitGalleryQueryService.getThumbnail(imageId)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(result.contentType))
            .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
            .body(result.resource)
    }

    @GetMapping("/tags")
    @Operation(summary = "Get all available tags", description = "Get list of all available tags for filtering")
    fun getAllTags(): ResponseEntity<List<String>> {
        val tags = visitGalleryQueryService.getAllTags()
        return ResponseEntity.ok(tags)
    }
}