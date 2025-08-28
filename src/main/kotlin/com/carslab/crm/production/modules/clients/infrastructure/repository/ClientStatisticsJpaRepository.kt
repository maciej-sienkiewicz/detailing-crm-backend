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

    @Query("SELECT s FROM ClientStatisticsEntity s WHERE s.clientId IN :clientIds")
    fun findByClientIds(@Param("clientIds") clientIds: List<Long>): List<ClientStatisticsEntity>

    @Modifying
    @Query("""
        INSERT INTO client_statistics (client_id, visit_count, total_revenue, vehicle_count, last_visit_date, created_at, updated_at) 
        VALUES (:clientId, 1, 0.00, 0, :visitDate, :now, :now)
        ON CONFLICT (client_id) DO UPDATE SET 
            visit_count = client_statistics.visit_count + 1,
            last_visit_date = :visitDate,
            updated_at = :now
    """, nativeQuery = true)
    fun upsertVisitCount(
        @Param("clientId") clientId: Long,
        @Param("visitDate") visitDate: LocalDateTime,
        @Param("now") now: LocalDateTime
    ): Int

    @Modifying
    @Query("""
        INSERT INTO client_statistics (client_id, visit_count, total_revenue, vehicle_count, last_visit_date, created_at, updated_at) 
        VALUES (:clientId, 0, 0.00, 1, NULL, :now, :now)
        ON CONFLICT (client_id) DO UPDATE SET 
            vehicle_count = client_statistics.vehicle_count + 1,
            updated_at = :now
    """, nativeQuery = true)
    fun upsertVehicleCount(
        @Param("clientId") clientId: Long,
        @Param("now") now: LocalDateTime
    ): Int

    @Modifying
    @Query("""
        INSERT INTO client_statistics (client_id, visit_count, total_revenue, vehicle_count, last_visit_date, created_at, updated_at) 
        VALUES (:clientId, 0, :amount, 0, NULL, :now, :now)
        ON CONFLICT (client_id) DO UPDATE SET 
            total_revenue = client_statistics.total_revenue + :amount,
            updated_at = :now
    """, nativeQuery = true)
    fun upsertRevenue(
        @Param("clientId") clientId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("now") now: LocalDateTime
    ): Int

    @Modifying
    @Query("""
        UPDATE ClientStatisticsEntity s 
        SET s.vehicleCount = :newCount, s.updatedAt = :now 
        WHERE s.clientId = :clientId
    """)
    fun updateVehicleCount(
        @Param("clientId") clientId: Long,
        @Param("newCount") newCount: Long,
        @Param("now") now: LocalDateTime
    ): Int
    
    @Modifying
    @Query("""
        UPDATE ClientStatisticsEntity s
        SET s.vehicleCount = GREATEST(s.vehicleCount - 1, 0), s.updatedAt = :now 
        WHERE s.clientId = :clientId AND s.vehicleCount > 0
    """)
    fun decrementVehicleCount(@Param("clientId") clientId: Long, @Param("now") now: LocalDateTime): Int

    @Modifying
    @Query("DELETE FROM ClientStatisticsEntity s WHERE s.clientId = :clientId")
    fun deleteByClientId(@Param("clientId") clientId: Long): Int

    @Modifying
    @Query("DELETE FROM ClientStatisticsEntity s WHERE s.clientId IN :clientIds")
    fun deleteByClientIds(@Param("clientIds") clientIds: List<Long>): Int
}