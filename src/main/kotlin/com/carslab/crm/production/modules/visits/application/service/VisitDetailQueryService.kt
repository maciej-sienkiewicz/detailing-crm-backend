package com.carslab.crm.production.modules.visits.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.api.commands.*
import com.carslab.crm.modules.visits.api.request.ApiDiscountType
import com.carslab.crm.modules.visits.api.request.ApiReferralSource
import com.carslab.crm.modules.visits.api.request.ServiceApprovalStatus
import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.production.modules.visits.application.queries.models.VisitDetailReadModel
import com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitDetailQueryRepository
import com.carslab.crm.production.modules.visits.domain.service.VisitMediaService
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitDetailQueryService(
    private val visitDetailQueryRepository: VisitDetailQueryRepository,
    private val visitMediaService: VisitMediaService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitDetailQueryService::class.java)

    fun getVisitDetail(visitId: String): CarReceptionDetailDto {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visit detail: {} for company: {}", visitId, companyId)

        val visitDetail = visitDetailQueryRepository.findVisitDetailById(VisitId.of(visitId), companyId)
            ?: throw EntityNotFoundException("Visit not found: $visitId")

        val mediaItems = visitMediaService.getMediaForVisit(VisitId.of(visitId))

        logger.debug("Visit detail found: {}", visitDetail.title)

        return mapToCarReceptionDetailDto(visitDetail, mediaItems)
    }

    private fun mapToCarReceptionDetailDto(
        visitDetail: VisitDetailReadModel,
        mediaItems: List<com.carslab.crm.production.modules.visits.domain.models.entities.VisitMedia>
    ): CarReceptionDetailDto {
        return CarReceptionDetailDto(
            id = visitDetail.id,
            title = visitDetail.title,
            calendarColorId = visitDetail.calendarColorId,
            startDate = visitDetail.period.startDate,
            endDate = visitDetail.period.endDate,
            licensePlate = visitDetail.vehicle.licensePlate,
            make = visitDetail.vehicle.make,
            model = visitDetail.vehicle.model,
            productionYear = visitDetail.vehicle.productionYear,
            mileage = visitDetail.vehicle.mileage,
            vin = visitDetail.vehicle.vin,
            color = visitDetail.vehicle.color,
            keysProvided = visitDetail.documents.keysProvided,
            documentsProvided = visitDetail.documents.documentsProvided,
            ownerId = visitDetail.client.id?.toLong(),
            ownerName = visitDetail.client.name,
            companyName = visitDetail.client.companyName,
            taxId = visitDetail.client.taxId,
            email = visitDetail.client.email,
            phone = visitDetail.client.phone,
            address = visitDetail.client.address,
            notes = visitDetail.notes,
            selectedServices = visitDetail.services.map { service ->
                ServiceDto(
                    id = service.id,
                    name = service.name,
                    quantity = service.quantity,
                    price = service.basePrice.toDouble(),
                    discountType = service.discountType?.let { mapToApiDiscountType(it) },
                    discountValue = service.discountValue.toDouble(),
                    finalPrice = service.finalPrice.toDouble(),
                    approvalStatus = mapToServiceApprovalStatus(service.approvalStatus),
                    note = service.note
                )
            },
            status = mapToApiProtocolStatus(visitDetail.status),
            referralSource = visitDetail.referralSource?.let { mapToApiReferralSource(it) },
            otherSourceDetails = visitDetail.otherSourceDetails,
            vehicleImages = mediaItems.map { media ->
                VehicleImageDto(
                    id = media.id,
                    name = media.name,
                    size = media.size,
                    type = media.contentType,
                    storageId = media.id,
                    createdAt = java.time.Instant.from(media.createdAt.atZone(java.time.ZoneId.systemDefault())),
                    description = media.description,
                    location = media.location,
                    tags = media.tags
                )
            },
            createdAt = visitDetail.audit.createdAt,
            updatedAt = visitDetail.audit.updatedAt,
            statusUpdatedAt = visitDetail.audit.statusUpdatedAt,
            appointmentId = visitDetail.appointmentId
        )
    }

    private fun mapToApiProtocolStatus(status: String): ApiProtocolStatus {
        return try {
            ApiProtocolStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            ApiProtocolStatus.SCHEDULED
        }
    }

    private fun mapToApiReferralSource(source: String): ApiReferralSource {
        return try {
            ApiReferralSource.valueOf(source)
        } catch (e: IllegalArgumentException) {
            ApiReferralSource.OTHER
        }
    }

    private fun mapToApiDiscountType(type: String): ApiDiscountType {
        return when (type) {
            "PERCENTAGE" -> ApiDiscountType.PERCENTAGE
            "AMOUNT" -> ApiDiscountType.AMOUNT
            "FIXED_PRICE" -> ApiDiscountType.FIXED_PRICE
            else -> ApiDiscountType.AMOUNT
        }
    }

    private fun mapToServiceApprovalStatus(status: String): ServiceApprovalStatus {
        return when (status) {
            "PENDING" -> ServiceApprovalStatus.PENDING
            "APPROVED" -> ServiceApprovalStatus.APPROVED
            "REJECTED" -> ServiceApprovalStatus.REJECTED
            else -> ServiceApprovalStatus.PENDING
        }
    }
}