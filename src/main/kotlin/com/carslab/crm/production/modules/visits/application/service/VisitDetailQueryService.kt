package com.carslab.crm.production.modules.visits.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.api.commands.*
import com.carslab.crm.modules.visits.api.request.ApiDiscountType
import com.carslab.crm.modules.visits.api.request.ApiReferralSource
import com.carslab.crm.modules.visits.api.request.ServiceApprovalStatus
import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.production.modules.visits.domain.model.*
import com.carslab.crm.production.modules.visits.domain.repository.VisitDetailRepository
import com.carslab.crm.production.modules.visits.domain.repository.VisitDetailProjection
import com.carslab.crm.production.modules.visits.domain.service.VisitMediaService
import com.carslab.crm.production.modules.visits.infrastructure.repository.VisitServiceJpaRepository
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class VisitDetailQueryService(
    private val visitDetailRepository: VisitDetailRepository,
    private val visitMediaService: VisitMediaService,
    private val visitServiceJpaRepository: VisitServiceJpaRepository,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitDetailQueryService::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun getVisitDetail(visitId: String): CarReceptionDetailDto {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visit detail: {} for company: {}", visitId, companyId)

        val visitDetail = visitDetailRepository.findVisitDetailWithRelations(VisitId.of(visitId), companyId)
            ?: throw EntityNotFoundException("Visit not found: $visitId")

        val services = visitServiceJpaRepository.findByVisitId(visitDetail.visitId)
        val mediaItems = visitMediaService.getMediaForVisit(VisitId.of(visitDetail.visitId))

        logger.debug("Visit detail found: {}", visitDetail.title)

        return mapToCarReceptionDetailDto(visitDetail, services, mediaItems)
    }

    private fun mapToCarReceptionDetailDto(
        visitDetail: VisitDetailProjection,
        services: List<com.carslab.crm.production.modules.visits.infrastructure.entity.VisitServiceEntity>,
        mediaItems: List<VisitMedia>
    ): CarReceptionDetailDto {
        return CarReceptionDetailDto(
            id = visitDetail.visitId.toString(),
            title = visitDetail.title,
            calendarColorId = visitDetail.calendarColorId,
            startDate = visitDetail.startDate.format(dateTimeFormatter),
            endDate = visitDetail.endDate.format(dateTimeFormatter),
            licensePlate = visitDetail.vehicleLicensePlate,
            make = visitDetail.vehicleMake,
            model = visitDetail.vehicleModel,
            productionYear = visitDetail.vehicleYear ?: 0,
            mileage = visitDetail.vehicleMileage,
            vin = visitDetail.vehicleVin,
            color = visitDetail.vehicleColor,
            keysProvided = visitDetail.keysProvided,
            documentsProvided = visitDetail.documentsProvided,
            ownerId = visitDetail.clientId,
            ownerName = visitDetail.clientName,
            companyName = visitDetail.clientCompany,
            taxId = visitDetail.clientTaxId,
            email = visitDetail.clientEmail,
            phone = visitDetail.clientPhone,
            address = visitDetail.clientAddress,
            notes = visitDetail.notes,
            selectedServices = services.map { service ->
                ServiceDto(
                    id = service.id,
                    name = service.name,
                    quantity = service.quantity,
                    price = service.basePrice.toDouble(),
                    discountType = service.discountType?.let { mapToApiDiscountType(it) },
                    discountValue = service.discountValue?.toDouble() ?: 0.0,
                    finalPrice = service.finalPrice.toDouble(),
                    approvalStatus = mapToServiceApprovalStatus(service.approvalStatus),
                    note = service.note
                )
            },
            status = mapToApiProtocolStatus(visitDetail.status),
            referralSource = visitDetail.referralSource?.let { mapToApiReferralSource(it) },
            otherSourceDetails = null,
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
            createdAt = visitDetail.createdAt.format(dateTimeFormatter),
            updatedAt = visitDetail.updatedAt.format(dateTimeFormatter),
            statusUpdatedAt = visitDetail.updatedAt.format(dateTimeFormatter),
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

    private fun mapToApiDiscountType(type: DiscountType): ApiDiscountType {
        return when (type) {
            DiscountType.PERCENTAGE -> ApiDiscountType.PERCENTAGE
            DiscountType.AMOUNT -> ApiDiscountType.AMOUNT
            DiscountType.FIXED_PRICE -> ApiDiscountType.FIXED_PRICE
        }
    }

    private fun mapToServiceApprovalStatus(status: com.carslab.crm.production.modules.visits.domain.model.ServiceApprovalStatus): ServiceApprovalStatus {
        return when (status) {
            com.carslab.crm.production.modules.visits.domain.model.ServiceApprovalStatus.PENDING ->
                ServiceApprovalStatus.PENDING
            com.carslab.crm.production.modules.visits.domain.model.ServiceApprovalStatus.APPROVED ->
                ServiceApprovalStatus.APPROVED
            com.carslab.crm.production.modules.visits.domain.model.ServiceApprovalStatus.REJECTED ->
                ServiceApprovalStatus.REJECTED
        }
    }
}