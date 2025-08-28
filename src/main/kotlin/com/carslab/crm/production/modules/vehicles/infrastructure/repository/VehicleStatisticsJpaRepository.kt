package com.carslab.crm.production.modules.vehicles.infrastructure.repository

import com.carslab.crm.production.modules.vehicles.infrastructure.entity.VehicleStatisticsEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Repository
interface VehicleStatisticsJpaRepository : JpaRepository<VehicleStatisticsEntity, Long> {

    @Query("SELECT s FROM VehicleStatisticsEntity s WHERE s.vehicleId = :vehicleId")
    fun findByVehicleId(@Param("vehicleId") vehicleId: Long): Optional<VehicleStatisticsEntity>

    @Query("SELECT s FROM VehicleStatisticsEntity s WHERE s.vehicleId IN :vehicleIds")
    fun findByVehicleIds(@Param("vehicleIds") vehicleIds: List<Long>): List<VehicleStatisticsEntity>

    @Modifying
    @Query("""
        UPDATE VehicleStatisticsEntity s 
        SET s.visitCount = s.visitCount + 1, s.updatedAt = :now 
        WHERE s.vehicleId = :vehicleId
    """)
    fun incrementVisitCount(@Param("vehicleId") vehicleId: Long, @Param("now") now: LocalDateTime = LocalDateTime.now()): Int

    @Modifying
    @Query("""
        INSERT INTO vehicle_statistics (vehicle_id, visit_count, total_revenue, created_at, updated_at) 
        VALUES (:vehicleId, 1, 0.00, :now, :now)
        ON CONFLICT (vehicle_id) DO UPDATE SET 
            visit_count = vehicle_statistics.visit_count + 1,
            last_visit_date = :visitDate,
            updated_at = :now
    """, nativeQuery = true)
    fun upsertVisitCount(@Param("vehicleId") vehicleId: Long, @Param("visitDate") visitDate: LocalDateTime, @Param("now") now: LocalDateTime): Int
    
    @Modifying
    @Query("""
        UPDATE VehicleStatisticsEntity s 
        SET s.totalRevenue = s.totalRevenue + :amount, s.updatedAt = :now 
        WHERE s.vehicleId = :vehicleId
    """)
    fun addRevenue(@Param("vehicleId") vehicleId: Long, @Param("amount") amount: BigDecimal, @Param("now") now: LocalDateTime = LocalDateTime.now()): Int

    @Modifying
    @Query("DELETE FROM VehicleStatisticsEntity s WHERE s.vehicleId = :vehicleId")
    fun deleteByVehicleId(@Param("vehicleId") vehicleId: Long): Int
}