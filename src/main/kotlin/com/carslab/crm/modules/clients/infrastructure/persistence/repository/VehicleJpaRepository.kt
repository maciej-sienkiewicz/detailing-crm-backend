package com.carslab.crm.modules.clients.infrastructure.persistence.repository

import com.carslab.crm.modules.clients.infrastructure.persistence.entity.VehicleEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface VehicleJpaRepository : JpaRepository<VehicleEntity, Long>, JpaSpecificationExecutor<VehicleEntity> {

    // Basic queries with company isolation
    @Query("SELECT v FROM VehicleEntity v WHERE v.companyId = :companyId AND v.active = true")
    fun findByCompanyId(@Param("companyId") companyId: Long, pageable: Pageable): Page<VehicleEntity>

    @Query("SELECT v FROM VehicleEntity v WHERE v.id = :id AND v.companyId = :companyId AND v.active = true")
    fun findByIdAndCompanyId(@Param("id") id: Long, @Param("companyId") companyId: Long): Optional<VehicleEntity>

    // Simple queries without LOWER function
    @Query("SELECT v FROM VehicleEntity v WHERE v.licensePlate = :licensePlate AND v.companyId = :companyId AND v.active = true")
    fun findByLicensePlateAndCompanyId(@Param("licensePlate") licensePlate: String, @Param("companyId") companyId: Long): Optional<VehicleEntity>

    @Query("SELECT v FROM VehicleEntity v WHERE v.vin = :vin AND v.companyId = :companyId AND v.active = true")
    fun findByVinAndCompanyId(@Param("vin") vin: String, @Param("companyId") companyId: Long): Optional<VehicleEntity>

    @Query("SELECT v FROM VehicleEntity v WHERE (v.vin = :vin OR v.licensePlate = :licensePlate) AND v.companyId = :companyId AND v.active = true")
    fun findByVinOrLicensePlateAndCompanyId(
        @Param("vin") vin: String?,
        @Param("licensePlate") licensePlate: String?,
        @Param("companyId") companyId: Long
    ): Optional<VehicleEntity>

    // FIXED: Native query zamiast JPQL dla wyszukiwania
    @Query(nativeQuery = true, value = """
        SELECT * FROM vehicles v 
        WHERE v.company_id = :companyId AND v.active = true
        AND (:make IS NULL OR LOWER(COALESCE(v.make, '')) LIKE LOWER(CONCAT('%', :make, '%')))
        AND (:model IS NULL OR LOWER(COALESCE(v.model, '')) LIKE LOWER(CONCAT('%', :model, '%')))
        AND (:licensePlate IS NULL OR LOWER(COALESCE(v.license_plate, '')) LIKE LOWER(CONCAT('%', :licensePlate, '%')))
        AND (:vin IS NULL OR LOWER(COALESCE(v.vin, '')) LIKE LOWER(CONCAT('%', :vin, '%')))
        AND (:year IS NULL OR v.year = :year)
        ORDER BY v.created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    fun searchVehiclesNative(
        @Param("make") make: String?,
        @Param("model") model: String?,
        @Param("licensePlate") licensePlate: String?,
        @Param("vin") vin: String?,
        @Param("year") year: Int?,
        @Param("companyId") companyId: Long,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<VehicleEntity>

    // Count query dla paginacji
    @Query(nativeQuery = true, value = """
        SELECT COUNT(*) FROM vehicles v 
        WHERE v.company_id = :companyId AND v.active = true
        AND (:make IS NULL OR LOWER(COALESCE(v.make, '')) LIKE LOWER(CONCAT('%', :make, '%')))
        AND (:model IS NULL OR LOWER(COALESCE(v.model, '')) LIKE LOWER(CONCAT('%', :model, '%')))
        AND (:licensePlate IS NULL OR LOWER(COALESCE(v.license_plate, '')) LIKE LOWER(CONCAT('%', :licensePlate, '%')))
        AND (:vin IS NULL OR LOWER(COALESCE(v.vin, '')) LIKE LOWER(CONCAT('%', :vin, '%')))
        AND (:year IS NULL OR v.year = :year)
    """)
    fun countSearchVehicles(
        @Param("make") make: String?,
        @Param("model") model: String?,
        @Param("licensePlate") licensePlate: String?,
        @Param("vin") vin: String?,
        @Param("year") year: Int?,
        @Param("companyId") companyId: Long
    ): Long

    // Soft delete
    @Modifying
    @Query("UPDATE VehicleEntity v SET v.active = false, v.updatedAt = :now WHERE v.id = :id AND v.companyId = :companyId")
    fun softDeleteByIdAndCompanyId(@Param("id") id: Long, @Param("companyId") companyId: Long, @Param("now") now: LocalDateTime): Int

    // Statistics
    @Query("SELECT COUNT(v) FROM VehicleEntity v WHERE v.companyId = :companyId AND v.active = true")
    fun countByCompanyId(@Param("companyId") companyId: Long): Long

    // Check existence
    fun existsByLicensePlateAndCompanyIdAndActiveTrue(licensePlate: String, companyId: Long): Boolean
    fun existsByVinAndCompanyIdAndActiveTrue(vin: String, companyId: Long): Boolean

    // Find vehicles by client (jeśli masz taką relację)
    @Query("""
        SELECT v FROM VehicleEntity v 
        JOIN ClientVehicleAssociationEntity cva ON v.id = cva.vehicle.id 
        WHERE cva.client.id = :clientId AND cva.companyId = :companyId 
        AND cva.endDate IS NULL AND v.active = true
    """)
    fun findActiveVehiclesByClientId(@Param("clientId") clientId: Long, @Param("companyId") companyId: Long): List<VehicleEntity>
}