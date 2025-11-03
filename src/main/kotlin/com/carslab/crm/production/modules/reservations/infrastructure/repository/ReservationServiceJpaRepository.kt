package com.carslab.crm.production.modules.reservations.infrastructure.repository

import com.carslab.crm.production.modules.reservations.infrastructure.entity.ReservationServiceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReservationServiceJpaRepository : JpaRepository<ReservationServiceEntity, Long> {
    fun findByReservationId(reservationId: Long): List<ReservationServiceEntity>
    fun deleteByReservationId(reservationId: Long)
}