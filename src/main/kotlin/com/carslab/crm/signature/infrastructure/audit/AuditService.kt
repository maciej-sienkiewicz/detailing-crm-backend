package com.carslab.crm.signature.infrastructure.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_logs")
data class AuditLog(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val tenantId: UUID?,

    @Column(nullable = false)
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

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, UUID> {
    fun findByTenantIdAndTimestampBetween(
        tenantId: UUID,
        startTime: Instant,
        endTime: Instant
    ): List<AuditLog>

    fun findBySeverityAndTimestampAfter(
        severity: AuditSeverity,
        timestamp: Instant
    ): List<AuditLog>
}

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun logSignatureRequest(tenantId: UUID?, sessionId: String?, status: String, details: String? = null) {
        val auditLog = AuditLog(
            tenantId = tenantId,
            userId = getCurrentUserId(),
            action = "SIGNATURE_REQUEST",
            resource = "signature_session",
            resourceId = sessionId,
            details = details ?: status,
            ipAddress = getCurrentIpAddress(),
            severity = if (status == "ERROR") AuditSeverity.ERROR else AuditSeverity.INFO
        )

        saveAuditLog(auditLog)
    }

    fun logSignatureCompletion(tenantId: UUID?, sessionId: String, status: String, details: String? = null) {
        val auditLog = AuditLog(
            tenantId = tenantId,
            userId = getCurrentUserId(),
            action = "SIGNATURE_COMPLETION",
            resource = "signature_session",
            resourceId = sessionId,
            details = details ?: status,
            severity = if (status == "ERROR") AuditSeverity.ERROR else AuditSeverity.INFO
        )

        saveAuditLog(auditLog)
    }

    fun logTabletConnection(tabletId: UUID, tenantId: UUID, status: String, details: String? = null) {
        val auditLog = AuditLog(
            tenantId = tenantId,
            userId = null, // System action
            action = "TABLET_CONNECTION",
            resource = "tablet_device",
            resourceId = tabletId.toString(),
            details = details ?: status,
            severity = when (status) {
                "ERROR" -> AuditSeverity.ERROR
                "DISCONNECTED" -> AuditSeverity.WARN
                else -> AuditSeverity.INFO
            }
        )

        saveAuditLog(auditLog)
    }

    fun logWorkstationConnection(
        workstationId: UUID,
        tenantId: UUID,
        userId: UUID,
        status: String,
        details: String? = null
    ) {
        val auditLog = AuditLog(
            tenantId = tenantId,
            userId = userId,
            action = "WORKSTATION_CONNECTION",
            resource = "workstation",
            resourceId = workstationId.toString(),
            details = details ?: status,
            severity = if (status == "ERROR") AuditSeverity.ERROR else AuditSeverity.INFO
        )

        saveAuditLog(auditLog)
    }

    fun logSecurityViolation(action: String, ipAddress: String?, details: String?) {
        val auditLog = AuditLog(
            tenantId = null,
            userId = getCurrentUserId(),
            action = action.uppercase(),
            resource = "security",
            resourceId = null,
            details = details,
            ipAddress = ipAddress,
            severity = AuditSeverity.CRITICAL
        )

        saveAuditLog(auditLog)

        // Also log to security-specific logger for SIEM integration
        logger.warn("SECURITY_VIOLATION: $action from $ipAddress - $details")
    }

    fun logSignatureAcknowledgment(tabletId: UUID, sessionId: String, success: Boolean) {
        val auditLog = AuditLog(
            tenantId = null, // Will be resolved from tablet
            userId = null,
            action = "SIGNATURE_ACKNOWLEDGMENT",
            resource = "signature_session",
            resourceId = sessionId,
            details = "Tablet $tabletId acknowledged signature completion: $success"
        )

        saveAuditLog(auditLog)
    }

    fun logWorkstationNotification(workstationId: UUID, tenantId: UUID, messageType: String) {
        val auditLog = AuditLog(
            tenantId = tenantId,
            userId = getCurrentUserId(),
            action = "WORKSTATION_NOTIFICATION",
            resource = "workstation",
            resourceId = workstationId.toString(),
            details = "Sent notification: $messageType"
        )

        saveAuditLog(auditLog)
    }

    private fun saveAuditLog(auditLog: AuditLog) {
        try {
            auditLogRepository.save(auditLog)
        } catch (e: Exception) {
            // Never let audit logging break the main flow
            logger.error("Failed to save audit log", e)
        }
    }

    private fun getCurrentUserId(): UUID? {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
            (authentication?.principal as? UserPrincipal)?.id
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentIpAddress(): String? {
        return try {
            val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
            request.getHeader("X-Forwarded-For") ?: request.remoteAddr
        } catch (e: Exception) {
            null
        }
    }
}