package com.carslab.crm.production.modules.associations.infrastructure.repository

import com.carslab.crm.production.modules.associations.infrastructure.entity.ClientVehicleAssociationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ClientVehicleAssociationJpaRepository : JpaRepository<ClientVehicleAssociationEntity, Long> {

    @Query("SELECT a FROM ClientVehicleAssociationEntity a WHERE a.clientId = :clientId AND a.endDate IS NULL")
    fun findByClientIdAndEndDateIsNull(@Param("clientId") clientId: Long): List<ClientVehicleAssociationEntity>

    @Query("SELECT a FROM ClientVehicleAssociationEntity a WHERE a.vehicleId = :vehicleId AND a.endDate IS NULL")
    fun findByVehicleIdAndEndDateIsNull(@Param("vehicleId") vehicleId: Long): List<ClientVehicleAssociationEntity>

    @Query("SELECT a FROM ClientVehicleAssociationEntity a WHERE a.clientId = :clientId AND a.vehicleId = :vehicleId")
    fun findByClientIdAndVehicleId(@Param("clientId") clientId: Long, @Param("vehicleId") vehicleId: Long): ClientVehicleAssociationEntity?

    @Query("SELECT a FROM ClientVehicleAssociationEntity a WHERE a.clientId IN :clientIds AND a.endDate IS NULL")
    fun findByClientIdInAndEndDateIsNull(@Param("clientIds") clientIds: List<Long>): List<ClientVehicleAssociationEntity>

    @Query("SELECT a FROM ClientVehicleAssociationEntity a WHERE a.vehicleId IN :vehicleIds AND a.endDate IS NULL")
    fun findByVehicleIdInAndEndDateIsNull(@Param("vehicleIds") vehicleIds: List<Long>): List<ClientVehicleAssociationEntity>

    @Modifying
    @Query("""
        UPDATE ClientVehicleAssociationEntity a 
        SET a.endDate = :endDate 
        WHERE a.clientId = :clientId AND a.vehicleId = :vehicleId AND a.endDate IS NULL
    """)
    fun endAssociation(
        @Param("clientId") clientId: Long,
        @Param("vehicleId") vehicleId: Long,
        @Param("endDate") endDate: LocalDateTime
    ): Int

    @Modifying
    @Query("""
        UPDATE ClientVehicleAssociationEntity a 
        SET a.endDate = :endDate 
        WHERE (a.clientId, a.vehicleId) IN :associations AND a.endDate IS NULL
    """)
    fun batchEndAssociations(
        @Param("associations") associations: List<Pair<Long, Long>>,
        @Param("endDate") endDate: LocalDateTime
    ): Int

    @Modifying
    @Query("DELETE FROM ClientVehicleAssociationEntity a WHERE a.clientId = :clientId")
    fun deleteByClientId(@Param("clientId") clientId: Long): Int

    @Modifying
    @Query("DELETE FROM ClientVehicleAssociationEntity a WHERE a.vehicleId = :vehicleId")
    fun deleteByVehicleId(@Param("vehicleId") vehicleId: Long): Int
}