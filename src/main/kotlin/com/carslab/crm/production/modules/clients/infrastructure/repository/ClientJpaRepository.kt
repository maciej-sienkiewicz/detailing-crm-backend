package com.carslab.crm.production.modules.clients.infrastructure.repository

import com.carslab.crm.production.modules.clients.infrastructure.entity.ClientEntity
import com.carslab.crm.production.modules.clients.infrastructure.dto.ClientWithStatisticsRaw
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
interface ClientJpaRepository : JpaRepository<ClientEntity, Long> {

    @Query("SELECT c FROM ClientEntity c WHERE c.id IN :ids AND c.active = true AND c.companyId = :companyId")
    fun findByIdInAndCompanyIdAndActiveTrue(@Param("ids") ids: List<Long>, @Param("companyId") companyId: Long): List<ClientEntity>

    @Query("SELECT c FROM ClientEntity c WHERE c.companyId = :companyId AND c.active = true ORDER BY c.createdAt DESC")
    fun findByCompanyIdAndActiveTrue(@Param("companyId") companyId: Long, pageable: Pageable): Page<ClientEntity>

    @Query("SELECT c FROM ClientEntity c WHERE c.id = :id AND c.companyId = :companyId AND c.active = true")
    fun findByIdAndCompanyIdAndActiveTrue(@Param("id") id: Long, @Param("companyId") companyId: Long): Optional<ClientEntity>

    @Query("SELECT c FROM ClientEntity c WHERE c.email = :email AND c.companyId = :companyId AND c.active = true")
    fun findByEmailAndCompanyIdAndActiveTrue(@Param("email") email: String, @Param("companyId") companyId: Long): Optional<ClientEntity>

    @Query("SELECT c FROM ClientEntity c WHERE c.phone = :phone AND c.companyId = :companyId AND c.active = true")
    fun findByPhoneAndCompanyIdAndActiveTrue(@Param("phone") phone: String, @Param("companyId") companyId: Long): Optional<ClientEntity>

    fun existsByEmailAndCompanyIdAndActiveTrue(email: String, companyId: Long): Boolean
    fun existsByPhoneAndCompanyIdAndActiveTrue(phone: String, companyId: Long): Boolean

    @Modifying
    @Query("UPDATE ClientEntity c SET c.active = false, c.updatedAt = :now WHERE c.id = :id AND c.companyId = :companyId")
    fun softDeleteByIdAndCompanyId(@Param("id") id: Long, @Param("companyId") companyId: Long, @Param("now") now: LocalDateTime): Int

    @Query("""
    SELECT c FROM ClientEntity c 
    WHERE c.companyId = :companyId 
    AND c.active = true 
    AND (
        (:phone IS NOT NULL AND :phone != '' AND c.phone = :phone) 
        OR 
        (:email IS NOT NULL AND :email != '' AND c.email = :email)
    )
""")
    fun findByPhoneOrEmailAndCompanyIdAndActiveTrue(phone: String?, email: String?, companyId: Long): Optional<ClientEntity>
    
    @Query("""
        SELECT c FROM ClientEntity c 
        WHERE c.companyId = :companyId AND c.active = true
        AND (:name IS NULL OR 
             LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
        AND (:phone IS NULL OR c.phone LIKE CONCAT('%', :phone, '%'))
        AND (:company IS NULL OR LOWER(COALESCE(c.company, '')) LIKE LOWER(CONCAT('%', :company, '%')))
        ORDER BY c.createdAt DESC
    """)
    fun searchClientsSimple(
        @Param("companyId") companyId: Long,
        @Param("name") name: String?,
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("company") company: String?,
        pageable: Pageable
    ): Page<ClientEntity>

    @Query(nativeQuery = true, value = """
    SELECT c.id as client_id,
        c.company_id as client_company_id,
        c.first_name as client_first_name,
        c.last_name as client_last_name,
        c.email as client_email,
        c.phone as client_phone,
        c.address as client_address,
        c.company as client_company,
        c.tax_id as client_tax_id,
        c.notes as client_notes,
        c.created_at as client_created_at,
        c.updated_at as client_updated_at,
        c.active as client_active,
        cs.client_id as stats_client_id,
        cs.visit_count as stats_visit_count,
        cs.total_revenue_netto as stats_total_revenue_netto,
        cs.total_revenue_brutto as stats_total_revenue_brutto,
        cs.total_tax_amount as stats_total_tax_amount,
        cs.vehicle_count as stats_vehicle_count,
        cs.last_visit_date as stats_last_visit_date,
        cs.updated_at as stats_updated_at
    FROM clients c 
    LEFT JOIN client_statistics cs ON c.id = cs.client_id
    WHERE c.company_id = :companyId AND c.active = true
    AND (:name IS NULL OR 
         LOWER(c.first_name || ' ' || c.last_name) LIKE LOWER('%' || :name || '%'))
    AND (:email IS NULL OR LOWER(c.email) LIKE LOWER('%' || :email || '%'))
    AND (:phone IS NULL OR c.phone LIKE '%' || :phone || '%')
    AND (:company IS NULL OR LOWER(COALESCE(c.company, '')) LIKE LOWER('%' || :company || '%'))
    AND (:minVehicles IS NULL OR 
         COALESCE(cs.vehicle_count, 0) >= :minVehicles)
    AND (:minTotalRevenue IS NULL OR COALESCE(cs.total_revenue_brutto, 0) >= :minTotalRevenue)
    AND (:maxTotalRevenue IS NULL OR COALESCE(cs.total_revenue_brutto, 0) <= :maxTotalRevenue)
    AND (:minVisits IS NULL OR COALESCE(cs.visit_count, 0) >= :minVisits)
    AND (:maxVisits IS NULL OR COALESCE(cs.visit_count, 0) <= :maxVisits)
    ORDER BY c.created_at DESC
    LIMIT :limit OFFSET :offset
""")
    fun searchClientsWithStatisticsOptimized(
        @Param("companyId") companyId: Long,
        @Param("name") name: String?,
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("company") company: String?,
        @Param("minVehicles") minVehicles: Int?,
        @Param("minTotalRevenue") minTotalRevenue: Double?,
        @Param("maxTotalRevenue") maxTotalRevenue: Double?,
        @Param("minVisits") minVisits: Int?,
        @Param("maxVisits") maxVisits: Int?,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<ClientWithStatisticsRaw>

    @Query(nativeQuery = true, value = """
        SELECT COUNT(c.id) 
        FROM clients c 
        LEFT JOIN client_statistics cs ON c.id = cs.client_id
        WHERE c.company_id = :companyId AND c.active = true
        AND (:name IS NULL OR 
             LOWER(c.first_name || ' ' || c.last_name) LIKE LOWER('%' || :name || '%'))
        AND (:email IS NULL OR LOWER(c.email) LIKE LOWER('%' || :email || '%'))
        AND (:phone IS NULL OR c.phone LIKE '%' || :phone || '%')
        AND (:company IS NULL OR LOWER(COALESCE(c.company, '')) LIKE LOWER('%' || :company || '%'))
            AND (:minVehicles IS NULL OR 
         COALESCE(cs.vehicle_count, 0) >= :minVehicles)
        AND (:minTotalRevenue IS NULL OR COALESCE(cs.total_revenue_brutto, 0) >= :minTotalRevenue)
        AND (:maxTotalRevenue IS NULL OR COALESCE(cs.total_revenue_brutto, 0) <= :maxTotalRevenue)
        AND (:minVisits IS NULL OR COALESCE(cs.visit_count, 0) >= :minVisits)
        AND (:maxVisits IS NULL OR COALESCE(cs.visit_count, 0) <= :maxVisits)
    """)
    fun countSearchClientsOptimized(
        @Param("companyId") companyId: Long,
        @Param("name") name: String?,
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("company") company: String?,
        @Param("minVehicles") minVehicles: Int?,
        @Param("minTotalRevenue") minTotalRevenue: Double?,
        @Param("maxTotalRevenue") maxTotalRevenue: Double?,
        @Param("minVisits") minVisits: Int?,
        @Param("maxVisits") maxVisits: Int?
    ): Long

    @Query(nativeQuery = true, value = """
        SELECT c.id, c.first_name, c.last_name, c.email, c.phone
        FROM clients c 
        WHERE c.company_id = :companyId AND c.active = true
        AND c.id > :lastId
        ORDER BY c.id ASC
        LIMIT :limit
    """)
    fun findClientsAfterCursor(
        @Param("companyId") companyId: Long,
        @Param("lastId") lastId: Long,
        @Param("limit") limit: Int
    ): List<ClientProjection>

    interface ClientProjection {
        fun getId(): Long
        fun getFirstName(): String
        fun getLastName(): String
        fun getEmail(): String
        fun getPhone(): String
    }
}