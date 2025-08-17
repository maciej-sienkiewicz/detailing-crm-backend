package com.carslab.crm.production.modules.visits.application.service

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.production.modules.visits.domain.repositories.VisitListRepository
import com.carslab.crm.production.modules.visits.domain.models.aggregates.VisitListItem
import com.carslab.crm.production.modules.visits.domain.models.aggregates.VisitListService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class VisitListQueryService(
    private val visitListRepository: VisitListRepository,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitListQueryService::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun getVisitList(pageable: Pageable): PaginatedResponse<VisitListReadModel> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visit list for company: {}", companyId)

        val visitListItems = visitListRepository.findVisitListForCompany(companyId, pageable)
        val visitIds = visitListItems.content.map { it.visitId }
        val servicesMap = visitListRepository.findVisitServicesForVisits(companyId, visitIds)

        logger.debug("Found {} visits for company: {}", visitListItems.numberOfElements, companyId)

        val visitListReadModels = visitListItems.content.map { visitItem ->
            mapToVisitListReadModel(visitItem, servicesMap[visitItem.visitId] ?: emptyList())
        }

        return PaginatedResponse(
            data = visitListReadModels,
            page = visitListItems.number,
            size = visitListItems.size,
            totalItems = visitListItems.totalElements,
            totalPages = visitListItems.totalPages.toLong()
        )
    }

    private fun mapToVisitListReadModel(
        visitItem: VisitListItem,
        services: List<VisitListService>
    ): VisitListReadModel {
        return VisitListReadModel(
            id = visitItem.visitId.toString(),
            title = visitItem.title,
            vehicle = VehicleBasicReadModel(
                make = visitItem.vehicleMake,
                model = visitItem.vehicleModel,
                licensePlate = visitItem.licensePlate,
                productionYear = visitItem.productionYear ?: 0,
                color = visitItem.color
            ),
            client = ClientBasicReadModel(
                name = visitItem.clientName,
                companyName = visitItem.companyName
            ),
            period = PeriodReadModel(
                startDate = visitItem.startDate.format(dateTimeFormatter),
                endDate = visitItem.endDate.format(dateTimeFormatter)
            ),
            status = mapToStatusString(visitItem.status),
            calendarColorId = visitItem.calendarColorId,
            totalServiceCount = visitItem.totalServiceCount,
            totalAmount = visitItem.totalAmount,
            services = services.map { service ->
                VisitServiceReadModel(
                    id = service.id,
                    name = service.name,
                    finalPrice = service.finalPrice
                )
            },
            lastUpdate = visitItem.lastUpdate.format(dateTimeFormatter)
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