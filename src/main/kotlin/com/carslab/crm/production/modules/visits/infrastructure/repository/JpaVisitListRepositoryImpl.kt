package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.visits.application.queries.models.VisitListProjection
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitListService
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitListRepository
import com.carslab.crm.production.shared.observability.annotations.DatabaseMonitored
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class JpaVisitListRepositoryImpl(
    private val visitListJpaRepository: VisitListJpaRepository
) : VisitListRepository {

    @DatabaseMonitored(repository = "visit_list", method = "findVisitListProjectionsForCompany", operation = "select")
    override fun findVisitListProjectionsForCompany(companyId: Long, pageable: Pageable): Page<VisitListProjection> {
        return visitListJpaRepository.findVisitListForCompany(companyId, pageable)
    }

    @DatabaseMonitored(repository = "visit_list", method = "findVisitServicesForVisits", operation = "select")
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