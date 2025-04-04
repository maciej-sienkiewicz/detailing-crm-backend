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

    fun findByMakeContainingIgnoreCase(make: String): List<VehicleEntity>
    fun findByModelContainingIgnoreCase(model: String): List<VehicleEntity>
}

@Repository
interface ProtocolJpaRepository : JpaRepository<ProtocolEntity, String> {
    fun findByStatus(status: ProtocolStatus): List<ProtocolEntity>
    fun findByVehicle_LicensePlateContainingIgnoreCase(licensePlate: String): List<ProtocolEntity>
    fun findByClient_Id(clientId: Long): List<ProtocolEntity>
    fun findByClient_FirstNameContainingIgnoreCaseOrClient_LastNameContainingIgnoreCase(firstName: String, lastName: String): List<ProtocolEntity>
    fun findByStartDateGreaterThanEqual(startDate: LocalDateTime): List<ProtocolEntity>
    fun findByEndDateLessThanEqual(endDate: LocalDateTime): List<ProtocolEntity>

    @Query("SELECT p FROM ProtocolEntity p WHERE " +
            "(:clientName IS NULL OR LOWER(p.client.firstName) LIKE LOWER(CONCAT('%', :clientName, '%')) OR LOWER(p.client.lastName) LIKE LOWER(CONCAT('%', :clientName, '%'))) AND " +
            "(:clientId IS NULL OR p.client.id = :clientId) AND " +
            "(:licensePlate IS NULL OR LOWER(p.vehicle.licensePlate) LIKE LOWER(CONCAT('%', :licensePlate, '%'))) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:startDate IS NULL OR p.endDate >= :startDate) AND " +
            "(:endDate IS NULL OR p.startDate <= :endDate)")
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
    fun findByProtocol_Id(protocolId: Long): List<ProtocolServiceEntity>
}

@Repository
interface ProtocolCommentJpaRepository : JpaRepository<ProtocolCommentEntity, Long> {
    fun findByProtocol_Id(protocolId: Long): List<ProtocolCommentEntity>
}

@Repository
interface ServiceHistoryJpaRepository : JpaRepository<ServiceHistoryEntity, String> {
    fun findByVehicle_Id(vehicleId: Long): List<ServiceHistoryEntity>
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
    fun findByProtocol_Id(protocolId: Long): List<VehicleImageEntity>
}

@Repository
interface ClientStatisticsJpaRepository : JpaRepository<ClientStatisticsEntity, Long>

@Repository
interface VehicleStatisticsJpaRepository : JpaRepository<VehicleStatisticsEntity, Long>

@Repository
interface ServiceRecipeJpaRepository : JpaRepository<ServiceRecipeEntity, Long>