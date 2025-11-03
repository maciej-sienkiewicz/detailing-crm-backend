package com.carslab.crm.production.modules.reservations.infrastructure.repository

import com.carslab.crm.production.modules.reservations.domain.models.aggregates.Reservation
import com.carslab.crm.production.modules.reservations.domain.models.entities.ReservationService
import com.carslab.crm.production.modules.reservations.domain.models.enums.ReservationStatus
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationId
import com.carslab.crm.production.modules.reservations.domain.repositories.ReservationRepository
import com.carslab.crm.production.modules.reservations.infrastructure.entity.ReservationEntity
import com.carslab.crm.production.modules.reservations.infrastructure.entity.ReservationServiceEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaReservationRepositoryImpl(
    private val jpaRepository: ReservationJpaRepository,
    private val serviceJpaRepository: ReservationServiceJpaRepository
) : ReservationRepository {

    override fun save(reservation: Reservation): Reservation {
        val entity = ReservationEntity.fromDomain(reservation)
        val savedEntity = jpaRepository.save(entity)

        val reservationId = savedEntity.id!!
        replaceServices(reservationId, reservation.services)

        return findByIdWithServices(reservationId)!!
    }

    @Transactional(readOnly = true)
    override fun findById(reservationId: ReservationId, companyId: Long): Reservation? {
        return findByIdWithServices(reservationId.value, companyId)
    }

    @Transactional(readOnly = true)
    override fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Reservation> {
        return jpaRepository.findByCompanyId(companyId, pageable)
            .map { findByIdWithServices(it.id!!)!! }
    }

    @Transactional(readOnly = true)
    override fun findByCompanyIdAndStatus(
        companyId: Long,
        status: ReservationStatus,
        pageable: Pageable
    ): Page<Reservation> {
        return jpaRepository.findByCompanyIdAndStatus(companyId, status, pageable)
            .map { findByIdWithServices(it.id!!)!! }
    }

    @Transactional(readOnly = true)
    override fun countByStatus(companyId: Long, status: ReservationStatus): Long {
        return jpaRepository.countByCompanyIdAndStatus(companyId, status)
    }

    @Transactional(readOnly = true)
    override fun existsById(reservationId: ReservationId, companyId: Long): Boolean {
        return jpaRepository.existsByIdAndCompanyId(reservationId.value, companyId)
    }

    override fun deleteById(reservationId: ReservationId, companyId: Long): Boolean {
        val entity = jpaRepository.findByIdAndCompanyId(reservationId.value, companyId)
            ?: return false
        jpaRepository.delete(entity)
        return true
    }

    private fun replaceServices(reservationId: Long, services: List<ReservationService>) {
        serviceJpaRepository.deleteByReservationId(reservationId)

        if (services.isNotEmpty()) {
            val serviceEntities = services.map { service ->
                ReservationServiceEntity.fromDomain(service, reservationId)
            }
            serviceJpaRepository.saveAll(serviceEntities)
        }
    }

    private fun findByIdWithServices(reservationId: Long, companyId: Long? = null): Reservation? {
        val entity: ReservationEntity = if (companyId != null) {
            jpaRepository.findByIdAndCompanyId(reservationId, companyId)
        } else {
            jpaRepository.findById(reservationId).orElse(null)
        } ?: return null

        val services = serviceJpaRepository.findByReservationId(reservationId)

        return entity.copy(services = services.toMutableList()).toDomain()
    }
}