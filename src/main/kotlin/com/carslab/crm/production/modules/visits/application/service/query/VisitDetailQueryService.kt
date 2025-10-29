package com.carslab.crm.production.modules.visits.application.service.query

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.api.commands.*
import com.carslab.crm.production.modules.visits.application.queries.models.VisitDetailReadModel
import com.carslab.crm.production.modules.visits.domain.command.DeliveryPerson
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitMedia
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitDetailQueryRepository
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
import com.carslab.crm.production.modules.visits.domain.service.details.MediaService
import com.carslab.crm.production.modules.visits.infrastructure.mapper.EnumMappers
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class VisitDetailQueryService(
    private val visitDetailQueryRepository: VisitDetailQueryRepository,
    private val mediaService: MediaService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitDetailQueryService::class.java)

    fun getVisitDetail(visitId: String, companyId: Long? = null): CarReceptionDetailDto {
        val companyId = companyId ?: securityContext.getCurrentCompanyId()
        logger.debug("Fetching visit detail: {} for company: {}", visitId, companyId)

        val visitDetail = visitDetailQueryRepository.findVisitDetailById(VisitId.of(visitId), companyId)
            ?: throw EntityNotFoundException("Visit not found: $visitId")

        val mediaItems = mediaService.getMediaForVisit(VisitId.of(visitId))

        logger.debug("Visit detail found: {}", visitDetail.title)

        return mapToCarReceptionDetailDto(visitDetail, mediaItems)
    }

    fun getSimpleDetails(visitId: String, authContext: AuthContext? = null): VisitDetailReadModel {
        val companyId = authContext?.companyId?.value ?: securityContext.getCurrentCompanyId()
        logger.debug("Fetching simple visit details: {} for company: {}", visitId, companyId)

        val visitDetail = visitDetailQueryRepository.findVisitDetailById(VisitId.of(visitId), companyId)
            ?: throw EntityNotFoundException("Visit not found: $visitId")

        logger.debug("Simple visit detail found: {}", visitDetail.title)

        return visitDetail
    }

    private fun mapToCarReceptionDetailDto(
        visitDetail: VisitDetailReadModel,
        mediaItems: List<VisitMedia>
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
                    price = service.basePriceBrutto,
                    discountType = service.discountType?.let { EnumMappers.mapToApiDiscountType(EnumMappers.mapToDiscountType(it)!!) },
                    discountValue = service.discountValue,
                    finalPrice = service.finalPriceBrutto,
                    approvalStatus = EnumMappers.mapToApiServiceApprovalStatus(EnumMappers.mapToServiceApprovalStatus(service.approvalStatus)),
                    note = service.note
                )
            },
            status = EnumMappers.mapToApiProtocolStatus(EnumMappers.mapToVisitStatus(visitDetail.status)),
            referralSource = visitDetail.referralSource?.let {
                EnumMappers.mapToApiReferralSource(EnumMappers.mapToReferralSource(it)!!)
            },
            otherSourceDetails = visitDetail.otherSourceDetails,
            vehicleImages = mediaItems.map { media ->
                VehicleImageDto(
                    id = media.id,
                    name = media.name,
                    size = media.size,
                    type = media.contentType,
                    storageId = media.id,
                    createdAt = Instant.from(media.createdAt.atZone(ZoneId.systemDefault())),
                    description = media.description,
                    location = media.location,
                    tags = media.tags
                )
            },
            createdAt = visitDetail.audit.createdAt,
            updatedAt = visitDetail.audit.updatedAt,
            statusUpdatedAt = visitDetail.audit.statusUpdatedAt,
            appointmentId = visitDetail.appointmentId,
            deliveryPerson = visitDetail.deliveryPerson?.id?.let { 
                DeliveryPerson(
                    id = visitDetail.deliveryPerson.id,
                    name = visitDetail.deliveryPerson.name,
                    phone = visitDetail.deliveryPerson.phone,
                )
            }
        )
    }
}