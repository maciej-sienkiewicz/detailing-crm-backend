package com.carslab.crm.production.modules.visits.domain.service.details

import com.carslab.crm.production.modules.visits.application.dto.GalleryDownloadResponse
import com.carslab.crm.production.modules.visits.application.dto.GalleryFilterRequest
import com.carslab.crm.production.modules.visits.application.dto.GalleryImageResponse
import com.carslab.crm.production.modules.visits.application.dto.PaginatedResponse
import com.carslab.crm.production.modules.visits.domain.models.aggregates.GalleryStats
import com.carslab.crm.production.modules.visits.domain.models.aggregates.TagStat
import com.carslab.crm.production.modules.visits.domain.repositories.VisitGalleryRepository
import org.springframework.stereotype.Service

@Service
class GalleryService(
    private val visitGalleryRepository: VisitGalleryRepository
) {

    fun searchImages(companyId: Long, request: GalleryFilterRequest): PaginatedResponse<GalleryImageResponse> {
        return visitGalleryRepository.searchImages(companyId, request)
    }

    fun getGalleryStats(companyId: Long): GalleryStats {
        return visitGalleryRepository.getGalleryStats(companyId)
    }

    fun getTagStatistics(companyId: Long): List<TagStat> {
        return visitGalleryRepository.getTagStatistics(companyId)
    }

    fun downloadImage(imageId: String, companyId: Long): GalleryDownloadResponse {
        return visitGalleryRepository.downloadImage(imageId, companyId)
    }

    fun getThumbnail(imageId: String, companyId: Long): GalleryDownloadResponse {
        return visitGalleryRepository.getThumbnail(imageId, companyId)
    }

    fun getAllTags(companyId: Long): List<String> {
        return visitGalleryRepository.getAllTags(companyId)
    }
}