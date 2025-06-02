package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.domain.model.ContactAttemptResult
import com.carslab.crm.domain.model.ContactAttemptType
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.infrastructure.persistence.entity.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface ClientJpaRepository : JpaRepository<ClientEntity, Long> {
    fun findByCompanyId(companyId: Long): List<ClientEntity>
    fun findByCompanyIdAndId(companyId: Long, id: Long): Optional<ClientEntity>
    fun findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseAndCompanyId(
        firstName: String,
        lastName: String,
        companyId: Long
    ): List<ClientEntity>
    fun findByEmailContainingIgnoreCaseAndCompanyId(email: String, companyId: Long): List<ClientEntity>
    fun findByPhoneContainingAndCompanyId(phone: String, companyId: Long): List<ClientEntity>

    @Query("SELECT c FROM ClientEntity c WHERE (c.email = :email OR c.phone = :phone) AND c.companyId = :companyId")
    fun findByEmailOrPhoneAndCompanyId(
        @Param("email") email: String?,
        @Param("phone") phone: String?,
        @Param("companyId") companyId: Long
    ): List<ClientEntity>
}

@Repository
interface VehicleJpaRepository : JpaRepository<VehicleEntity, Long> {

    @Query("SELECT DISTINCT v FROM VehicleEntity v LEFT JOIN FETCH v.owners WHERE v.companyId = :companyId")
    fun findByCompanyId(@Param("companyId") companyId: Long): List<VehicleEntity>

    fun findByCompanyIdAndId(companyId: Long, id: Long): Optional<VehicleEntity>
    fun findByLicensePlateIgnoreCaseAndCompanyId(licensePlate: String, companyId: Long): VehicleEntity?
    fun findByVinIgnoreCaseAndCompanyId(vin: String, companyId: Long): VehicleEntity?

    @Query("SELECT v FROM VehicleEntity v WHERE " +
            "((v.vin = :vin AND :vin IS NOT NULL) OR " +
            "(v.licensePlate = :licensePlate AND :licensePlate IS NOT NULL)) " +
            "AND v.companyId = :companyId")
    fun findByVinOrLicensePlateAndCompanyId(
        @Param("vin") vin: String?,
        @Param("licensePlate") licensePlate: String?,
        @Param("companyId") companyId: Long
    ): VehicleEntity?

    // ✅ FIXED: Original query with LEFT JOIN FETCH to load owners
    @Query("SELECT DISTINCT v FROM VehicleEntity v LEFT JOIN FETCH v.owners c WHERE c.id = :clientId AND v.companyId = :companyId")
    fun findAllByClientIdAndCompanyId(@Param("clientId") clientId: Long, @Param("companyId") companyId: Long): List<VehicleEntity>

    fun findByMakeContainingIgnoreCaseAndCompanyId(make: String, companyId: Long): List<VehicleEntity>
    fun findByModelContainingIgnoreCaseAndCompanyId(model: String, companyId: Long): List<VehicleEntity>

    // ✅ NEW METHODS: Eager loading versions to avoid LazyInitializationException

    /**
     * Find all vehicles with owners loaded (for full detail views)
     */
    @Query("SELECT DISTINCT v FROM VehicleEntity v LEFT JOIN FETCH v.owners WHERE v.companyId = :companyId")
    fun findByCompanyIdWithOwners(@Param("companyId") companyId: Long): List<VehicleEntity>

    /**
     * Find single vehicle with owners loaded
     */
    @Query("SELECT v FROM VehicleEntity v LEFT JOIN FETCH v.owners WHERE v.id = :id AND v.companyId = :companyId")
    fun findByIdAndCompanyIdWithOwners(@Param("id") id: Long, @Param("companyId") companyId: Long): VehicleEntity?

    /**
     * Find all vehicles with owners loaded (for admin views)
     */
    @Query("SELECT DISTINCT v FROM VehicleEntity v LEFT JOIN FETCH v.owners")
    fun findAllWithOwners(): List<VehicleEntity>

    /**
     * Find by VIN or License Plate with owners loaded
     */
    @Query("SELECT v FROM VehicleEntity v LEFT JOIN FETCH v.owners WHERE " +
            "((v.vin = :vin AND :vin IS NOT NULL) OR " +
            "(v.licensePlate = :licensePlate AND :licensePlate IS NOT NULL)) " +
            "AND v.companyId = :companyId")
    fun findByVinOrLicensePlateAndCompanyIdWithOwners(
        @Param("vin") vin: String?,
        @Param("licensePlate") licensePlate: String?,
        @Param("companyId") companyId: Long
    ): VehicleEntity?

    /**
     * Search by make with owners loaded
     */
    @Query("SELECT DISTINCT v FROM VehicleEntity v LEFT JOIN FETCH v.owners WHERE " +
            "LOWER(v.make) LIKE LOWER(CONCAT('%', :make, '%')) AND v.companyId = :companyId")
    fun findByMakeContainingIgnoreCaseAndCompanyIdWithOwners(
        @Param("make") make: String,
        @Param("companyId") companyId: Long
    ): List<VehicleEntity>

    /**
     * Search by model with owners loaded
     */
    @Query("SELECT DISTINCT v FROM VehicleEntity v LEFT JOIN FETCH v.owners WHERE " +
            "LOWER(v.model) LIKE LOWER(CONCAT('%', :model, '%')) AND v.companyId = :companyId")
    fun findByModelContainingIgnoreCaseAndCompanyIdWithOwners(
        @Param("model") model: String,
        @Param("companyId") companyId: Long
    ): List<VehicleEntity>

    // ✅ UTILITY QUERIES: For statistics and counts

    /**
     * Count vehicles by company
     */
    @Query("SELECT COUNT(v) FROM VehicleEntity v WHERE v.companyId = :companyId")
    fun countByCompanyId(@Param("companyId") companyId: Long): Long

    /**
     * Get vehicle IDs and owner counts for efficient list views
     */
    @Query("SELECT v.id, COUNT(o.id) FROM VehicleEntity v LEFT JOIN v.owners o WHERE v.companyId = :companyId GROUP BY v.id")
    fun findVehicleOwnerCounts(@Param("companyId") companyId: Long): List<Array<Any>>

    /**
     * Check if vehicle exists by license plate
     */
    fun existsByLicensePlateIgnoreCaseAndCompanyId(licensePlate: String, companyId: Long): Boolean

    /**
     * Check if vehicle exists by VIN
     */
    fun existsByVinIgnoreCaseAndCompanyId(vin: String, companyId: Long): Boolean

    /**
     * Find vehicles by multiple criteria (for advanced search)
     */
    @Query("SELECT DISTINCT v FROM VehicleEntity v LEFT JOIN FETCH v.owners WHERE " +
            "(:make IS NULL OR LOWER(v.make) LIKE LOWER(CONCAT('%', :make, '%'))) AND " +
            "(:model IS NULL OR LOWER(v.model) LIKE LOWER(CONCAT('%', :model, '%'))) AND " +
            "(:year IS NULL OR v.year = :year) AND " +
            "(:licensePlate IS NULL OR LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :licensePlate, '%'))) AND " +
            "v.companyId = :companyId")
    fun findByCriteriaWithOwners(
        @Param("make") make: String?,
        @Param("model") model: String?,
        @Param("year") year: Int?,
        @Param("licensePlate") licensePlate: String?,
        @Param("companyId") companyId: Long
    ): List<VehicleEntity>

    /**
     * Find vehicles without owners (for performance in list views)
     */
    @Query("SELECT v FROM VehicleEntity v WHERE " +
            "(:make IS NULL OR LOWER(v.make) LIKE LOWER(CONCAT('%', :make, '%'))) AND " +
            "(:model IS NULL OR LOWER(v.model) LIKE LOWER(CONCAT('%', :model, '%'))) AND " +
            "(:year IS NULL OR v.year = :year) AND " +
            "(:licensePlate IS NULL OR LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :licensePlate, '%'))) AND " +
            "v.companyId = :companyId")
    fun findByCriteriaWithoutOwners(
        @Param("make") make: String?,
        @Param("model") model: String?,
        @Param("year") year: Int?,
        @Param("licensePlate") licensePlate: String?,
        @Param("companyId") companyId: Long
    ): List<VehicleEntity>
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

    /**
     * Find statistics for clients belonging to a specific company
     */
    @Query("SELECT cs FROM ClientStatisticsEntity cs JOIN cs.client c WHERE c.companyId = :companyId")
    fun findByCompanyId(@Param("companyId") companyId: Long): List<ClientStatisticsEntity>

    /**
     * Find statistics for a specific client, checking that it belongs to the specified company
     */
    @Query("SELECT cs FROM ClientStatisticsEntity cs JOIN cs.client c WHERE cs.clientId = :clientId AND c.companyId = :companyId")
    fun findByClientIdAndCompanyId(
        @Param("clientId") clientId: Long,
        @Param("companyId") companyId: Long
    ): Optional<ClientStatisticsEntity>

    /**
     * Find statistics for clients with visit count greater than specified value
     */
    @Query("SELECT cs FROM ClientStatisticsEntity cs JOIN cs.client c WHERE cs.visitNo > :minVisits AND c.companyId = :companyId")
    fun findByVisitNoGreaterThanAndCompanyId(
        @Param("minVisits") minVisits: Long,
        @Param("companyId") companyId: Long
    ): List<ClientStatisticsEntity>

    /**
     * Find statistics for clients with GMV greater than specified value
     */
    @Query("SELECT cs FROM ClientStatisticsEntity cs JOIN cs.client c WHERE cs.gmv > :minGmv AND c.companyId = :companyId")
    fun findByGmvGreaterThanAndCompanyId(
        @Param("minGmv") minGmv: BigDecimal,
        @Param("companyId") companyId: Long
    ): List<ClientStatisticsEntity>

    /**
     * Get top clients by GMV
     */
    @Query("SELECT cs FROM ClientStatisticsEntity cs JOIN cs.client c WHERE c.companyId = :companyId ORDER BY cs.gmv DESC")
    fun findTopClientsByGmvAndCompanyId(
        @Param("companyId") companyId: Long,
        pageable: org.springframework.data.domain.Pageable
    ): List<ClientStatisticsEntity>

    /**
     * Get top clients by visit count
     */
    @Query("SELECT cs FROM ClientStatisticsEntity cs JOIN cs.client c WHERE c.companyId = :companyId ORDER BY cs.visitNo DESC")
    fun findTopClientsByVisitNoAndCompanyId(
        @Param("companyId") companyId: Long,
        pageable: org.springframework.data.domain.Pageable
    ): List<ClientStatisticsEntity>

    /**
     * Calculate total GMV for a company
     */
    @Query("SELECT SUM(cs.gmv) FROM ClientStatisticsEntity cs JOIN cs.client c WHERE c.companyId = :companyId")
    fun calculateTotalGmvByCompanyId(@Param("companyId") companyId: Long): BigDecimal

    /**
     * Calculate total visits for a company
     */
    @Query("SELECT SUM(cs.visitNo) FROM ClientStatisticsEntity cs JOIN cs.client c WHERE c.companyId = :companyId")
    fun calculateTotalVisitsByCompanyId(@Param("companyId") companyId: Long): Long

    /**
     * Count clients with statistics for a company
     */
    @Query("SELECT COUNT(cs) FROM ClientStatisticsEntity cs JOIN cs.client c WHERE c.companyId = :companyId")
    fun countClientStatisticsByCompanyId(@Param("companyId") companyId: Long): Long

    /**
     * Delete statistics for a client, ensuring it belongs to the specified company
     */
    @Query("DELETE FROM ClientStatisticsEntity cs WHERE cs.clientId = :clientId AND EXISTS (SELECT 1 FROM ClientEntity c WHERE c.id = cs.clientId AND c.companyId = :companyId)")
    fun deleteByClientIdAndCompanyId(
        @Param("clientId") clientId: Long,
        @Param("companyId") companyId: Long
    ): Int
}

@Repository
interface VehicleStatisticsJpaRepository : JpaRepository<VehicleStatisticsEntity, Long> {

    /**
     * Find statistics for vehicles belonging to a specific company
     */
    @Query("SELECT vs FROM VehicleStatisticsEntity vs JOIN vs.vehicle v WHERE v.companyId = :companyId")
    fun findByCompanyId(@Param("companyId") companyId: Long): List<VehicleStatisticsEntity>

    /**
     * Find statistics for a specific vehicle, checking that it belongs to the specified company
     */
    @Query("SELECT vs FROM VehicleStatisticsEntity vs JOIN vs.vehicle v WHERE vs.vehicleId = :vehicleId AND v.companyId = :companyId")
    fun findByVehicleIdAndCompanyId(
        @Param("vehicleId") vehicleId: Long,
        @Param("companyId") companyId: Long
    ): Optional<VehicleStatisticsEntity>

    /**
     * Find statistics for vehicles with visit count greater than specified value
     */
    @Query("SELECT vs FROM VehicleStatisticsEntity vs JOIN vs.vehicle v WHERE vs.visitNo > :minVisits AND v.companyId = :companyId")
    fun findByVisitNoGreaterThanAndCompanyId(
        @Param("minVisits") minVisits: Long,
        @Param("companyId") companyId: Long
    ): List<VehicleStatisticsEntity>

    /**
     * Find statistics for vehicles with GMV greater than specified value
     */
    @Query("SELECT vs FROM VehicleStatisticsEntity vs JOIN vs.vehicle v WHERE vs.gmv > :minGmv AND v.companyId = :companyId")
    fun findByGmvGreaterThanAndCompanyId(
        @Param("minGmv") minGmv: BigDecimal,
        @Param("companyId") companyId: Long
    ): List<VehicleStatisticsEntity>

    /**
     * Get top vehicles by GMV
     */
    @Query("SELECT vs FROM VehicleStatisticsEntity vs JOIN vs.vehicle v WHERE v.companyId = :companyId ORDER BY vs.gmv DESC")
    fun findTopVehiclesByGmvAndCompanyId(
        @Param("companyId") companyId: Long,
        pageable: org.springframework.data.domain.Pageable
    ): List<VehicleStatisticsEntity>

    /**
     * Get top vehicles by visit count
     */
    @Query("SELECT vs FROM VehicleStatisticsEntity vs JOIN vs.vehicle v WHERE v.companyId = :companyId ORDER BY vs.visitNo DESC")
    fun findTopVehiclesByVisitNoAndCompanyId(
        @Param("companyId") companyId: Long,
        pageable: org.springframework.data.domain.Pageable
    ): List<VehicleStatisticsEntity>

    /**
     * Calculate total GMV for vehicles in a company
     */
    @Query("SELECT SUM(vs.gmv) FROM VehicleStatisticsEntity vs JOIN vs.vehicle v WHERE v.companyId = :companyId")
    fun calculateTotalGmvByCompanyId(@Param("companyId") companyId: Long): BigDecimal

    /**
     * Calculate total visits for vehicles in a company
     */
    @Query("SELECT SUM(vs.visitNo) FROM VehicleStatisticsEntity vs JOIN vs.vehicle v WHERE v.companyId = :companyId")
    fun calculateTotalVisitsByCompanyId(@Param("companyId") companyId: Long): Long

    /**
     * Count vehicles with statistics for a company
     */
    @Query("SELECT COUNT(vs) FROM VehicleStatisticsEntity vs JOIN vs.vehicle v WHERE v.companyId = :companyId")
    fun countVehicleStatisticsByCompanyId(@Param("companyId") companyId: Long): Long

    /**
     * Delete statistics for a vehicle, ensuring it belongs to the specified company
     */
    @Query("DELETE FROM VehicleStatisticsEntity vs WHERE vs.vehicleId = :vehicleId AND EXISTS (SELECT 1 FROM VehicleEntity v WHERE v.id = vs.vehicleId AND v.companyId = :companyId)")
    fun deleteByVehicleIdAndCompanyId(
        @Param("vehicleId") vehicleId: Long,
        @Param("companyId") companyId: Long
    ): Long
}

@Repository
interface ServiceRecipeJpaRepository : JpaRepository<ServiceRecipeEntity, Long> {
    fun findByCompanyId(companyId: Long): List<ServiceRecipeEntity>
    fun findByCompanyIdAndId(companyId: Long, id: Long): Optional<ServiceRecipeEntity>
    fun findByNameContainingIgnoreCaseAndCompanyId(name: String, companyId: Long): List<ServiceRecipeEntity>
}