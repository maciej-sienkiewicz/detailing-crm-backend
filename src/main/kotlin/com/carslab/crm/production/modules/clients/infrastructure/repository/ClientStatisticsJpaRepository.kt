package com.carslab.crm.production.modules.clients.infrastructure.repository

import com.carslab.crm.production.modules.clients.infrastructure.entity.ClientStatisticsEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Repository
interface ClientStatisticsJpaRepository : JpaRepository<ClientStatisticsEntity, Long> {

    @Query("SELECT s FROM ClientStatisticsEntity s WHERE s.clientId = :clientId")
    fun findByClientId(@Param("clientId") clientId: Long): Optional<ClientStatisticsEntity>

    @Modifying
    @Query("""
        UPDATE ClientStatisticsEntity s 
        SET s.visitCount = s.visitCount + 1, s.updatedAt = :now 
        WHERE s.clientId = :clientId
    """)
    fun incrementVisitCount(@Param("clientId") clientId: Long, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("""
        UPDATE ClientStatisticsEntity s 
        SET s.totalRevenue = s.totalRevenue + :amount, s.updatedAt = :now 
        WHERE s.clientId = :clientId
    """)
    fun addRevenue(@Param("clientId") clientId: Long, @Param("amount") amount: BigDecimal, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("""
        UPDATE ClientStatisticsEntity s 
        SET s.vehicleCount = s.vehicleCount + 1, s.updatedAt = :now 
        WHERE s.clientId = :clientId
    """)
    fun incrementVehicleCount(@Param("clientId") clientId: Long, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("""
        UPDATE ClientStatisticsEntity s 
        SET s.lastVisitDate = :visitDate, s.updatedAt = :now 
        WHERE s.clientId = :clientId
    """)
    fun setLastVisitDate(@Param("clientId") clientId: Long, @Param("visitDate") visitDate: LocalDateTime, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("DELETE FROM ClientStatisticsEntity s WHERE s.clientId = :clientId")
    fun deleteByClientId(@Param("clientId") clientId: Long): Int
}