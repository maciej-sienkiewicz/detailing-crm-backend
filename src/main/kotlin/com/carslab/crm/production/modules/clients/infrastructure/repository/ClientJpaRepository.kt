package com.carslab.crm.production.modules.clients.infrastructure.repository

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.infrastructure.entity.ClientEntity
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

    @Query(nativeQuery = true, value = """
        SELECT DISTINCT c.* FROM clients c 
        LEFT JOIN client_vehicle_associations cva ON c.id = cva.client_id AND cva.end_date IS NULL
        LEFT JOIN client_statistics cs ON c.id = cs.client_id
        WHERE c.company_id = :companyId AND c.active = true
        AND (:name IS NULL OR 
             LOWER(CONCAT(c.first_name, ' ', c.last_name)) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
        AND (:phone IS NULL OR c.phone LIKE CONCAT('%', :phone, '%'))
        AND (:company IS NULL OR LOWER(COALESCE(c.company, '')) LIKE LOWER(CONCAT('%', :company, '%')))
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
    fun searchClients(
        @Param("companyId") companyId: Long,
        @Param("name") name: String?,
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("company") company: String?,
        @Param("hasVehicles") hasVehicles: Boolean?,
        @Param("minTotalRevenue") minTotalRevenue: Double?,
        @Param("maxTotalRevenue") maxTotalRevenue: Double?,
        @Param("minVisits") minVisits: Int?,
        @Param("maxVisits") maxVisits: Int?,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<ClientEntity>

    @Query(nativeQuery = true, value = """
        SELECT COUNT(DISTINCT c.id) FROM clients c 
        LEFT JOIN client_vehicle_associations cva ON c.id = cva.client_id AND cva.end_date IS NULL
        LEFT JOIN client_statistics cs ON c.id = cs.client_id
        WHERE c.company_id = :companyId AND c.active = true
        AND (:name IS NULL OR 
             LOWER(CONCAT(c.first_name, ' ', c.last_name)) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
        AND (:phone IS NULL OR c.phone LIKE CONCAT('%', :phone, '%'))
        AND (:company IS NULL OR LOWER(COALESCE(c.company, '')) LIKE LOWER(CONCAT('%', :company, '%')))
        AND (:hasVehicles IS NULL OR 
             (:hasVehicles = true AND cva.client_id IS NOT NULL) OR 
             (:hasVehicles = false AND cva.client_id IS NULL))
        AND (:minTotalRevenue IS NULL OR COALESCE(cs.total_revenue, 0) >= :minTotalRevenue)
        AND (:maxTotalRevenue IS NULL OR COALESCE(cs.total_revenue, 0) <= :maxTotalRevenue)
        AND (:minVisits IS NULL OR COALESCE(cs.visit_count, 0) >= :minVisits)
        AND (:maxVisits IS NULL OR COALESCE(cs.visit_count, 0) <= :maxVisits)
    """)
    fun countSearchClients(
        @Param("companyId") companyId: Long,
        @Param("name") name: String?,
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("company") company: String?,
        @Param("hasVehicles") hasVehicles: Boolean?,
        @Param("minTotalRevenue") minTotalRevenue: Double?,
        @Param("maxTotalRevenue") maxTotalRevenue: Double?,
        @Param("minVisits") minVisits: Int?,
        @Param("maxVisits") maxVisits: Int?
    ): Long
}