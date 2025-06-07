package com.carslab.crm.clients.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface VehicleTableJpaRepository : JpaRepository<com.carslab.crm.clients.infrastructure.persistence.entity.VehicleEntity, Long> {

    /**
     * Native query to find vehicles for table with proper dynamic sorting
     * Note: We use a workaround for dynamic ORDER BY in native queries
     */
    @Query(nativeQuery = true, value = """
        SELECT DISTINCT
            v.id,
            v.make,
            v.model,
            v.year,
            v.license_plate,
            v.color,
            v.vin,
            v.mileage,
            COALESCE(vs.visit_count, 0) as visit_count,
            vs.last_visit_date,
            COALESCE(vs.total_revenue, 0) as total_revenue,
            v.created_at,
            v.updated_at
        FROM vehicles v
        LEFT JOIN vehicle_statistics vs ON v.id = vs.vehicle_id
        LEFT JOIN client_vehicle_associations cva ON v.id = cva.vehicle_id AND cva.end_date IS NULL
        LEFT JOIN clients c ON cva.client_id = c.id
        WHERE v.company_id = :companyId 
        AND v.active = true
        AND (:make IS NULL OR LOWER(v.make) LIKE LOWER(CONCAT('%', :make, '%')))
        AND (:model IS NULL OR LOWER(v.model) LIKE LOWER(CONCAT('%', :model, '%')))
        AND (:licensePlate IS NULL OR LOWER(v.license_plate) LIKE LOWER(CONCAT('%', :licensePlate, '%')))
        AND (:ownerName IS NULL OR 
             LOWER(CONCAT(c.first_name, ' ', c.last_name)) LIKE LOWER(CONCAT('%', :ownerName, '%')))
        AND (:minVisits IS NULL OR COALESCE(vs.visit_count, 0) >= :minVisits)
        AND (:maxVisits IS NULL OR COALESCE(vs.visit_count, 0) <= :maxVisits)
        ORDER BY 
            CASE WHEN :sortField = 'make' AND :sortDirection = 'ASC' THEN v.make END ASC,
            CASE WHEN :sortField = 'make' AND :sortDirection = 'DESC' THEN v.make END DESC,
            CASE WHEN :sortField = 'model' AND :sortDirection = 'ASC' THEN v.model END ASC,
            CASE WHEN :sortField = 'model' AND :sortDirection = 'DESC' THEN v.model END DESC,
            CASE WHEN :sortField = 'year' AND :sortDirection = 'ASC' THEN v.year END ASC,
            CASE WHEN :sortField = 'year' AND :sortDirection = 'DESC' THEN v.year END DESC,
            CASE WHEN :sortField = 'licensePlate' AND :sortDirection = 'ASC' THEN v.license_plate END ASC,
            CASE WHEN :sortField = 'licensePlate' AND :sortDirection = 'DESC' THEN v.license_plate END DESC,
            CASE WHEN :sortField = 'visitCount' AND :sortDirection = 'ASC' THEN COALESCE(vs.visit_count, 0) END ASC,
            CASE WHEN :sortField = 'visitCount' AND :sortDirection = 'DESC' THEN COALESCE(vs.visit_count, 0) END DESC,
            CASE WHEN :sortField = 'lastVisitDate' AND :sortDirection = 'ASC' THEN vs.last_visit_date END ASC,
            CASE WHEN :sortField = 'lastVisitDate' AND :sortDirection = 'DESC' THEN vs.last_visit_date END DESC,
            CASE WHEN :sortField = 'totalRevenue' AND :sortDirection = 'ASC' THEN COALESCE(vs.total_revenue, 0) END ASC,
            CASE WHEN :sortField = 'totalRevenue' AND :sortDirection = 'DESC' THEN COALESCE(vs.total_revenue, 0) END DESC,
            CASE WHEN :sortField = 'createdAt' AND :sortDirection = 'ASC' THEN v.created_at END ASC,
            CASE WHEN :sortField = 'createdAt' AND :sortDirection = 'DESC' THEN v.created_at END DESC,
            v.created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    fun findVehiclesForTableNative(
        @Param("make") make: String?,
        @Param("model") model: String?,
        @Param("licensePlate") licensePlate: String?,
        @Param("ownerName") ownerName: String?,
        @Param("minVisits") minVisits: Long?,
        @Param("maxVisits") maxVisits: Long?,
        @Param("companyId") companyId: Long,
        @Param("sortField") sortField: String,
        @Param("sortDirection") sortDirection: String,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<Array<Any>>

    @Query(nativeQuery = true, value = """
        SELECT COUNT(DISTINCT v.id)
        FROM vehicles v
        LEFT JOIN vehicle_statistics vs ON v.id = vs.vehicle_id
        LEFT JOIN client_vehicle_associations cva ON v.id = cva.vehicle_id AND cva.end_date IS NULL
        LEFT JOIN clients c ON cva.client_id = c.id
        WHERE v.company_id = :companyId 
        AND v.active = true
        AND (:make IS NULL OR LOWER(v.make) LIKE LOWER(CONCAT('%', :make, '%')))
        AND (:model IS NULL OR LOWER(v.model) LIKE LOWER(CONCAT('%', :model, '%')))
        AND (:licensePlate IS NULL OR LOWER(v.license_plate) LIKE LOWER(CONCAT('%', :licensePlate, '%')))
        AND (:ownerName IS NULL OR 
             LOWER(CONCAT(c.first_name, ' ', c.last_name)) LIKE LOWER(CONCAT('%', :ownerName, '%')))
        AND (:minVisits IS NULL OR COALESCE(vs.visit_count, 0) >= :minVisits)
        AND (:maxVisits IS NULL OR COALESCE(vs.visit_count, 0) <= :maxVisits)
    """)
    fun countVehiclesForTableNative(
        @Param("make") make: String?,
        @Param("model") model: String?,
        @Param("licensePlate") licensePlate: String?,
        @Param("ownerName") ownerName: String?,
        @Param("minVisits") minVisits: Long?,
        @Param("maxVisits") maxVisits: Long?,
        @Param("companyId") companyId: Long
    ): Long

    @Query(nativeQuery = true, value = """
        SELECT 
            c.id,
            c.first_name,
            c.last_name,
            c.email,
            c.phone
        FROM clients c
        INNER JOIN client_vehicle_associations cva ON c.id = cva.client_id
        WHERE cva.vehicle_id = :vehicleId 
        AND cva.company_id = :companyId
        AND cva.end_date IS NULL
        AND c.active = true
        ORDER BY c.first_name, c.last_name
    """)
    fun findVehicleOwnersNative(
        @Param("vehicleId") vehicleId: Long,
        @Param("companyId") companyId: Long
    ): List<Array<Any>>
}