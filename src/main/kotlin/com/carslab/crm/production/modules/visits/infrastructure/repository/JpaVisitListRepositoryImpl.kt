package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.visits.domain.models.aggregates.VisitListItem
import com.carslab.crm.production.modules.visits.domain.models.aggregates.VisitListService
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitListRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class JpaVisitListRepositoryImpl(
    private val visitListJpaRepository: VisitListJpaRepository
) : VisitListRepository {

    override fun findVisitListForCompany(companyId: Long, pageable: Pageable): Page<VisitListItem> {
        val projections = visitListJpaRepository.findVisitListForCompany(companyId, pageable)

        return projections.map { projection ->
            VisitListItem(
                visitId = VisitId.of(projection.visitId),
                title = projection.title,
                clientName = projection.clientName,
                companyName = projection.companyName,
                vehicleMake = projection.vehicleMake,
                vehicleModel = projection.vehicleModel,
                licensePlate = projection.licensePlate,
                productionYear = projection.productionYear,
                color = projection.color,
                startDate = projection.startDate,
                endDate = projection.endDate,
                status = projection.status,
                totalServiceCount = projection.totalServiceCount,
                totalAmount = projection.totalAmount,
                calendarColorId = projection.calendarColorId,
                lastUpdate = projection.lastUpdate
            )
        }
    }

    override fun findVisitServicesForVisits(companyId: Long, visitIds: List<VisitId>): Map<VisitId, List<VisitListService>> {
        val visitIdValues = visitIds.map { it.value }
        val projections = visitListJpaRepository.findVisitServicesForVisits(companyId, visitIdValues)

        return projections.groupBy { VisitId.of(it.visitId) }
            .mapValues { (_, services) ->
                services.map { service ->
                    VisitListService(
                        id = service.serviceId,
                        name = service.serviceName,
                        finalPrice = service.finalPrice
                    )
                }
            }
    }
}