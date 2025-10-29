package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.visits.application.queries.models.*
import com.carslab.crm.production.modules.visits.domain.command.DeliveryPerson
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitDetailQueryRepository
import com.carslab.crm.production.modules.visits.domain.repositories.VisitDetailProjection
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Repository
@Transactional(readOnly = true)
class JpaVisitDetailQueryRepositoryImpl(
    private val visitDetailJpaRepository: VisitDetailJpaRepository,
    private val visitServiceJpaRepository: VisitServiceJpaRepository
) : VisitDetailQueryRepository {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun findVisitDetailById(visitId: VisitId, companyId: Long): VisitDetailReadModel? {
        val projection = visitDetailJpaRepository.findVisitDetailWithRelations(visitId.value, companyId)
            ?: return null

        val services = visitServiceJpaRepository.findByVisitId(visitId.value)

        return VisitDetailReadModel(
            id = projection.visitId.toString(),
            title = projection.title,
            calendarColorId = projection.calendarColorId,
            vehicle = VehicleDetailReadModel(
                id = projection.vehicleId.toString(),
                make = projection.vehicleMake,
                model = projection.vehicleModel,
                licensePlate = projection.vehicleLicensePlate,
                productionYear = projection.vehicleYear ?: 0,
                vin = projection.vehicleVin,
                color = projection.vehicleColor,
                mileage = projection.vehicleMileage
            ),
            client = ClientDetailReadModel(
                id = projection.clientId.toString(),
                name = projection.clientName,
                email = projection.clientEmail,
                address = projection.clientAddress,
                phone = projection.clientPhone,
                companyName = projection.clientCompany,
                taxId = projection.clientTaxId
            ),
            period = PeriodReadModel(
                startDate = projection.startDate.format(dateTimeFormatter),
                endDate = projection.endDate.format(dateTimeFormatter)
            ),
            status = projection.status,
            services = services.map { service ->
                val domainService = service.toDomain()
                val finalPrice = domainService.calculateFinalPrice()
                ServiceDetailReadModel(
                    id = service.serviceId?.toString() ?: "",
                    name = service.name,
                    baseTaxAmount = service.baseTaxAmount,
                    basePriceNetto = service.basePriceNetto,
                    basePriceBrutto = service.basePriceBrutto,
                    finalPriceNetto = finalPrice.priceNetto,
                    finalPriceBrutto = finalPrice.priceBrutto,
                    finalTaxAmount = finalPrice.taxAmount,
                    quantity = service.quantity,
                    discountType = service.discountType?.name,
                    discountValue = service.discountValue ?: java.math.BigDecimal.ZERO,
                    approvalStatus = service.approvalStatus.name,
                    note = service.note
                )
            },
            notes = projection.notes,
            referralSource = projection.referralSource,
            otherSourceDetails = null,
            documents = DocumentsReadModel(
                keysProvided = projection.keysProvided,
                documentsProvided = projection.documentsProvided
            ),
            mediaItems = emptyList(),
            audit = AuditReadModel(
                createdAt = projection.createdAt.format(dateTimeFormatter),
                updatedAt = projection.updatedAt.format(dateTimeFormatter),
                statusUpdatedAt = projection.updatedAt.format(dateTimeFormatter)
            ),
            appointmentId = projection.appointmentId,
            deliveryPerson = projection.deliveryPersonId?.let {
                DeliveryPerson(
                    id = projection.deliveryPersonId,
                    name = projection.deliveryPersonName!!,
                    phone = projection.deliveryPersonPhoneNumber!!
                )
            }
        )
    }
}