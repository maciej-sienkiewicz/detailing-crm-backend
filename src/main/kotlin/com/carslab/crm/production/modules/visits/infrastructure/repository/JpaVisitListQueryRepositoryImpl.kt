package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitListQueryRepository
import com.carslab.crm.production.modules.visits.domain.repositories.VisitListRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Repository
@Transactional(readOnly = true)
class JpaVisitListQueryRepositoryImpl(
    private val visitListRepository: VisitListRepository
) : VisitListQueryRepository {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun findVisitList(companyId: Long, pageable: Pageable): PaginatedResponse<VisitListReadModel> {
        val visitListProjections = visitListRepository.findVisitListProjectionsForCompany(companyId, pageable)
        val visitIds = visitListProjections.content.map { VisitId.of(it.visitId) }
        val servicesMap = visitListRepository.findVisitServicesForVisits(companyId, visitIds)

        val visitListReadModels = visitListProjections.content.map { projection ->
            val visitId = VisitId.of(projection.visitId)
            val services = servicesMap[visitId] ?: emptyList()

            VisitListReadModel(
                id = projection.visitId.toString(),
                title = projection.title,
                vehicle = VehicleBasicReadModel(
                    make = projection.vehicleMake,
                    model = projection.vehicleModel,
                    licensePlate = projection.licensePlate,
                    productionYear = projection.productionYear ?: 0,
                    color = projection.color
                ),
                client = ClientBasicReadModel(
                    name = projection.clientName,
                    companyName = projection.companyName
                ),
                period = PeriodReadModel(
                    startDate = projection.startDate.format(dateTimeFormatter),
                    endDate = projection.endDate.format(dateTimeFormatter)
                ),
                status = mapToStatusString(projection.status),
                calendarColorId = projection.calendarColorId,
                totalServiceCount = projection.totalServiceCount,
                totalAmount = projection.totalAmount,
                services = services.map { service ->
                    VisitServiceReadModel(
                        id = service.id,
                        name = service.name,
                        finalPrice = service.finalPrice
                    )
                },
                lastUpdate = projection.lastUpdate.format(dateTimeFormatter)
            )
        }

        return PaginatedResponse(
            data = visitListReadModels,
            page = visitListProjections.number,
            size = visitListProjections.size,
            totalItems = visitListProjections.totalElements,
            totalPages = visitListProjections.totalPages.toLong()
        )
    }

    private fun mapToStatusString(status: com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus): String {
        return when (status) {
            com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.SCHEDULED -> "SCHEDULED"
            com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.PENDING_APPROVAL -> "PENDING_APPROVAL"
            com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.IN_PROGRESS -> "IN_PROGRESS"
            com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.READY_FOR_PICKUP -> "READY_FOR_PICKUP"
            com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.COMPLETED -> "COMPLETED"
            com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.CANCELLED -> "CANCELLED"
        }
    }
}