package com.carslab.crm.audit.service

import com.carslab.crm.audit.repository.AuditLogRepository
import com.carslab.crm.audit.entity.AuditLog
import com.carslab.crm.audit.entity.AuditSeverity
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant
import java.util.*
import jakarta.servlet.http.HttpServletRequest

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository
) {

    // Tablet audit methods
    fun logTabletConnection(deviceId: UUID, tenantId: UUID, action: String) {
        val (ipAddress, userAgent) = extractRequestInfo()
        val auditLog = AuditLog(
            tenantId = tenantId,
            userId = null,
            action = "TABLET_$action",
            resource = "tablet",
            resourceId = deviceId.toString(),
            details = "Tablet connection: $action",
            ipAddress = ipAddress,
            userAgent = userAgent,
            timestamp = Instant.now()
        )
        auditLogRepository.save(auditLog)
    }

    fun logSignatureRequest(tenantId: UUID, sessionId: String, action: String) {
        val (ipAddress, userAgent) = extractRequestInfo()
        val auditLog = AuditLog(
            tenantId = tenantId,
            userId = null,
            action = "SIGNATURE_REQUEST_$action",
            resource = "signature_session",
            resourceId = sessionId,
            details = "Signature request: $action",
            ipAddress = ipAddress,
            userAgent = userAgent,
            timestamp = Instant.now()
        )
        auditLogRepository.save(auditLog)
    }

    fun logSignatureAcknowledgment(deviceId: UUID, sessionId: String, success: Boolean) {
        val (ipAddress, userAgent) = extractRequestInfo()
        val auditLog = AuditLog(
            tenantId = null,
            userId = null,
            action = "SIGNATURE_ACKNOWLEDGMENT",
            resource = "signature_session",
            resourceId = sessionId,
            details = "Signature acknowledged by device $deviceId: success=$success",
            ipAddress = ipAddress,
            userAgent = userAgent,
            timestamp = Instant.now()
        )
        auditLogRepository.save(auditLog)
    }

    // Workstation audit methods - overloaded for different parameter types

    // UUID workstationId, UUID companyId, UUID userId
    fun logWorkstationConnection(workstationId: UUID, companyId: UUID, userId: UUID, action: String) {
        logWorkstationConnectionInternal(workstationId, companyId.toString(), userId.toString(), action)
    }

    // UUID workstationId, Long companyId, Long userId
    fun logWorkstationConnection(workstationId: UUID, companyId: Long, userId: Long, action: String) {
        logWorkstationConnectionInternal(workstationId, companyId.toString(), userId.toString(), action)
    }

    // UUID workstationId, String companyId, String userId
    fun logWorkstationConnection(workstationId: UUID, companyId: String, userId: String, action: String) {
        logWorkstationConnectionInternal(workstationId, companyId, userId, action)
    }

    // UUID workstationId, UUID companyId
    fun logWorkstationNotification(workstationId: UUID, companyId: UUID, action: String) {
        logWorkstationNotificationInternal(workstationId, companyId.toString(), action)
    }

    // UUID workstationId, Long companyId
    fun logWorkstationNotification(workstationId: UUID, companyId: Long, action: String) {
        logWorkstationNotificationInternal(workstationId, companyId.toString(), action)
    }

    // UUID workstationId, String companyId
    fun logWorkstationNotification(workstationId: UUID, companyId: String, action: String) {
        logWorkstationNotificationInternal(workstationId, companyId, action)
    }

    // Internal implementation methods
    private fun logWorkstationConnectionInternal(workstationId: UUID, companyId: String, userId: String, action: String) {
        val (ipAddress, userAgent) = extractRequestInfo()
        val auditLog = AuditLog(
            tenantId = null,
            userId = tryConvertToUUID(userId),
            action = "WORKSTATION_$action",
            resource = "workstation",
            resourceId = workstationId.toString(),
            details = "Workstation connection by user $userId from company $companyId: $action",
            ipAddress = ipAddress,
            userAgent = userAgent,
            timestamp = Instant.now()
        )
        auditLogRepository.save(auditLog)
    }

    private fun logWorkstationNotificationInternal(workstationId: UUID, companyId: String, action: String) {
        val (ipAddress, userAgent) = extractRequestInfo()
        val auditLog = AuditLog(
            tenantId = null,
            userId = null,
            action = "WORKSTATION_NOTIFICATION",
            resource = "workstation",
            resourceId = workstationId.toString(),
            details = "Workstation notification to company $companyId: $action",
            ipAddress = ipAddress,
            userAgent = userAgent,
            timestamp = Instant.now()
        )
        auditLogRepository.save(auditLog)
    }

    // Security audit methods
    fun logSecurityViolation(action: String, ipAddress: String?, details: String?) {
        val (extractedIp, userAgent) = extractRequestInfo()
        val auditLog = AuditLog(
            tenantId = null,
            userId = null,
            action = "SECURITY_VIOLATION",
            resource = "security",
            resourceId = null,
            details = details,
            ipAddress = ipAddress ?: extractedIp, // Use provided IP or extract from request
            userAgent = userAgent,
            timestamp = Instant.now(),
            severity = AuditSeverity.CRITICAL
        )
        auditLogRepository.save(auditLog)
    }

    // General audit methods
    fun logUserAction(userId: Any, action: String, resource: String, details: String? = null) {
        val (ipAddress, userAgent) = extractRequestInfo()
        val auditLog = AuditLog(
            tenantId = null,
            userId = tryConvertToUUID(userId),
            action = action,
            resource = resource,
            resourceId = null,
            details = details,
            ipAddress = ipAddress,
            userAgent = userAgent,
            timestamp = Instant.now()
        )
        auditLogRepository.save(auditLog)
    }

    fun logSystemEvent(action: String, details: String? = null) {
        val (ipAddress, userAgent) = extractRequestInfo()
        val auditLog = AuditLog(
            tenantId = null,
            userId = null,
            action = "SYSTEM_$action",
            resource = "system",
            resourceId = null,
            details = details,
            ipAddress = ipAddress,
            userAgent = userAgent,
            timestamp = Instant.now()
        )
        auditLogRepository.save(auditLog)
    }

    // Helper methods
    private fun extractRequestInfo(): Pair<String?, String?> {
        return try {
            val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            val request = requestAttributes?.request

            val ipAddress = request?.let { extractClientIpAddress(it) }
            val userAgent = request?.getHeader("User-Agent")

            Pair(ipAddress, userAgent)
        } catch (e: Exception) {
            // Not in a web request context (e.g., async task, scheduled job)
            Pair(null, null)
        }
    }

    private fun extractClientIpAddress(request: HttpServletRequest): String? {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr
    }

    private fun tryConvertToUUID(value: Any?): UUID? {
        return when (value) {
            null -> null
            is UUID -> value
            is String -> try {
                UUID.fromString(value)
            } catch (e: Exception) {
                null
            }
            is Long -> try {
                // Create a deterministic UUID from Long
                UUID.fromString("${String.format("%08d", value)}-0000-0000-0000-000000000000")
            } catch (e: Exception) {
                null
            }
            is Int -> try {
                UUID.fromString("${String.format("%08d", value)}-0000-0000-0000-000000000000")
            } catch (e: Exception) {
                null
            }
            else -> null
        }
    }
}