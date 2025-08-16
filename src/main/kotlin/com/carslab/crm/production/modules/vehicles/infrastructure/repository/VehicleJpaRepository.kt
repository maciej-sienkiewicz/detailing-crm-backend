package com.carslab.crm.production.modules.vehicles.infrastructure.repository

import com.carslab.crm.production.modules.vehicles.infrastructure.entity.VehicleEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface VehicleJpaRepository : JpaRepository<VehicleEntity, Long> {

    @Query("SELECT v FROM VehicleEntity v WHERE v.companyId = :companyId AND v.active = true ORDER BY v.createdAt DESC")
    fun findByCompanyIdAndActiveTrue(@Param("companyId") companyId: Long, pageable: Pageable): Page<VehicleEntity>

    @Query("SELECT v FROM VehicleEntity v WHERE v.licensePlate = :licensePlate AND v.companyId = :companyId AND v.active = true")
    fun findByLicensePlateAndCompanyIdAndActiveTrue(@Param("licensePlate") licensePlate: String, @Param("companyId") companyId: Long): Optional<VehicleEntity>

    @Query("SELECT v FROM VehicleEntity v WHERE v.vin = :vin AND v.companyId = :companyId AND v.active = true")
    fun findByVinAndCompanyIdAndActiveTrue(@Param("vin") vin: String, @Param("companyId") companyId: Long): Optional<VehicleEntity>

    fun existsByLicensePlateAndCompanyIdAndActiveTrue(licensePlate: String, companyId: Long): Boolean
    fun existsByVinAndCompanyIdAndActiveTrue(vin: String, companyId: Long): Boolean
    fun existsByIdAndCompanyIdAndActiveTrue(id: Long, companyId: Long): Boolean

    @Modifying
    @Query("UPDATE VehicleEntity v SET v.active = false, v.updatedAt = :now WHERE v.id = :id AND v.companyId = :companyId")
    fun softDeleteByIdAndCompanyId(@Param("id") id: Long, @Param("companyId") companyId: Long, @Param("now") now: LocalDateTime): Int

    @Query(nativeQuery = true, value = """
        SELECT DISTINCT v.* FROM vehicles v 
        LEFT JOIN client_vehicle_associations cva ON v.id = cva.vehicle_id AND cva.end_date IS NULL
        LEFT JOIN clients c ON cva.client_id = c.id AND c.active = true
        LEFT JOIN vehicle_statistics vs ON v.id = vs.vehicle_id
        WHERE v.company_id = :companyId AND v.active = true
        AND (:make IS NULL OR LOWER(v.make) LIKE LOWER(CONCAT('%', :make, '%')))
        AND (:model IS NULL OR LOWER(v.model) LIKE LOWER(CONCAT('%', :model, '%')))
        AND (:licensePlate IS NULL OR LOWER(v.license_plate) LIKE LOWER(CONCAT('%', :licensePlate, '%')))
        AND (:vin IS NULL OR LOWER(v.vin) LIKE LOWER(CONCAT('%', :vin, '%')))
        AND (:year IS NULL OR v.year = :year)
        AND (:ownerName IS NULL OR LOWER(CONCAT(c.first_name, ' ', c.last_name)) LIKE LOWER(CONCAT('%', :ownerName, '%')))
        AND (:minVisits IS NULL OR COALESCE(vs.visit_count, 0) >= :minVisits)
        AND (:maxVisits IS NULL OR COALESCE(vs.visit_count, 0) <= :maxVisits)
        ORDER BY v.created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    fun searchVehicles(
        @Param("companyId") companyId: Long,
        @Param("make") make: String?,
        @Param("model") model: String?,
        @Param("licensePlate") licensePlate: String?,
        @Param("vin") vin: String?,
        @Param("year") year: Int?,
        @Param("ownerName") ownerName: String?,
        @Param("minVisits") minVisits: Int?,
        @Param("maxVisits") maxVisits: Int?,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<VehicleEntity>

    @Query(nativeQuery = true, value = """
        SELECT COUNT(DISTINCT v.id) FROM vehicles v 
        LEFT JOIN client_vehicle_associations cva ON v.id = cva.vehicle_id AND cva.end_date IS NULL
        LEFT JOIN clients c ON cva.client_id = c.id AND c.active = true
        LEFT JOIN vehicle_statistics vs ON v.id = vs.vehicle_id
        WHERE v.company_id = :companyId AND v.active = true
        AND (:make IS NULL OR LOWER(v.make) LIKE LOWER(CONCAT('%', :make, '%')))
        AND (:model IS NULL OR LOWER(v.model) LIKE LOWER(CONCAT('%', :model, '%')))
        AND (:licensePlate IS NULL OR LOWER(v.license_plate) LIKE LOWER(CONCAT('%', :licensePlate, '%')))
        AND (:vin IS NULL OR LOWER(v.vin) LIKE LOWER(CONCAT('%', :vin, '%')))
        AND (:year IS NULL OR v.year = :year)
        AND (:ownerName IS NULL OR LOWER(CONCAT(c.first_name, ' ', c.last_name)) LIKE LOWER(CONCAT('%', :ownerName, '%')))
        AND (:minVisits IS NULL OR COALESCE(vs.visit_count, 0) >= :minVisits)
        AND (:maxVisits IS NULL OR COALESCE(vs.visit_count, 0) <= :maxVisits)
    """)
    fun countSearchVehicles(
        @Param("companyId") companyId: Long,
        @Param("make") make: String?,
        @Param("model") model: String?,
        @Param("licensePlate") licensePlate: String?,
        @Param("vin") vin: String?,
        @Param("year") year: Int?,
        @Param("ownerName") ownerName: String?,
        @Param("minVisits") minVisits: Int?,
        @Param("maxVisits") maxVisits: Int?
    ): Long
}