package com.carslab.crm.production.modules.visits.application.service.query

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.media.application.service.MediaQueryService
import com.carslab.crm.production.modules.visits.application.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitGalleryQueryService(
    private val mediaQueryService: MediaQueryService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitGalleryQueryService::class.java)

    fun searchImages(request: GalleryFilterRequest): PaginatedResponse<GalleryImageResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Searching gallery images for company: {} using Media module", companyId)

        val allVisitMedia = mediaQueryService.getAllMediaForCompany(companyId)

        val filteredMedia = if (request.getTagsOrEmpty().isEmpty()) {
            allVisitMedia
        } else {
            filterMediaByTags(allVisitMedia, request.getTagsOrEmpty(), request.getTagMatchModeOrDefault())
        }

        val page = request.getPageOrDefault()
        val size = request.getSizeOrDefault()
        val totalItems = filteredMedia.size.toLong()
        val totalPages = if (size == 0) 1 else ((totalItems + size - 1) / size).toInt()

        val startIndex = page * size
        val endIndex = minOf(startIndex + size, filteredMedia.size)
        val paginatedData = if (startIndex < filteredMedia.size) {
            filteredMedia.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        val responses = paginatedData.map { media ->
            GalleryImageResponse(
                id = media.id,
                name = media.name,
                protocolId = media.visitId ?: "",
                protocolTitle = null,
                clientName = null,
                vehicleInfo = media.vehicleId?.let { "Vehicle ID: $it" },
                size = media.size,
                contentType = media.contentType,
                description = media.description,
                location = media.location,
                tags = media.tags,
                createdAt = media.createdAt,
                thumbnailUrl = "/api/media/${media.id}/thumbnail",
                downloadUrl = "/api/media/${media.id}/download"
            )
        }

        return PaginatedResponse(
            data = responses,
            page = page,
            size = size,
            totalItems = totalItems,
            totalPages = totalPages.toLong()
        )
    }

    fun getGalleryStats(): GalleryStatsResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Getting gallery statistics for company: {}", companyId)

        val allVisitMedia = mediaQueryService.getAllMediaForCompany(companyId)
        val totalImages = allVisitMedia.size.toLong()
        val totalSize = allVisitMedia.sumOf { it.size }

        val tagStats = calculateTagStatistics(allVisitMedia)

        return GalleryStatsResponse(
            totalImages = totalImages,
            totalSize = totalSize,
            availableTags = tagStats
        )
    }

    fun downloadImage(imageId: String): GalleryDownloadResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Downloading image: {} for company: {}", imageId, companyId)

        val mediaFile = mediaQueryService.getMediaWithMetadata(imageId)
            ?: throw RuntimeException("Image not found or access denied")

        return GalleryDownloadResponse(
            resource = org.springframework.core.io.ByteArrayResource(mediaFile.data),
            contentType = mediaFile.contentType,
            originalName = mediaFile.originalName
        )
    }

    fun getThumbnail(imageId: String): GalleryDownloadResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Getting thumbnail for image: {} for company: {}", imageId, companyId)

        return downloadImage(imageId)
    }

    fun getAllTags(): List<String> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Getting all available tags for company: {}", companyId)

        val allMedia = mediaQueryService.getAllMediaForCompany(companyId)
        return allMedia
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }

    private fun filterMediaByTags(
        media: List<com.carslab.crm.production.modules.media.application.dto.MediaResponse>,
        requestTags: List<String>,
        matchMode: TagMatchMode
    ): List<com.carslab.crm.production.modules.media.application.dto.MediaResponse> {
        return when (matchMode) {
            TagMatchMode.ALL -> media.filter { mediaItem ->
                requestTags.all { tag -> mediaItem.tags.contains(tag) }
            }
            TagMatchMode.ANY -> media.filter { mediaItem ->
                requestTags.any { tag -> mediaItem.tags.contains(tag) }
            }
        }
    }

    private fun calculateTagStatistics(
        media: List<com.carslab.crm.production.modules.media.application.dto.MediaResponse>
    ): List<TagStatResponse> {
        return media
            .flatMap { it.tags }
            .groupBy { it }
            .map { (tag, occurrences) ->
                TagStatResponse(tag = tag, count = occurrences.size.toLong())
            }
            .sortedByDescending { it.count }
    }
}