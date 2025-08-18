package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.production.modules.visits.application.dto.*
import com.carslab.crm.production.modules.visits.domain.model.GalleryStats
import com.carslab.crm.production.modules.visits.domain.model.TagStat
import com.carslab.crm.production.modules.visits.domain.repositories.VisitGalleryRepository
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitMediaEntity
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
@Transactional(readOnly = true)
class JpaVisitGalleryRepositoryImpl(
    private val visitGalleryJpaRepository: VisitGalleryJpaRepository,
    private val storageService: UniversalStorageService
) : VisitGalleryRepository {

    private val logger = LoggerFactory.getLogger(JpaVisitGalleryRepositoryImpl::class.java)

    override fun searchImages(companyId: Long, request: GalleryFilterRequest): PaginatedResponse<GalleryImageResponse> {
        val tags = request.getTagsOrEmpty()
        val tagMatchMode = request.getTagMatchModeOrDefault()
        val page = request.getPageOrDefault()
        val size = request.getSizeOrDefault()

        val startDate = parseDate(request.startDate, true)
        val endDate = parseDate(request.endDate, false)
        val protocolId = parseProtocolId(request.protocolId)

        val offset = page * size

        val images = if (tags.isEmpty()) {
            visitGalleryJpaRepository.findImagesWithFiltersNative(
                companyId = companyId,
                protocolId = protocolId,
                name = request.name?.takeIf { it.isNotBlank() },
                startDate = startDate?.toString(),
                endDate = endDate?.toString(),
                size = size,
                offset = offset
            )
        } else {
            when (tagMatchMode) {
                TagMatchMode.ALL -> visitGalleryJpaRepository.findImagesWithAllTags(
                    companyId = companyId,
                    protocolId = protocolId,
                    name = request.name?.takeIf { it.isNotBlank() },
                    startDate = startDate?.toString(),
                    endDate = endDate?.toString(),
                    tags = tags,
                    tagCount = tags.size,
                    size = size,
                    offset = offset
                )
                TagMatchMode.ANY -> visitGalleryJpaRepository.findImagesWithAnyTags(
                    companyId = companyId,
                    protocolId = protocolId,
                    name = request.name?.takeIf { it.isNotBlank() },
                    startDate = startDate?.toString(),
                    endDate = endDate?.toString(),
                    tags = tags,
                    size = size,
                    offset = offset
                )
            }
        }

        val total = if (tags.isEmpty()) {
            visitGalleryJpaRepository.countImagesWithFilters(
                companyId = companyId,
                protocolId = protocolId,
                name = request.name?.takeIf { it.isNotBlank() },
                startDate = startDate.toString(),
                endDate = endDate.toString()
            )
        } else {
            when (tagMatchMode) {
                TagMatchMode.ALL -> visitGalleryJpaRepository.countImagesWithAllTags(
                    companyId = companyId,
                    protocolId = protocolId,
                    name = request.name?.takeIf { it.isNotBlank() },
                    startDate = startDate?.toString(),
                    endDate = endDate?.toString(),
                    tags = tags,
                    tagCount = tags.size
                )
                TagMatchMode.ANY -> visitGalleryJpaRepository.countImagesWithAnyTags(
                    companyId = companyId,
                    protocolId = protocolId,
                    name = request.name?.takeIf { it.isNotBlank() },
                    startDate = startDate?.toString(),
                    endDate = endDate?.toString(),
                    tags = tags
                )
            }
        }

        val responses = images.map { image ->
            GalleryImageResponse(
                id = image.id,
                name = image.name,
                protocolId = image.visitId.toString(),
                protocolTitle = null,
                clientName = null,
                vehicleInfo = null,
                size = image.size,
                contentType = image.contentType,
                description = image.description,
                location = image.location,
                tags = if (image.tags.isBlank()) emptyList() else image.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                createdAt = image.createdAt,
                thumbnailUrl = "/api/gallery/images/${image.id}/thumbnail",
                downloadUrl = "/api/gallery/images/${image.id}/download"
            )
        }

        val totalPages = if (size == 0) 1 else ((total + size - 1) / size).toInt()

        return PaginatedResponse(
            data = responses,
            page = page,
            size = size,
            totalItems = total,
            totalPages = totalPages.toLong()
        )
    }

    override fun getGalleryStats(companyId: Long): GalleryStats {
        val totalImages = visitGalleryJpaRepository.countTotalImages(companyId)
        val totalSize = visitGalleryJpaRepository.getTotalSize(companyId)

        return GalleryStats(
            totalImages = totalImages,
            totalSize = totalSize
        )
    }

    override fun getTagStatistics(companyId: Long): List<TagStat> {
        return visitGalleryJpaRepository.getTagStatistics(companyId).map { row ->
            TagStat(
                tag = row[0] as String,
                count = (row[1] as Number).toLong()
            )
        }
    }

    override fun downloadImage(imageId: String, companyId: Long): GalleryDownloadResponse {
        val image = visitGalleryJpaRepository.findByIdAndCompanyExists(imageId, companyId)
            ?: throw RuntimeException("Image not found or access denied")

        val data = storageService.retrieveFile(imageId)
            ?: throw RuntimeException("Image data not found")

        val resource = ByteArrayResource(data)

        return GalleryDownloadResponse(
            resource = resource,
            contentType = image.contentType,
            originalName = image.name
        )
    }

    override fun getThumbnail(imageId: String, companyId: Long): GalleryDownloadResponse {
        return downloadImage(imageId, companyId)
    }

    override fun getAllTags(companyId: Long): List<String> {
        return visitGalleryJpaRepository.getAllTagsNative(companyId)
    }

    private fun parseDate(dateStr: String?, isStartDate: Boolean): LocalDateTime? {
        return dateStr?.takeIf { it.isNotBlank() }?.let {
            try {
                LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
            } catch (e: Exception) {
                try {
                    val timeStr = if (isStartDate) "T00:00:00" else "T23:59:59"
                    LocalDateTime.parse("${it}${timeStr}")
                } catch (e2: Exception) {
                    logger.warn("Could not parse date: $it", e2)
                    null
                }
            }
        }
    }

    private fun parseProtocolId(protocolId: String?): Long? {
        return protocolId?.takeIf { it.isNotBlank() }?.let {
            try {
                it.toLong()
            } catch (e: NumberFormatException) {
                logger.warn("Could not parse protocol ID as Long: $protocolId")
                null
            }
        }
    }
}