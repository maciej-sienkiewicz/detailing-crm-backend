package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitEntity
import com.carslab.crm.production.modules.visits.infrastructure.repository.batch.VisitBatchOperations
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaVisitRepositoryImpl(
    private val visitJpaRepository: VisitJpaRepository,
    private val batchOperations: VisitBatchOperations
) : VisitRepository {

    override fun save(visit: Visit): Visit {
        val entity = VisitEntity.fromDomain(visit)
        val savedEntity = visitJpaRepository.save(entity)

        val visitId = savedEntity.id!!
        val services = batchOperations.replaceServices(visitId, visit.services)

        return savedEntity.toDomain(services)
    }

    @Transactional(readOnly = true)
    override fun findById(visitId: VisitId, companyId: Long, authContext: AuthContext?): Visit? {
        return visitJpaRepository.findByIdAndCompanyIdWithServices(visitId.value, authContext?.companyId?.value ?: companyId)
            ?.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Visit> {
        return visitJpaRepository.findByCompanyIdWithServices(companyId, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun existsById(visitId: VisitId, companyId: Long): Boolean {
        return visitJpaRepository.existsByIdAndCompanyId(visitId.value, companyId)
    }

    override fun deleteById(visitId: VisitId, companyId: Long): Boolean {
        return visitJpaRepository.deleteByIdAndCompanyId(visitId.value, companyId) > 0
    }

    @Transactional(readOnly = true)
    override fun countByStatus(companyId: Long, status: VisitStatus): Long {
        return visitJpaRepository.countByCompanyIdAndStatus(companyId, status)
    }

    @Transactional(readOnly = true)
    override fun findByClientId(clientId: ClientId, companyId: Long, pageable: Pageable): Page<Visit> {
        return visitJpaRepository.findByClientIdAndCompanyIdWithServices(clientId.value, companyId, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByVehicleId(vehicleId: VehicleId, companyId: Long, pageable: Pageable): Page<Visit> {
        return visitJpaRepository.findByVehicleIdAndCompanyIdWithServices(vehicleId.value, companyId, pageable)
            .map { it.toDomain() }
    }
}