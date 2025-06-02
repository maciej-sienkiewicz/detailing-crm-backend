package com.carslab.crm.clients.infrastructure.persistence.repository

import com.carslab.crm.clients.infrastructure.persistence.entity.ClientEntity
import com.carslab.crm.clients.infrastructure.persistence.entity.ClientStatisticsEntity
import com.carslab.crm.clients.infrastructure.persistence.entity.VehicleEntity
import com.carslab.crm.clients.infrastructure.persistence.entity.VehicleStatisticsEntity
import com.carslab.crm.domain.model.ContactAttemptResult
import com.carslab.crm.domain.model.ContactAttemptType
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.infrastructure.persistence.entity.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface ClientJpaRepository : JpaRepository<ClientEntity, Long>, JpaSpecificationExecutor<ClientEntity> {

    // Basic queries with company isolation
    @Query("SELECT c FROM ClientEntity c WHERE c.companyId = :companyId AND c.active = true")
    fun findByCompanyId(@Param("companyId") companyId: Long, pageable: Pageable): Page<ClientEntity>

    @Query("SELECT c FROM ClientEntity c WHERE c.id = :id AND c.companyId = :companyId AND c.active = true")
    fun findByIdAndCompanyId(@Param("id") id: Long, @Param("companyId") companyId: Long): Optional<ClientEntity>

    // Search queries
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

    // Advanced search with dynamic criteria
    @Query("""
        SELECT c FROM ClientEntity c 
        WHERE c.companyId = :companyId AND c.active = true
        AND (:name IS NULL OR LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
        AND (:phone IS NULL OR c.phone LIKE CONCAT('%', :phone, '%'))
        AND (:company IS NULL OR LOWER(c.company) LIKE LOWER(CONCAT('%', :company, '%')))
    """)
    fun searchClients(
        @Param("name") name: String?,
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("company") company: String?,
        @Param("companyId") companyId: Long,
        pageable: Pageable
    ): Page<ClientEntity>

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
interface VehicleJpaRepository : JpaRepository<VehicleEntity, Long>, JpaSpecificationExecutor<VehicleEntity> {

    // Basic queries with company isolation
    @Query("SELECT v FROM VehicleEntity v WHERE v.companyId = :companyId AND v.active = true")
    fun findByCompanyId(@Param("companyId") companyId: Long, pageable: Pageable): Page<VehicleEntity>

    @Query("SELECT v FROM VehicleEntity v WHERE v.id = :id AND v.companyId = :companyId AND v.active = true")
    fun findByIdAndCompanyId(@Param("id") id: Long, @Param("companyId") companyId: Long): Optional<VehicleEntity>

    // Search queries
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

    // Advanced search
    @Query("""
        SELECT v FROM VehicleEntity v 
        WHERE v.companyId = :companyId AND v.active = true
        AND (:make IS NULL OR LOWER(v.make) LIKE LOWER(CONCAT('%', :make, '%')))
        AND (:model IS NULL OR LOWER(v.model) LIKE LOWER(CONCAT('%', :model, '%')))
        AND (:licensePlate IS NULL OR LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :licensePlate, '%')))
        AND (:vin IS NULL OR LOWER(v.vin) LIKE LOWER(CONCAT('%', :vin, '%')))
        AND (:year IS NULL OR v.year = :year)
    """)
    fun searchVehicles(
        @Param("make") make: String?,
        @Param("model") model: String?,
        @Param("licensePlate") licensePlate: String?,
        @Param("vin") vin: String?,
        @Param("year") year: Int?,
        @Param("companyId") companyId: Long,
        pageable: Pageable
    ): Page<VehicleEntity>

    // Find vehicles by client
    @Query("""
        SELECT DISTINCT v FROM VehicleEntity v 
        JOIN v.associations a 
        WHERE a.client.id = :clientId AND a.endDate IS NULL 
        AND v.companyId = :companyId AND v.active = true
    """)
    fun findByClientIdAndCompanyId(@Param("clientId") clientId: Long, @Param("companyId") companyId: Long): List<VehicleEntity>

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
}

interface ProtocolJpaRepository : JpaRepository<ProtocolEntity, Long>, JpaSpecificationExecutor<ProtocolEntity> {
    fun findByCompanyId(companyId: Long): List<ProtocolEntity>

    // Zmiana typu parametru z String na Long
    fun findByCompanyIdAndId(companyId: Long, id: Long): Optional<ProtocolEntity>

    fun findByStatusAndCompanyId(status: ProtocolStatus, companyId: Long): List<ProtocolEntity>
    fun findByClientIdAndCompanyId(clientId: Long, companyId: Long): List<ProtocolEntity>

    @Query("SELECT COUNT(p) FROM ProtocolEntity p WHERE p.status = :status AND p.companyId = :companyId")
    fun countByStatusAndCompanyId(@Param("status") status: ProtocolStatus, @Param("companyId") companyId: Long): Int


    @Query(nativeQuery = true,
        value = "SELECT p.* FROM protocols p " +
                "JOIN clients c ON p.client_id = c.id " +
                "JOIN vehicles v ON p.vehicle_id = v.id " +
                "WHERE " +
                "(:clientName IS NULL OR LOWER(CAST(c.first_name AS text)) LIKE LOWER(CONCAT('%', CAST(:clientName AS text), '%')) OR LOWER(CAST(c.last_name AS text)) LIKE LOWER(CONCAT('%', CAST(:clientName AS text), '%'))) AND " +
                "(:clientId IS NULL OR p.client_id = :clientId) AND " +
                "(:licensePlate IS NULL OR LOWER(CAST(v.license_plate AS text)) LIKE LOWER(CONCAT('%', CAST(:licensePlate AS text), '%'))) AND " +
                "(:status IS NULL OR p.status = :status) AND " +
                "(:startDate IS NULL OR p.end_date >= :startDate) AND " +
                "(:endDate IS NULL OR p.start_date <= :endDate) AND " +
                "p.company_id = :companyId " +
                "ORDER BY p.created_at DESC " +
                "LIMIT :limit OFFSET :offset",
        countQuery = "SELECT COUNT(p.*) FROM protocols p " +
                "JOIN clients c ON p.client_id = c.id " +
                "JOIN vehicles v ON p.vehicle_id = v.id " +
                "WHERE " +
                "(:clientName IS NULL OR LOWER(CAST(c.first_name AS text)) LIKE LOWER(CONCAT('%', CAST(:clientName AS text), '%')) OR LOWER(CAST(c.last_name AS text)) LIKE LOWER(CONCAT('%', CAST(:clientName AS text), '%'))) AND " +
                "(:clientId IS NULL OR p.client_id = :clientId) AND " +
                "(:licensePlate IS NULL OR LOWER(CAST(v.license_plate AS text)) LIKE LOWER(CONCAT('%', CAST(:licensePlate AS text), '%'))) AND " +
                "(:status IS NULL OR p.status = :status) AND " +
                "(:startDate IS NULL OR p.end_date >= :startDate) AND " +
                "(:endDate IS NULL OR p.start_date <= :endDate) AND " +
                "p.company_id = :companyId")
    fun searchProtocolsPaginated(
        @Param("clientName") clientName: String?,
        @Param("clientId") clientId: Long?,
        @Param("licensePlate") licensePlate: String?,
        @Param("status") status: String?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?,
        @Param("companyId") companyId: Long,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<ProtocolEntity>

    // Podobnie modyfikujemy pozostałe metody
    @Query("SELECT p FROM ProtocolEntity p " +
            "JOIN ClientEntity c ON p.clientId = c.id " +
            "WHERE (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND p.companyId = :companyId")
    fun findByClientNameAndCompanyId(@Param("name") name: String, @Param("companyId") companyId: Long): List<ProtocolEntity>

    @Query("SELECT p FROM ProtocolEntity p " +
            "JOIN VehicleEntity v ON p.vehicleId = v.id " +
            "WHERE LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :licensePlate, '%')) " +
            "AND p.companyId = :companyId")
    fun findByLicensePlateContainingAndCompanyId(@Param("licensePlate") licensePlate: String, @Param("companyId") companyId: Long): List<ProtocolEntity>
}

@Repository
interface ProtocolServiceJpaRepository : JpaRepository<ProtocolServiceEntity, Long> {
    fun findByCompanyId(companyId: Long): List<ProtocolServiceEntity>
    fun findByProtocolIdAndCompanyId(protocolId: Long, companyId: Long): List<ProtocolServiceEntity>
}

@Repository
interface ProtocolCommentJpaRepository : JpaRepository<ProtocolCommentEntity, Long> {
    fun findByProtocolId(protocolId: Long): List<ProtocolCommentEntity>
}

@Repository
interface ServiceHistoryJpaRepository : JpaRepository<ServiceHistoryEntity, String> {
    fun findByCompanyId(companyId: Long): List<ServiceHistoryEntity>
    fun findByCompanyIdAndId(companyId: Long, id: String): Optional<ServiceHistoryEntity>
    fun findByVehicleIdAndCompanyId(vehicleId: Long, companyId: Long): List<ServiceHistoryEntity>
    fun findByDateBetweenAndCompanyId(startDate: LocalDate, endDate: LocalDate, companyId: Long): List<ServiceHistoryEntity>
}

@Repository
interface ContactAttemptJpaRepository : JpaRepository<ContactAttemptEntity, String> {
    fun findByCompanyId(companyId: Long): List<ContactAttemptEntity>
    fun findByCompanyIdAndId(companyId: Long, id: String): Optional<ContactAttemptEntity>
    fun findByClientIdAndCompanyId(clientId: String, companyId: Long): List<ContactAttemptEntity>
    fun findByTypeAndCompanyId(type: ContactAttemptType, companyId: Long): List<ContactAttemptEntity>
    fun findByResultAndCompanyId(result: ContactAttemptResult, companyId: Long): List<ContactAttemptEntity>
    fun findByDateBetweenAndCompanyId(startDate: LocalDateTime, endDate: LocalDateTime, companyId: Long): List<ContactAttemptEntity>
}

@Repository
interface VehicleImageJpaRepository : JpaRepository<VehicleImageEntity, String> {
    fun findByCompanyId(companyId: Long): List<VehicleImageEntity>
    fun findByCompanyIdAndId(companyId: Long, id: String): Optional<VehicleImageEntity>
    fun findByProtocolIdAndCompanyId(protocolId: Long, companyId: Long): List<VehicleImageEntity>
    fun findByCompanyIdAndIdIn(companyId: Long, ids: Set<String>): List<VehicleImageEntity>
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

@Repository
interface ServiceRecipeJpaRepository : JpaRepository<ServiceRecipeEntity, Long> {
    fun findByCompanyId(companyId: Long): List<ServiceRecipeEntity>
    fun findByCompanyIdAndId(companyId: Long, id: Long): Optional<ServiceRecipeEntity>
    fun findByNameContainingIgnoreCaseAndCompanyId(name: String, companyId: Long): List<ServiceRecipeEntity>
}