package com.carslab.crm.production.modules.visits.domain.repositories

import com.carslab.crm.production.modules.visits.application.dto.GalleryDownloadResponse
import com.carslab.crm.production.modules.visits.application.dto.GalleryFilterRequest
import com.carslab.crm.production.modules.visits.application.dto.GalleryImageResponse
import com.carslab.crm.production.modules.visits.application.dto.PaginatedResponse
import com.carslab.crm.production.modules.visits.domain.model.GalleryStats
import com.carslab.crm.production.modules.visits.domain.model.TagStat

interface VisitGalleryRepository {
    fun searchImages(companyId: Long, request: GalleryFilterRequest): PaginatedResponse<GalleryImageResponse>
    fun getGalleryStats(companyId: Long): GalleryStats
    fun getTagStatistics(companyId: Long): List<TagStat>
    fun downloadImage(imageId: String, companyId: Long): GalleryDownloadResponse
    fun getThumbnail(imageId: String, companyId: Long): GalleryDownloadResponse
    fun getAllTags(companyId: Long): List<String>
}