package com.carslab.crm.production.modules.clients.infrastructure.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "client_statistics",
    indexes = [
        Index(name = "idx_client_stats_client_id", columnList = "client_id"),
        Index(name = "idx_client_stats_visit_count", columnList = "visit_count"),
        Index(name = "idx_client_stats_total_revenue", columnList = "total_revenue"),
        Index(name = "idx_client_stats_last_visit", columnList = "last_visit_date")
    ]
)
class ClientStatisticsEntity(
    @Id
    @Column(name = "client_id")
    val clientId: Long,

    @Column(name = "visit_count", nullable = false)
    var visitCount: Long = 0,

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    var totalRevenue: BigDecimal = BigDecimal.ZERO,

    @Column(name = "vehicle_count", nullable = false)
    var vehicleCount: Long = 0,

    @Column(name = "last_visit_date")
    var lastVisitDate: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)