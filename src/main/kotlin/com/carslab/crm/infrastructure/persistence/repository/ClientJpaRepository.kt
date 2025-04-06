package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.domain.model.ContactAttemptResult
import com.carslab.crm.domain.model.ContactAttemptType
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.infrastructure.persistence.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface ClientJpaRepository : JpaRepository<ClientEntity, Long> {
    fun findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(firstName: String, lastName: String): List<ClientEntity>
    fun findByEmailContainingIgnoreCase(email: String): List<ClientEntity>
    fun findByPhoneContaining(phone: String): List<ClientEntity>

    @Query("SELECT c FROM ClientEntity c WHERE c.email = :email OR c.phone = :phone")
    fun findByEmailOrPhone(@Param("email") email: String?, @Param("phone") phone: String?): List<ClientEntity>
}

@Repository
interface VehicleJpaRepository : JpaRepository<VehicleEntity, Long> {
    fun findByLicensePlateIgnoreCase(licensePlate: String): VehicleEntity?
    fun findByVinIgnoreCase(vin: String): VehicleEntity?

    @Query("SELECT v FROM VehicleEntity v WHERE " +
            "(v.vin = :vin AND :vin IS NOT NULL) OR " +
            "(v.licensePlate = :licensePlate AND :licensePlate IS NOT NULL)")
    fun findByVinOrLicensePlate(
        @Param("vin") vin: String?,
        @Param("licensePlate") licensePlate: String?
    ): VehicleEntity?

    @Query("SELECT v FROM VehicleEntity v JOIN v.owners c WHERE c.id = :clientId")
    fun findAllByClientId(@Param("clientId") clientId: Long): List<VehicleEntity>

    fun findByMakeContainingIgnoreCase(make: String): List<VehicleEntity>
    fun findByModelContainingIgnoreCase(model: String): List<VehicleEntity>
}

@Repository
interface ProtocolJpaRepository : JpaRepository<ProtocolEntity, String> {
    fun findByStatus(status: ProtocolStatus): List<ProtocolEntity>
    fun findByClientId(clientId: Long): List<ProtocolEntity>

    // Usunięta metoda findByClientFirstNameContainingIgnoreCaseOrClientLastNameContainingIgnoreCase
    // i zastąpiona zapytaniem JPQL, które odpowiednio łączy tabele
    @Query("SELECT p FROM ProtocolEntity p " +
            "JOIN ClientEntity c ON p.clientId = c.id " +
            "WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun findByClientName(@Param("name") name: String): List<ProtocolEntity>

    @Query("SELECT p FROM ProtocolEntity p " +
            "JOIN VehicleEntity v ON p.vehicleId = v.id " +
            "WHERE LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :licensePlate, '%'))")
    fun findByLicensePlateContaining(@Param("licensePlate") licensePlate: String): List<ProtocolEntity>

    fun findByStartDateGreaterThanEqual(startDate: LocalDateTime): List<ProtocolEntity>
    fun findByEndDateLessThanEqual(endDate: LocalDateTime): List<ProtocolEntity>

    @Query(nativeQuery = true,
        value = "SELECT p.* FROM protocols p " +
                "JOIN clients c ON p.client_id = c.id " +
                "JOIN vehicles v ON p.vehicle_id = v.id " +
                "WHERE " +
                "(CAST(:clientName AS text) IS NULL OR LOWER(c.first_name::text) LIKE LOWER(CONCAT('%', :clientName, '%')) OR LOWER(c.last_name::text) LIKE LOWER(CONCAT('%', :clientName, '%'))) AND " +
                "(CAST(:clientId AS text) IS NULL OR p.client_id = :clientId) AND " +
                "(CAST(:licensePlate AS text) IS NULL OR LOWER(v.license_plate::text) LIKE LOWER(CONCAT('%', :licensePlate, '%'))) AND " +
                "(CAST(:status AS text) IS NULL OR p.status = :status::text) AND " +
                "(CAST(:startDate AS text) IS NULL OR p.end_date >= :startDate) AND " +
                "(CAST(:endDate AS text) IS NULL OR p.start_date <= :endDate)")
    fun searchProtocols(
        @Param("clientName") clientName: String?,
        @Param("clientId") clientId: Long?,
        @Param("licensePlate") licensePlate: String?,
        @Param("status") status: ProtocolStatus?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): List<ProtocolEntity>
}

@Repository
interface ProtocolServiceJpaRepository : JpaRepository<ProtocolServiceEntity, Long> {
    fun findByProtocolId(protocolId: Long): List<ProtocolServiceEntity>
}

@Repository
interface ProtocolCommentJpaRepository : JpaRepository<ProtocolCommentEntity, Long> {
    fun findByProtocolId(protocolId: Long): List<ProtocolCommentEntity>
}

@Repository
interface ServiceHistoryJpaRepository : JpaRepository<ServiceHistoryEntity, String> {
    fun findByVehicleId(vehicleId: Long): List<ServiceHistoryEntity>
    fun findByDateBetween(startDate: LocalDate, endDate: LocalDate): List<ServiceHistoryEntity>
}

@Repository
interface ContactAttemptJpaRepository : JpaRepository<ContactAttemptEntity, String> {
    fun findByClientId(clientId: String): List<ContactAttemptEntity>
    fun findByType(type: ContactAttemptType): List<ContactAttemptEntity>
    fun findByResult(result: ContactAttemptResult): List<ContactAttemptEntity>
    fun findByDateBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<ContactAttemptEntity>
}

@Repository
interface VehicleImageJpaRepository : JpaRepository<VehicleImageEntity, String> {
    fun findByProtocolId(protocolId: Long): List<VehicleImageEntity>
}

@Repository
interface ClientStatisticsJpaRepository : JpaRepository<ClientStatisticsEntity, Long>

@Repository
interface VehicleStatisticsJpaRepository : JpaRepository<VehicleStatisticsEntity, Long>

@Repository
interface ServiceRecipeJpaRepository : JpaRepository<ServiceRecipeEntity, Long>