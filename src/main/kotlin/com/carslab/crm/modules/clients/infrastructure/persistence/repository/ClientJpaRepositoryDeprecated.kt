package com.carslab.crm.modules.clients.infrastructure.persistence.repository

import com.carslab.crm.modules.clients.infrastructure.persistence.entity.ClientEntityDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.entity.ClientStatisticsEntityDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.entity.VehicleStatisticsEntityDeprecated
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Repository
interface ClientJpaRepositoryDeprecated : JpaRepository<ClientEntityDeprecated, Long>, JpaSpecificationExecutor<ClientEntityDeprecated> {

    // Basic queries with company isolation
    @Query("SELECT c FROM ClientEntityDeprecated c WHERE c.companyId = :companyId AND c.active = true")
    fun findByCompanyId(@Param("companyId") companyId: Long, pageable: Pageable): Page<ClientEntityDeprecated>

    @Query("SELECT c FROM ClientEntityDeprecated c WHERE c.id = :id AND c.companyId = :companyId AND c.active = true")
    fun findByIdAndCompanyId(@Param("id") id: Long, @Param("companyId") companyId: Long): Optional<ClientEntityDeprecated>

    // Simple queries without LOWER function
    @Query("SELECT c FROM ClientEntityDeprecated c WHERE c.email = :email AND c.companyId = :companyId AND c.active = true")
    fun findByEmailAndCompanyId(@Param("email") email: String, @Param("companyId") companyId: Long): Optional<ClientEntityDeprecated>

    @Query("SELECT c FROM ClientEntityDeprecated c WHERE c.phone = :phone AND c.companyId = :companyId AND c.active = true")
    fun findByPhoneAndCompanyId(@Param("phone") phone: String, @Param("companyId") companyId: Long): Optional<ClientEntityDeprecated>

    @Query("SELECT c FROM ClientEntityDeprecated c WHERE (c.email = :email OR c.phone = :phone) AND c.companyId = :companyId AND c.active = true")
    fun findByEmailOrPhoneAndCompanyId(
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("companyId") companyId: Long
    ): Optional<ClientEntityDeprecated>

    @Query(nativeQuery = true, value = """
        SELECT DISTINCT c.* FROM clients c 
        LEFT JOIN client_vehicle_associations cva ON c.id = cva.client_id AND cva.end_date IS NULL
        LEFT JOIN vehicles v ON cva.vehicle_id = v.id AND v.active = true
        LEFT JOIN client_statistics cs ON c.id = cs.client_id
        WHERE c.company_id = :companyId AND c.active = true
        AND (:name IS NULL OR 
             LOWER(REPLACE(CONCAT(COALESCE(c.first_name, ''), ' ', COALESCE(c.last_name, '')), '  ', ' ')) LIKE CONCAT('%', :name, '%') OR
             LOWER(COALESCE(c.company, '')) LIKE CONCAT('%', :name, '%'))
        AND (:email IS NULL OR LOWER(COALESCE(c.email, '')) LIKE CONCAT('%', :email, '%'))
        AND (:phone IS NULL OR REGEXP_REPLACE(COALESCE(c.phone, ''), '[^0-9]', '') LIKE CONCAT('%', :phone, '%'))
        AND (:company IS NULL OR LOWER(COALESCE(c.company, '')) LIKE CONCAT('%', :company, '%'))
        AND (:hasVehicles IS NULL OR 
             (:hasVehicles = true AND cva.client_id IS NOT NULL) OR 
             (:hasVehicles = false AND cva.client_id IS NULL))
        AND (:minTotalRevenue IS NULL OR COALESCE(cs.total_revenue, 0) >= :minTotalRevenue)
        AND (:maxTotalRevenue IS NULL OR COALESCE(cs.total_revenue, 0) <= :maxTotalRevenue)
        AND (:minVisits IS NULL OR COALESCE(cs.visit_count, 0) >= :minVisits)
        AND (:maxVisits IS NULL OR COALESCE(cs.visit_count, 0) <= :maxVisits)
        ORDER BY c.created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    fun searchClientsNative(
        @Param("name") name: String?,
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("company") company: String?,
        @Param("hasVehicles") hasVehicles: Boolean?,
        @Param("minTotalRevenue") minTotalRevenue: Double?,
        @Param("maxTotalRevenue") maxTotalRevenue: Double?,
        @Param("minVisits") minVisits: Int?,
        @Param("maxVisits") maxVisits: Int?,
        @Param("companyId") companyId: Long,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<ClientEntityDeprecated>

    @Query(nativeQuery = true, value = """
        SELECT COUNT(DISTINCT c.id) FROM clients c 
        LEFT JOIN client_vehicle_associations cva ON c.id = cva.client_id AND cva.end_date IS NULL
        LEFT JOIN vehicles v ON cva.vehicle_id = v.id AND v.active = true
        LEFT JOIN client_statistics cs ON c.id = cs.client_id
        WHERE c.company_id = :companyId AND c.active = true
        AND (:name IS NULL OR 
             LOWER(REPLACE(CONCAT(COALESCE(c.first_name, ''), ' ', COALESCE(c.last_name, '')), '  ', ' ')) LIKE CONCAT('%', :name, '%') OR
             LOWER(COALESCE(c.company, '')) LIKE CONCAT('%', :name, '%'))
        AND (:email IS NULL OR LOWER(COALESCE(c.email, '')) LIKE CONCAT('%', :email, '%'))
        AND (:phone IS NULL OR REGEXP_REPLACE(COALESCE(c.phone, ''), '[^0-9]', '') LIKE CONCAT('%', :phone, '%'))
        AND (:company IS NULL OR LOWER(COALESCE(c.company, '')) LIKE CONCAT('%', :company, '%'))
        AND (:hasVehicles IS NULL OR 
             (:hasVehicles = true AND cva.client_id IS NOT NULL) OR 
             (:hasVehicles = false AND cva.client_id IS NULL))
        AND (:minTotalRevenue IS NULL OR COALESCE(cs.total_revenue, 0) >= :minTotalRevenue)
        AND (:maxTotalRevenue IS NULL OR COALESCE(cs.total_revenue, 0) <= :maxTotalRevenue)
        AND (:minVisits IS NULL OR COALESCE(cs.visit_count, 0) >= :minVisits)
        AND (:maxVisits IS NULL OR COALESCE(cs.visit_count, 0) <= :maxVisits)
    """)
    fun countSearchClients(
        @Param("name") name: String?,
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("company") company: String?,
        @Param("hasVehicles") hasVehicles: Boolean?,
        @Param("minTotalRevenue") minTotalRevenue: Double?,
        @Param("maxTotalRevenue") maxTotalRevenue: Double?,
        @Param("minVisits") minVisits: Int?,
        @Param("maxVisits") maxVisits: Int?,
        @Param("companyId") companyId: Long
    ): Long

    // Soft delete
    @Modifying
    @Query("UPDATE ClientEntityDeprecated c SET c.active = false, c.updatedAt = :now WHERE c.id = :id AND c.companyId = :companyId")
    fun softDeleteByIdAndCompanyId(@Param("id") id: Long, @Param("companyId") companyId: Long, @Param("now") now: LocalDateTime): Int

    // Statistics
    @Query("SELECT COUNT(c) FROM ClientEntityDeprecated c WHERE c.companyId = :companyId AND c.active = true")
    fun countByCompanyId(@Param("companyId") companyId: Long): Long

    // Check existence
    fun existsByEmailAndCompanyIdAndActiveTrue(email: String, companyId: Long): Boolean
    fun existsByPhoneAndCompanyIdAndActiveTrue(phone: String, companyId: Long): Boolean
}

@Repository
interface ClientStatisticsJpaRepositoryDeprecated : JpaRepository<ClientStatisticsEntityDeprecated, Long> {

    @Query("SELECT s FROM ClientStatisticsEntityDeprecated s WHERE s.clientId = :clientId")
    fun findByClientId(@Param("clientId") clientId: Long): Optional<ClientStatisticsEntityDeprecated>

    @Modifying
    @Query(
        """
        UPDATE ClientStatisticsEntityDeprecated s 
        SET s.visitCount = s.visitCount + :increment, s.updatedAt = :now 
        WHERE s.clientId = :clientId
    """
    )
    fun updateVisitCount(@Param("clientId") clientId: Long, @Param("increment") increment: Long, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query(
        """
        UPDATE ClientStatisticsEntityDeprecated s 
        SET s.totalRevenue = s.totalRevenue + :amount, s.updatedAt = :now 
        WHERE s.clientId = :clientId
    """
    )
    fun updateRevenue(@Param("clientId") clientId: Long, @Param("amount") amount: BigDecimal, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query(
        """
        UPDATE ClientStatisticsEntityDeprecated s 
        SET s.vehicleCount = s.vehicleCount + :increment, s.updatedAt = :now 
        WHERE s.clientId = :clientId
    """
    )
    fun updateVehicleCount(@Param("clientId") clientId: Long, @Param("increment") increment: Long, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("DELETE FROM ClientStatisticsEntityDeprecated s WHERE s.clientId = :clientId")
    fun deleteByClientId(@Param("clientId") clientId: Long): Int
}
@Repository
interface VehicleStatisticsJpaRepositoryDeprecated : JpaRepository<VehicleStatisticsEntityDeprecated, Long> {

    @Query("SELECT s FROM VehicleStatisticsEntityDeprecated s WHERE s.vehicleId = :vehicleId")
    fun findByVehicleId(@Param("vehicleId") vehicleId: Long): Optional<VehicleStatisticsEntityDeprecated>

    @Modifying
    @Query(
        """
        UPDATE VehicleStatisticsEntityDeprecated s 
        SET s.visitCount = s.visitCount + :increment, s.updatedAt = :now 
        WHERE s.vehicleId = :vehicleId
    """
    )
    fun updateVisitCount(@Param("vehicleId") vehicleId: Long, @Param("increment") increment: Long, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query(
        """
        UPDATE VehicleStatisticsEntityDeprecated s 
        SET s.totalRevenue = s.totalRevenue + :amount, s.updatedAt = :now 
        WHERE s.vehicleId = :vehicleId
    """
    )
    fun updateRevenue(@Param("vehicleId") vehicleId: Long, @Param("amount") amount: BigDecimal, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("DELETE FROM VehicleStatisticsEntityDeprecated s WHERE s.vehicleId = :vehicleId")
    fun deleteByVehicleId(@Param("vehicleId") vehicleId: Long): Int

    @Query(nativeQuery = true, value = """
        SELECT COUNT(DISTINCT v.id)
        FROM vehicles v
        WHERE v.company_id = :companyId AND v.active = true
    """)
    fun countTotalVehiclesForCompany(@Param("companyId") companyId: Long): Long

    @Query(nativeQuery = true, value = """
        SELECT COUNT(DISTINCT v.id)
        FROM vehicles v
        INNER JOIN vehicle_statistics vs ON v.id = vs.vehicle_id
        WHERE v.company_id = :companyId 
        AND v.active = true
        AND vs.total_revenue >= :threshold
    """)
    fun countPremiumVehiclesForCompany(
        @Param("companyId") companyId: Long,
        @Param("threshold") threshold: BigDecimal
    ): Long

    @Query(nativeQuery = true, value = """
        SELECT COALESCE(SUM(vs.total_revenue), 0)
        FROM vehicle_statistics vs
        INNER JOIN vehicles v ON vs.vehicle_id = v.id
        WHERE v.company_id = :companyId AND v.active = true
    """)
    fun getTotalRevenueForCompany(@Param("companyId") companyId: Long): BigDecimal?

    @Query(nativeQuery = true, value = """
        SELECT 
            v.id,
            v.make,
            v.model,
            v.license_plate,
            vs.visit_count,
            vs.total_revenue
        FROM vehicles v
        INNER JOIN vehicle_statistics vs ON v.id = vs.vehicle_id
        WHERE v.company_id = :companyId 
        AND v.active = true
        AND vs.visit_count > 0
        ORDER BY vs.visit_count DESC, vs.total_revenue DESC
        LIMIT 1
    """)
    fun findMostActiveVehicleForCompany(@Param("companyId") companyId: Long): Array<Any>?

    @Query(nativeQuery = true, value = """
        SELECT ps.final_price
        FROM protocol_services ps
        INNER JOIN protocols p ON ps.protocol_id = p.id
        INNER JOIN vehicles v ON p.vehicle_id = v.id
        WHERE v.company_id = :companyId 
        AND v.active = true
        AND p.status = 'COMPLETED'
        AND ps.approval_status = 'APPROVED'
        AND ps.final_price > 0
        ORDER BY ps.final_price
    """)
    fun getAllCompletedVisitRevenuesForCompany(@Param("companyId") companyId: Long): List<BigDecimal>

    @Query(nativeQuery = true, value = """
    SELECT COALESCE(SUM(ps.final_price), 0)
    FROM protocol_services ps
    INNER JOIN protocols p ON ps.protocol_id = p.id
    INNER JOIN vehicles v ON p.vehicle_id = v.id
    WHERE v.company_id = :companyId 
    AND v.active = true
    AND p.status = 'COMPLETED'
    AND ps.approval_status = 'APPROVED'
    AND ps.final_price > 0
""")
    fun getTotalRevenueFromProtocolServicesForCompany(@Param("companyId") companyId: Long): BigDecimal?
}
