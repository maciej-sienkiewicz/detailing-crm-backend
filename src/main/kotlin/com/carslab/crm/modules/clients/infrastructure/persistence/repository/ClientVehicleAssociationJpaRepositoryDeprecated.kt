package com.carslab.crm.modules.clients.infrastructure.persistence.repository

import com.carslab.crm.modules.clients.infrastructure.persistence.entity.ClientVehicleAssociationEntityDeprecated
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface ClientVehicleAssociationJpaRepositoryDeprecated : JpaRepository<ClientVehicleAssociationEntityDeprecated, Long> {

    @Query(
        """
        SELECT a FROM ClientVehicleAssociationEntityDeprecated a 
        WHERE a.client.id = :clientId AND a.companyId = :companyId
    """
    )
    fun findByClientIdAndCompanyId(@Param("clientId") clientId: Long, @Param("companyId") companyId: Long): List<ClientVehicleAssociationEntityDeprecated>

    @Query(
        """
        SELECT a FROM ClientVehicleAssociationEntityDeprecated a 
        WHERE a.vehicle.id = :vehicleId AND a.companyId = :companyId
    """
    )
    fun findByVehicleIdAndCompanyId(@Param("vehicleId") vehicleId: Long, @Param("companyId") companyId: Long): List<ClientVehicleAssociationEntityDeprecated>

    @Query(
        """
        SELECT a FROM ClientVehicleAssociationEntityDeprecated a 
        WHERE a.client.id = :clientId AND a.endDate IS NULL AND a.companyId = :companyId
    """
    )
    fun findActiveByClientIdAndCompanyId(@Param("clientId") clientId: Long, @Param("companyId") companyId: Long): List<ClientVehicleAssociationEntityDeprecated>

    @Query(
        """
        SELECT a FROM ClientVehicleAssociationEntityDeprecated a 
        WHERE a.vehicle.id = :vehicleId AND a.endDate IS NULL AND a.companyId = :companyId
    """
    )
    fun findActiveByVehicleIdAndCompanyId(@Param("vehicleId") vehicleId: Long, @Param("companyId") companyId: Long): List<ClientVehicleAssociationEntityDeprecated>

    @Query(
        """
        SELECT a FROM ClientVehicleAssociationEntityDeprecated a 
        WHERE a.client.id = :clientId AND a.vehicle.id = :vehicleId AND a.companyId = :companyId
    """
    )
    fun findByClientIdAndVehicleIdAndCompanyId(
        @Param("clientId") clientId: Long,
        @Param("vehicleId") vehicleId: Long,
        @Param("companyId") companyId: Long
    ): Optional<ClientVehicleAssociationEntityDeprecated>

    @Modifying
    @Query(
        """
        UPDATE ClientVehicleAssociationEntityDeprecated a 
        SET a.endDate = :endDate 
        WHERE a.client.id = :clientId AND a.vehicle.id = :vehicleId AND a.endDate IS NULL AND a.companyId = :companyId
    """
    )
    fun endAssociation(
        @Param("clientId") clientId: Long,
        @Param("vehicleId") vehicleId: Long,
        @Param("endDate") endDate: LocalDateTime,
        @Param("companyId") companyId: Long
    ): Int

    @Modifying
    @Query("DELETE FROM ClientVehicleAssociationEntityDeprecated a WHERE a.client.id = :clientId AND a.companyId = :companyId")
    fun deleteByClientIdAndCompanyId(@Param("clientId") clientId: Long, @Param("companyId") companyId: Long): Int

    @Modifying
    @Query("DELETE FROM ClientVehicleAssociationEntityDeprecated a WHERE a.vehicle.id = :vehicleId AND a.companyId = :companyId")
    fun deleteByVehicleIdAndCompanyId(@Param("vehicleId") vehicleId: Long, @Param("companyId") companyId: Long): Int
}