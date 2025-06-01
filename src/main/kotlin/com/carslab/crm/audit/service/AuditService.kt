package com.carslab.crm.audit.service

import com.carslab.crm.audit.entity.AuditLog
import com.carslab.crm.audit.entity.AuditSeverity
import com.carslab.crm.audit.repository.AuditLogRepository
import com.carslab.crm.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID

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