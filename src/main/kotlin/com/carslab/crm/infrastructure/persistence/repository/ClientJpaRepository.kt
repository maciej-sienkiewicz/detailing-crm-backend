package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.domain.model.ContactAttemptResult
import com.carslab.crm.domain.model.ContactAttemptType
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.infrastructure.persistence.entity.*
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolCommentEntity
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolEntity
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolServiceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@Repository
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
interface ServiceRecipeJpaRepository : JpaRepository<ServiceRecipeEntity, Long> {
    fun findByCompanyId(companyId: Long): List<ServiceRecipeEntity>
    fun findByCompanyIdAndId(companyId: Long, id: Long): Optional<ServiceRecipeEntity>
    fun findByNameContainingIgnoreCaseAndCompanyId(name: String, companyId: Long): List<ServiceRecipeEntity>
}