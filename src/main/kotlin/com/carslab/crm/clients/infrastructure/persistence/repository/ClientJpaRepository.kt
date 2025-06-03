package com.carslab.crm.clients.infrastructure.persistence.repository

import com.carslab.crm.clients.infrastructure.persistence.entity.ClientEntity
import com.carslab.crm.clients.infrastructure.persistence.entity.ClientStatisticsEntity
import com.carslab.crm.clients.infrastructure.persistence.entity.VehicleEntity
import com.carslab.crm.clients.infrastructure.persistence.entity.VehicleStatisticsEntity
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
import java.util.Optional

@Repository
interface ClientJpaRepository : JpaRepository<ClientEntity, Long>, JpaSpecificationExecutor<ClientEntity> {

    // Basic queries with company isolation
    @Query("SELECT c FROM ClientEntity c WHERE c.companyId = :companyId AND c.active = true")
    fun findByCompanyId(@Param("companyId") companyId: Long, pageable: Pageable): Page<ClientEntity>

    @Query("SELECT c FROM ClientEntity c WHERE c.id = :id AND c.companyId = :companyId AND c.active = true")
    fun findByIdAndCompanyId(@Param("id") id: Long, @Param("companyId") companyId: Long): Optional<ClientEntity>

    // Simple queries without LOWER function
    @Query("SELECT c FROM ClientEntity c WHERE c.email = :email AND c.companyId = :companyId AND c.active = true")
    fun findByEmailAndCompanyId(@Param("email") email: String, @Param("companyId") companyId: Long): Optional<ClientEntity>

    @Query("SELECT c FROM ClientEntity c WHERE c.phone = :phone AND c.companyId = :companyId AND c.active = true")
    fun findByPhoneAndCompanyId(@Param("phone") phone: String, @Param("companyId") companyId: Long): Optional<ClientEntity>

    @Query("SELECT c FROM ClientEntity c WHERE (c.email = :email OR c.phone = :phone) AND c.companyId = :companyId AND c.active = true")
    fun findByEmailOrPhoneAndCompanyId(
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("companyId") companyId: Long
    ): Optional<ClientEntity>

    // FIXED: Native query zamiast JPQL dla wyszukiwania
    @Query(nativeQuery = true, value = """
        SELECT * FROM clients c 
        WHERE c.company_id = :companyId AND c.active = true
        AND (:name IS NULL OR LOWER(CONCAT(c.first_name, ' ', c.last_name)) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
        AND (:phone IS NULL OR c.phone LIKE CONCAT('%', :phone, '%'))
        AND (:company IS NULL OR LOWER(COALESCE(c.company, '')) LIKE LOWER(CONCAT('%', :company, '%')))
        ORDER BY c.created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    fun searchClientsNative(
        @Param("name") name: String?,
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("company") company: String?,
        @Param("companyId") companyId: Long,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<ClientEntity>

    // Count query dla paginacji
    @Query(nativeQuery = true, value = """
        SELECT COUNT(*) FROM clients c 
        WHERE c.company_id = :companyId AND c.active = true
        AND (:name IS NULL OR LOWER(CONCAT(c.first_name, ' ', c.last_name)) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
        AND (:phone IS NULL OR c.phone LIKE CONCAT('%', :phone, '%'))
        AND (:company IS NULL OR LOWER(COALESCE(c.company, '')) LIKE LOWER(CONCAT('%', :company, '%')))
    """)
    fun countSearchClients(
        @Param("name") name: String?,
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("company") company: String?,
        @Param("companyId") companyId: Long
    ): Long

    // Soft delete
    @Modifying
    @Query("UPDATE ClientEntity c SET c.active = false, c.updatedAt = :now WHERE c.id = :id AND c.companyId = :companyId")
    fun softDeleteByIdAndCompanyId(@Param("id") id: Long, @Param("companyId") companyId: Long, @Param("now") now: LocalDateTime): Int

    // Statistics
    @Query("SELECT COUNT(c) FROM ClientEntity c WHERE c.companyId = :companyId AND c.active = true")
    fun countByCompanyId(@Param("companyId") companyId: Long): Long

    // Check existence
    fun existsByEmailAndCompanyIdAndActiveTrue(email: String, companyId: Long): Boolean
    fun existsByPhoneAndCompanyIdAndActiveTrue(phone: String, companyId: Long): Boolean
}

@Repository
interface ClientStatisticsJpaRepository : JpaRepository<ClientStatisticsEntity, Long> {

    @Query("SELECT s FROM ClientStatisticsEntity s WHERE s.clientId = :clientId")
    fun findByClientId(@Param("clientId") clientId: Long): Optional<ClientStatisticsEntity>

    @Modifying
    @Query("""
        UPDATE ClientStatisticsEntity s 
        SET s.visitCount = s.visitCount + :increment, s.updatedAt = :now 
        WHERE s.clientId = :clientId
    """)
    fun updateVisitCount(@Param("clientId") clientId: Long, @Param("increment") increment: Long, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("""
        UPDATE ClientStatisticsEntity s 
        SET s.totalRevenue = s.totalRevenue + :amount, s.updatedAt = :now 
        WHERE s.clientId = :clientId
    """)
    fun updateRevenue(@Param("clientId") clientId: Long, @Param("amount") amount: BigDecimal, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("""
        UPDATE ClientStatisticsEntity s 
        SET s.vehicleCount = s.vehicleCount + :increment, s.updatedAt = :now 
        WHERE s.clientId = :clientId
    """)
    fun updateVehicleCount(@Param("clientId") clientId: Long, @Param("increment") increment: Long, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("DELETE FROM ClientStatisticsEntity s WHERE s.clientId = :clientId")
    fun deleteByClientId(@Param("clientId") clientId: Long): Int
}
@Repository
interface VehicleStatisticsJpaRepository : JpaRepository<VehicleStatisticsEntity, Long> {

    @Query("SELECT s FROM VehicleStatisticsEntity s WHERE s.vehicleId = :vehicleId")
    fun findByVehicleId(@Param("vehicleId") vehicleId: Long): Optional<VehicleStatisticsEntity>

    @Modifying
    @Query("""
        UPDATE VehicleStatisticsEntity s 
        SET s.visitCount = s.visitCount + :increment, s.updatedAt = :now 
        WHERE s.vehicleId = :vehicleId
    """)
    fun updateVisitCount(@Param("vehicleId") vehicleId: Long, @Param("increment") increment: Long, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("""
        UPDATE VehicleStatisticsEntity s 
        SET s.totalRevenue = s.totalRevenue + :amount, s.updatedAt = :now 
        WHERE s.vehicleId = :vehicleId
    """)
    fun updateRevenue(@Param("vehicleId") vehicleId: Long, @Param("amount") amount: BigDecimal, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("DELETE FROM VehicleStatisticsEntity s WHERE s.vehicleId = :vehicleId")
    fun deleteByVehicleId(@Param("vehicleId") vehicleId: Long): Int
}
