package com.carslab.crm.modules.clients.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VehicleTableJpaRepository : JpaRepository<com.carslab.crm.modules.clients.infrastructure.persistence.entity.VehicleEntity, Long> {

    /**
     * POPRAWIONA implementacja - usunięto DISTINCT i dodano GROUP BY
     * Użycie CTE (Common Table Expression) dla lepszej czytelności i wydajności
     */
    @Query(nativeQuery = true, value = """
    WITH vehicle_data AS (
        SELECT 
            v.id,
            v.make,
            v.model,
            v.year,
            v.license_plate,
            v.color,
            v.vin,
            v.mileage,
            COALESCE(vs.visit_count, 0) as visit_count,
            v.last_service_date,
            COALESCE(vs.total_revenue, 0) as total_revenue,
            v.created_at,
            v.updated_at,
            -- Dodajemy wyrażenia sortujące do SELECT żeby można było ich użyć w ORDER BY
            CASE WHEN :sortField = 'make' THEN v.make END as sort_make,
            CASE WHEN :sortField = 'model' THEN v.model END as sort_model,
            CASE WHEN :sortField = 'year' THEN v.year END as sort_year,
            CASE WHEN :sortField = 'licensePlate' THEN v.license_plate END as sort_license_plate,
            CASE WHEN :sortField = 'visitCount' THEN COALESCE(vs.visit_count, 0) END as sort_visit_count,
            CASE WHEN :sortField = 'lastVisitDate' THEN v.last_service_date END as sort_last_service_date,
            CASE WHEN :sortField = 'totalRevenue' THEN COALESCE(vs.total_revenue, 0) END as sort_total_revenue,
            CASE WHEN :sortField = 'createdAt' THEN v.created_at END as sort_created_at
        FROM vehicles v
        LEFT JOIN vehicle_statistics vs ON v.id = vs.vehicle_id
        WHERE v.company_id = :companyId 
        AND v.active = true
        AND (:make IS NULL OR LOWER(v.make) LIKE LOWER(CONCAT('%', :make, '%')))
        AND (:model IS NULL OR LOWER(v.model) LIKE LOWER(CONCAT('%', :model, '%')))
        AND (:licensePlate IS NULL OR LOWER(v.license_plate) LIKE LOWER(CONCAT('%', :licensePlate, '%')))
        AND (:minVisits IS NULL OR COALESCE(vs.visit_count, 0) >= :minVisits)
        AND (:maxVisits IS NULL OR COALESCE(vs.visit_count, 0) <= :maxVisits)
    ),
    filtered_vehicles AS (
        SELECT vd.* FROM vehicle_data vd
        WHERE (:ownerName IS NULL OR EXISTS (
            SELECT 1 FROM client_vehicle_associations cva
            INNER JOIN clients c ON cva.client_id = c.id
            WHERE cva.vehicle_id = vd.id 
            AND cva.end_date IS NULL
            AND c.company_id = :companyId
            AND c.active = true
            AND LOWER(CONCAT(c.first_name, ' ', c.last_name)) LIKE LOWER(CONCAT('%', :ownerName, '%'))
        ))
    )
    SELECT 
        id, make, model, year, license_plate, color, vin, mileage,
        visit_count, last_service_date, total_revenue, created_at, updated_at
    FROM filtered_vehicles
    ORDER BY 
        CASE WHEN :sortDirection = 'ASC' THEN sort_make END ASC,
        CASE WHEN :sortDirection = 'DESC' THEN sort_make END DESC,
        CASE WHEN :sortDirection = 'ASC' THEN sort_model END ASC,
        CASE WHEN :sortDirection = 'DESC' THEN sort_model END DESC,
        CASE WHEN :sortDirection = 'ASC' THEN sort_year END ASC,
        CASE WHEN :sortDirection = 'DESC' THEN sort_year END DESC,
        CASE WHEN :sortDirection = 'ASC' THEN sort_license_plate END ASC,
        CASE WHEN :sortDirection = 'DESC' THEN sort_license_plate END DESC,
        CASE WHEN :sortDirection = 'ASC' THEN sort_visit_count END ASC,
        CASE WHEN :sortDirection = 'DESC' THEN sort_visit_count END DESC,
        CASE WHEN :sortDirection = 'ASC' THEN sort_last_service_date END ASC,
        CASE WHEN :sortDirection = 'DESC' THEN sort_last_service_date END DESC,
        CASE WHEN :sortDirection = 'ASC' THEN sort_total_revenue END ASC,
        CASE WHEN :sortDirection = 'DESC' THEN sort_total_revenue END DESC,
        CASE WHEN :sortDirection = 'ASC' THEN sort_created_at END ASC,
        CASE WHEN :sortDirection = 'DESC' THEN sort_created_at END DESC,
        created_at DESC
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

    /**
     * POPRAWIONE zapytanie COUNT - uproszczone i bardziej wydajne
     */
    @Query(nativeQuery = true, value = """
        SELECT COUNT(DISTINCT v.id)
        FROM vehicles v
        LEFT JOIN vehicle_statistics vs ON v.id = vs.vehicle_id
        WHERE v.company_id = :companyId 
        AND v.active = true
        AND (:make IS NULL OR LOWER(v.make) LIKE LOWER(CONCAT('%', :make, '%')))
        AND (:model IS NULL OR LOWER(v.model) LIKE LOWER(CONCAT('%', :model, '%')))
        AND (:licensePlate IS NULL OR LOWER(v.license_plate) LIKE LOWER(CONCAT('%', :licensePlate, '%')))
        AND (:minVisits IS NULL OR COALESCE(vs.visit_count, 0) >= :minVisits)
        AND (:maxVisits IS NULL OR COALESCE(vs.visit_count, 0) <= :maxVisits)
        AND (:ownerName IS NULL OR EXISTS (
            SELECT 1 FROM client_vehicle_associations cva
            INNER JOIN clients c ON cva.client_id = c.id
            WHERE cva.vehicle_id = v.id 
            AND cva.end_date IS NULL
            AND c.company_id = :companyId
            AND c.active = true
            AND LOWER(CONCAT(c.first_name, ' ', c.last_name)) LIKE LOWER(CONCAT('%', :ownerName, '%'))
        ))
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

    /**
     * POPRAWIONE zapytanie dla właścicieli pojazdów - optymalizacja
     */
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