package com.carslab.crm.audit.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_logs")
data class AuditLog(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = true)
    val tenantId: UUID?,

    @Column(nullable = true)
    val userId: UUID?,

    @Column(nullable = false, length = 50)
    val action: String,

    @Column(nullable = false, length = 100)
    val resource: String,

    val resourceId: String?,

    @Column(columnDefinition = "TEXT")
    val details: String?,

    @Column(length = 45)
    val ipAddress: String?,

    @Column(length = 200)
    val userAgent: String?,

    @Column(nullable = false)
    val timestamp: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val severity: AuditSeverity = AuditSeverity.INFO
)

enum class AuditSeverity {
    INFO, WARN, ERROR, CRITICAL
}