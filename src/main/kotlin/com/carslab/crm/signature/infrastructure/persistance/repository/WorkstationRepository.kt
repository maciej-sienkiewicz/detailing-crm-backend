package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.infrastructure.persistance.entity.Workstation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WorkstationRepository : JpaRepository<Workstation, Long> {

    // Company-based queries
    fun findByCompanyId(companyId: Long): List<Workstation>

    fun findByCompanyIdAndIsActive(companyId: Long, isActive: Boolean): List<Workstation>

    fun findByCompanyIdAndLocationId(companyId: Long, locationId: Long): List<Workstation>

    // Paginated queries
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Workstation>

    // Tablet assignments
    fun findByPairedTabletId(pairedTabletId: Long): Workstation?

    fun findByCompanyIdAndPairedTabletIdIsNotNull(companyId: Long): List<Workstation>

    fun findByCompanyIdAndPairedTabletIdIsNull(companyId: Long): List<Workstation>

    // Search and filtering
    fun findByCompanyIdAndWorkstationNameContainingIgnoreCase(
        companyId: Long,
        workstationName: String
    ): List<Workstation>

    fun findByCompanyIdAndWorkstationCode(companyId: Long, workstationCode: String): Workstation?

    // Analytics
    @Query("""
        SELECT w.locationId, COUNT(w) 
        FROM Workstation w 
        WHERE w.companyId = :companyId 
        AND w.isActive = true
        GROUP BY w.locationId
    """)
    fun getActiveWorkstationsByLocation(@Param("companyId") companyId: Long): List<Array<Any>>

    @Query("""
        SELECT COUNT(w) 
        FROM Workstation w 
        WHERE w.companyId = :companyId 
        AND w.pairedTabletId IS NOT NULL 
        AND w.isActive = true
    """)
    fun countPairedActiveWorkstations(@Param("companyId") companyId: Long): Long
}