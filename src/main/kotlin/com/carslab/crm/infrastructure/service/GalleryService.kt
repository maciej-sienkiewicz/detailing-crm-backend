// src/main/kotlin/com/carslab/crm/infrastructure/service/GalleryService.kt
package com.carslab.crm.infrastructure.service

import com.carslab.crm.infrastructure.persistence.entity.VehicleImageEntity
import com.carslab.crm.infrastructure.persistence.repository.GalleryJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ImageTagJpaRepository
import com.carslab.crm.infrastructure.storage.FileImageStorageService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GalleryService(private val galleryRepository: GalleryJpaRepository,
    private val imageTagJpaRepository: ImageTagJpaRepository,
    private val fileImageStorageService: FileImageStorageService) {

    fun findImagesWithFilters(
        companyId: Long,
        protocolId: Long?,
        name: String?,
        startDate: String?,
        endDate: String?,
        tags: List<String>,
        tagMatchMode: String,
        pageable: Pageable
    ): Page<VehicleImageEntity> {
        val offset = pageable.offset.toInt()
        val size = pageable.pageSize


        return if (tags.isEmpty()) {
            val images = imageTagJpaRepository.findByTagsAndCompanyId(tags, companyId, tagMatchMode,  tags.size.toLong(), pageable)
            val content = fileImageStorageService.find(images.toList())

            val total = galleryRepository.countImagesWithFiltersNative(
                companyId = companyId,
                protocolId = protocolId,
                name = name,
                startDate = startDate,
                endDate = endDate
            )

            PageImpl(content, pageable, total)
        } else {
            val tagsArray = tags.toTypedArray()

            val images = imageTagJpaRepository.findByTagsAndCompanyId(tags, companyId, tagMatchMode,  tags.size.toLong(), pageable)
            val content = fileImageStorageService.find(images.toList())

            val total = galleryRepository.countImagesWithTagFiltersNative(
                companyId = companyId,
                protocolId = protocolId,
                name = name,
                startDate = startDate,
                endDate = endDate,
                tags = tagsArray,
                tagsEmpty = false,
                matchMode = tagMatchMode,
                tagCount = tags.size
            )

            PageImpl(content, pageable, total)
        }
    }
}