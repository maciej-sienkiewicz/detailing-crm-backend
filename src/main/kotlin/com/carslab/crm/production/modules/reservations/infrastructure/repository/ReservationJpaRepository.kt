package com.carslab.crm.production.modules.reservations.infrastructure.repository

import com.carslab.crm.production.modules.reservations.domain.models.enums.ReservationStatus
import com.carslab.crm.production.modules.reservations.infrastructure.entity.ReservationEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ReservationJpaRepository : JpaRepository<ReservationEntity, Long> {

    fun findByIdAndCompanyId(id: Long, companyId: Long): ReservationEntity?

    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<ReservationEntity>

    fun findByCompanyIdAndStatus(
        companyId: Long,
        status: ReservationStatus,
        pageable: Pageable
    ): Page<ReservationEntity>

    @Query("""
        SELECT r FROM ReservationEntity r 
        WHERE r.companyId = :companyId 
        AND r.status IN :statuses
        ORDER BY r.startDate ASC
    """)
    fun findByCompanyIdAndStatusIn(
        @Param("companyId") companyId: Long,
        @Param("statuses") statuses: List<ReservationStatus>,
        pageable: Pageable
    ): Page<ReservationEntity>

    fun countByCompanyIdAndStatus(companyId: Long, status: ReservationStatus): Long

    fun existsByIdAndCompanyId(id: Long, companyId: Long): Boolean

    @Query("""
        SELECT r FROM ReservationEntity r 
        WHERE r.companyId = :companyId 
        AND LOWER(r.contactPhone) LIKE LOWER(CONCAT('%', :phone, '%'))
    """)
    fun findByContactPhoneContaining(
        @Param("companyId") companyId: Long,
        @Param("phone") phone: String,
        pageable: Pageable
    ): Page<ReservationEntity>
}