package com.carslab.crm.production.modules.visits.infrastructure.repository.batch

import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitServiceEntity
import com.carslab.crm.production.modules.visits.infrastructure.repository.VisitServiceJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class VisitBatchOperations(
    private val serviceJpaRepository: VisitServiceJpaRepository
) {

    @Transactional
    fun replaceServices(visitId: Long, services: List<VisitService>) {
        deleteExistingServices(visitId)
        insertNewServices(visitId, services)
    }

    private fun deleteExistingServices(visitId: Long) {
        serviceJpaRepository.deleteByVisitId(visitId)
    }

    private fun insertNewServices(visitId: Long, services: List<VisitService>) {
        if (services.isEmpty()) return

        val serviceEntities = services.map { service ->
            VisitServiceEntity.fromDomain(service, visitId)
        }

        serviceJpaRepository.saveAll(serviceEntities)
    }

    @Transactional
    fun bulkUpdateServiceStatus(
        visitId: Long,
        serviceIds: List<String>,
        newStatus: com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
    ) {
        serviceJpaRepository.bulkUpdateApprovalStatus(visitId, serviceIds, newStatus)
    }
}