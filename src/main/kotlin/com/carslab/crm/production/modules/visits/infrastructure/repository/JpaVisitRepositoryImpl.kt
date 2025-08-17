package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.model.*
import com.carslab.crm.production.modules.visits.domain.repository.VisitRepository
import com.carslab.crm.production.modules.visits.domain.repository.VisitSearchCriteria
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitEntity
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitServiceEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.criteria.Predicate

@Repository
@Transactional
class JpaVisitRepositoryImpl(
    private val visitJpaRepository: VisitJpaRepository,
    private val serviceJpaRepository: VisitServiceJpaRepository
) : VisitRepository {

    override fun save(visit: Visit): Visit {
        val entity = VisitEntity.Companion.fromDomain(visit)
        val savedEntity = visitJpaRepository.save(entity)

        val visitId = savedEntity.id!!

        serviceJpaRepository.deleteByVisitId(visitId)

        val serviceEntities = visit.services.map { service ->
            VisitServiceEntity.Companion.fromDomain(service, visitId)
        }
        serviceJpaRepository.saveAll(serviceEntities)

        return savedEntity.toDomain()
    }

    override fun findById(visitId: VisitId, companyId: Long): Visit? {
        return visitJpaRepository.findByIdAndCompanyId(visitId.value, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Visit> {
        return visitJpaRepository.findByCompanyId(companyId, pageable)
            .map { it.toDomain() }
    }

    override fun existsById(visitId: VisitId, companyId: Long): Boolean {
        return visitJpaRepository.existsByIdAndCompanyId(visitId.value, companyId)
    }

    override fun deleteById(visitId: VisitId, companyId: Long): Boolean {
        return visitJpaRepository.deleteByIdAndCompanyId(visitId.value, companyId) > 0
    }

    override fun searchVisits(companyId: Long, criteria: VisitSearchCriteria, pageable: Pageable): Page<Visit> {
        val specification = buildSearchSpecification(companyId, criteria)
        return visitJpaRepository.findAll(specification, pageable)
            .map { it.toDomain() }
    }

    override fun countByStatus(companyId: Long, status: VisitStatus): Long {
        return visitJpaRepository.countByCompanyIdAndStatus(companyId, status)
    }

    override fun findByClientId(clientId: ClientId, companyId: Long, pageable: Pageable): Page<Visit> {
        return visitJpaRepository.findByClientIdAndCompanyId(clientId.value, companyId, pageable)
            .map { it.toDomain() }
    }

    override fun findByVehicleId(vehicleId: VehicleId, companyId: Long, pageable: Pageable): Page<Visit> {
        return visitJpaRepository.findByVehicleIdAndCompanyId(vehicleId.value, companyId, pageable)
            .map { it.toDomain() }
    }

    private fun buildSearchSpecification(companyId: Long, criteria: VisitSearchCriteria): Specification<VisitEntity> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            predicates.add(cb.equal(root.get<Long>("companyId"), companyId))

            criteria.status?.let { status ->
                predicates.add(cb.equal(root.get<VisitStatus>("status"), status))
            }

            criteria.startDate?.let { startDate ->
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate))
            }

            criteria.endDate?.let { endDate ->
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), endDate))
            }

            criteria.title?.let { title ->
                predicates.add(cb.like(cb.lower(root.get("title")), "%${title.lowercase()}%"))
            }

            cb.and(*predicates.toTypedArray())
        }
    }
}