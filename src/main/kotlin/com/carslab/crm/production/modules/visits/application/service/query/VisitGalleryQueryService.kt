package com.carslab.crm.production.modules.visits.application.service.query

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.*
import com.carslab.crm.production.modules.visits.domain.service.details.GalleryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitGalleryQueryService(
    private val galleryService: GalleryService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitGalleryQueryService::class.java)

    fun searchImages(request: GalleryFilterRequest): PaginatedResponse<GalleryImageResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Searching gallery images for company: {}", companyId)

        return galleryService.searchImages(companyId, request)
    }

    fun getGalleryStats(): GalleryStatsResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Getting gallery statistics for company: {}", companyId)

        val stats = galleryService.getGalleryStats(companyId)
        val tagStats = galleryService.getTagStatistics(companyId)

        return GalleryStatsResponse(
            totalImages = stats.totalImages,
            totalSize = stats.totalSize,
            availableTags = tagStats.map { TagStatResponse(it.tag, it.count) }
        )
    }

    fun downloadImage(imageId: String): GalleryDownloadResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Downloading image: {} for company: {}", imageId, companyId)

        return galleryService.downloadImage(imageId, companyId)
    }

    fun getThumbnail(imageId: String): GalleryDownloadResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Getting thumbnail for image: {} for company: {}", imageId, companyId)

        return galleryService.getThumbnail(imageId, companyId)
    }

    fun getAllTags(): List<String> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Getting all available tags for company: {}", companyId)

        return galleryService.getAllTags(companyId)
    }
}