package com.carslab.crm.production.modules.reservations.domain.repositories

import com.carslab.crm.production.modules.reservations.domain.models.aggregates.Reservation
import com.carslab.crm.production.modules.reservations.domain.models.enums.ReservationStatus
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ReservationRepository {
    fun save(reservation: Reservation): Reservation
    fun findById(reservationId: ReservationId, companyId: Long): Reservation?
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Reservation>
    fun findByCompanyIdAndStatus(
        companyId: Long,
        status: ReservationStatus,
        pageable: Pageable
    ): Page<Reservation>
    fun countByStatus(companyId: Long, status: ReservationStatus): Long
    fun existsById(reservationId: ReservationId, companyId: Long): Boolean
    fun deleteById(reservationId: ReservationId, companyId: Long): Boolean
}