package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.domain.model.ContactAttemptResult
import com.carslab.crm.domain.model.ContactAttemptType
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.infrastructure.persistence.entity.*
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolCommentEntityDeprecated
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolEntityDeprecated
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolServiceEntityDeprecated
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface ProtocolJpaRepository : JpaRepository<ProtocolEntityDeprecated, Long>, JpaSpecificationExecutor<ProtocolEntityDeprecated> {
    fun findByCompanyId(companyId: Long): List<ProtocolEntityDeprecated>

    // Zmiana typu parametru z String na Long
    fun findByCompanyIdAndId(companyId: Long, id: Long): Optional<ProtocolEntityDeprecated>

    fun findByStatusAndCompanyId(status: ProtocolStatus, companyId: Long): List<ProtocolEntityDeprecated>
    fun findByClientIdAndCompanyId(clientId: Long, companyId: Long): List<ProtocolEntityDeprecated>

    @Query("SELECT COUNT(p) FROM ProtocolEntityDeprecated p WHERE p.status = :status AND p.companyId = :companyId")
    fun countByStatusAndCompanyId(@Param("status") status: ProtocolStatus, @Param("companyId") companyId: Long): Int

    fun countByCompanyId(companyId: Long): Int

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
    ): List<ProtocolEntityDeprecated>

    // Podobnie modyfikujemy pozostałe metody
    @Query(
        "SELECT p FROM ProtocolEntityDeprecated p " +
            "JOIN ClientEntityDeprecated c ON p.clientId = c.id " +
            "WHERE (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND p.companyId = :companyId")
    fun findByClientNameAndCompanyId(@Param("name") name: String, @Param("companyId") companyId: Long): List<ProtocolEntityDeprecated>

    @Query(
        "SELECT p FROM ProtocolEntityDeprecated p " +
            "JOIN VehicleEntityDeprecated v ON p.vehicleId = v.id " +
            "WHERE LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :licensePlate, '%')) " +
            "AND p.companyId = :companyId")
    fun findByLicensePlateContainingAndCompanyId(@Param("licensePlate") licensePlate: String, @Param("companyId") companyId: Long): List<ProtocolEntityDeprecated>
}

@Repository
interface ProtocolServiceJpaRepository : JpaRepository<ProtocolServiceEntityDeprecated, Long> {
    fun findByCompanyId(companyId: Long): List<ProtocolServiceEntityDeprecated>
    fun findByProtocolIdAndCompanyId(protocolId: Long, companyId: Long): List<ProtocolServiceEntityDeprecated>
}

@Repository
interface ProtocolCommentJpaRepository : JpaRepository<ProtocolCommentEntityDeprecated, Long> {
    fun findByProtocolId(protocolId: Long): List<ProtocolCommentEntityDeprecated>
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