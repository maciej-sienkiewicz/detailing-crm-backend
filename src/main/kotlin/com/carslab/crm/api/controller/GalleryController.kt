// src/main/kotlin/com/carslab/crm/api/controller/GalleryController.kt
package com.carslab.crm.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.request.GalleryFilterRequest
import com.carslab.crm.api.model.response.GalleryImageResponse
import com.carslab.crm.api.model.response.GalleryStatsResponse
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.api.model.response.TagStatResponse
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.GalleryJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ImageTagJpaRepository
import com.carslab.crm.infrastructure.service.GalleryService
import com.carslab.crm.infrastructure.storage.FileImageStorageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/gallery")
@Tag(name = "Gallery", description = "Image gallery management endpoints")
class GalleryController(
    private val galleryRepository: GalleryJpaRepository,
    private val galleryService: GalleryService,
    private val imageTagRepository: ImageTagJpaRepository,
    private val fileStorageService: FileImageStorageService
) : BaseController() {

    @PostMapping("/search")
    @Operation(summary = "Search images in gallery", description = "Search images with various filters including tags")
    fun searchImages(@Valid @RequestBody request: GalleryFilterRequest): ResponseEntity<PaginatedResponse<GalleryImageResponse>> {
        val tags = request.getTagsOrEmpty()
        val tagMatchMode = request.getTagMatchModeOrDefault()
        val page = request.getPageOrDefault()
        val size = request.getSizeOrDefault()

        logger.info("Searching gallery images with filters: tags=$tags, mode=$tagMatchMode, page=$page, size=$size")

        try {
            val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
            val pageable = PageRequest.of(page, size)

            // Parsowanie dat jeśli są podane - konwertujemy na String dla natywnego SQL
            val startDateStr = request.startDate?.takeIf { it.isNotBlank() }?.let {
                try {
                    LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME).toString()
                } catch (e: Exception) {
                    try {
                        "${it}T00:00:00"
                    } catch (e2: Exception) {
                        logger.warn("Could not parse start date: $it", e2)
                        null
                    }
                }
            }

            val endDateStr = request.endDate?.takeIf { it.isNotBlank() }?.let {
                try {
                    LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME).toString()
                } catch (e: Exception) {
                    try {
                        "${it}T23:59:59"
                    } catch (e2: Exception) {
                        logger.warn("Could not parse end date: $it", e2)
                        null
                    }
                }
            }

            // Konwersja protocolId na Long
            val protocolIdLong = request.protocolId?.takeIf { it.isNotBlank() }?.let {
                try {
                    it.toLong()
                } catch (e: NumberFormatException) {
                    logger.warn("Could not parse protocol ID as Long: ${request.protocolId}")
                    null
                }
            }

            logger.debug("Parsed filters - startDate: $startDateStr, endDate: $endDateStr, protocolIdLong: $protocolIdLong")

            val imagesPage = galleryService.findImagesWithFilters(
                companyId = companyId,
                protocolId = protocolIdLong,
                name = request.name?.takeIf { it.isNotBlank() },
                startDate = startDateStr,
                endDate = endDateStr,
                tags = tags,
                tagMatchMode = tagMatchMode.name,
                pageable = pageable
            )

            val responses = imagesPage.content.map { image ->
                val imageTags = try {
                    imageTagRepository.findByImageIdAndCompanyId(image.id, companyId).map { it.tag }
                } catch (e: Exception) {
                    logger.warn("Error fetching tags for image ${image.id}", e)
                    emptyList<String>()
                }

                GalleryImageResponse(
                    id = image.id,
                    name = image.name,
                    protocolId = image.protocolId.toString(),
                    protocolTitle = null,
                    clientName = null,
                    vehicleInfo = null,
                    size = image.size,
                    contentType = image.contentType,
                    description = image.description,
                    location = image.location,
                    tags = imageTags,
                    createdAt = image.createdAt,
                    thumbnailUrl = "/api/gallery/images/${image.id}/thumbnail",
                    downloadUrl = "/api/gallery/images/${image.id}/download"
                )
            }

            val paginatedResponse = PaginatedResponse(
                data = responses,
                page = imagesPage.number,
                size = imagesPage.size,
                totalItems = imagesPage.totalElements,
                totalPages = imagesPage.totalPages.toLong()
            )

            logger.info("Found ${responses.size} images out of ${imagesPage.totalElements} total")
            return ok(paginatedResponse)
        } catch (e: Exception) {
            logger.error("Error searching gallery images", e)
            return logAndRethrow("Error searching gallery images", e)
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get gallery statistics", description = "Get statistics about images and available tags")
    fun getGalleryStats(): ResponseEntity<GalleryStatsResponse> {
        logger.info("Getting gallery statistics")

        try {
            val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

            val totalImages = galleryRepository.countTotalImagesNative(companyId)
            val totalSize = galleryRepository.getTotalSizeNative(companyId)

            val tagStats = try {
                galleryRepository.getTagStatisticsNative(companyId).map { row ->
                    TagStatResponse(
                        tag = row[0] as String,
                        count = (row[1] as Number).toLong()
                    )
                }
            } catch (e: Exception) {
                logger.warn("Error getting tag statistics", e)
                emptyList()
            }

            val stats = GalleryStatsResponse(
                totalImages = totalImages,
                totalSize = totalSize,
                availableTags = tagStats
            )

            logger.info("Gallery stats - images: $totalImages, size: $totalSize, tags: ${tagStats.size}")
            return ok(stats)
        } catch (e: Exception) {
            return logAndRethrow("Error getting gallery statistics", e)
        }
    }

    @GetMapping("/images/{imageId}/download")
    @Operation(summary = "Download image", description = "Download full resolution image")
    fun downloadImage(
        @Parameter(description = "Image ID", required = true) @PathVariable imageId: String
    ): ResponseEntity<Resource> {
        logger.info("Downloading image: $imageId")

        try {
            val resource = fileStorageService.getFileAsResource(imageId)
            val metadata = fileStorageService.getFileMetadata(imageId)
                ?: throw RuntimeException("Image metadata not found")

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${metadata.originalName}\"")
                .body(resource)
        } catch (e: Exception) {
            return logAndRethrow("Error downloading image $imageId", e)
        }
    }

    @GetMapping("/images/{imageId}/thumbnail")
    @Operation(summary = "Get image thumbnail", description = "Get image thumbnail for preview")
    fun getImageThumbnail(
        @Parameter(description = "Image ID", required = true) @PathVariable imageId: String
    ): ResponseEntity<Resource> {
        logger.info("Getting thumbnail for image: $imageId")

        try {
            val resource = fileStorageService.getFileAsResource(imageId)
            val metadata = fileStorageService.getFileMetadata(imageId)
                ?: throw RuntimeException("Image metadata not found")

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(resource)
        } catch (e: Exception) {
            return logAndRethrow("Error getting thumbnail for image $imageId", e)
        }
    }

    @GetMapping("/tags")
    @Operation(summary = "Get all available tags", description = "Get list of all available tags for filtering")
    fun getAllTags(): ResponseEntity<List<String>> {
        logger.info("Getting all available tags")

        try {
            val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
            val tagStats = galleryRepository.getTagStatisticsNative(companyId)
            val tags = tagStats.map { it[0] as String }

            logger.info("Found ${tags.size} unique tags")
            return ok(tags)
        } catch (e: Exception) {
            return logAndRethrow("Error getting available tags", e)
        }
    }
}