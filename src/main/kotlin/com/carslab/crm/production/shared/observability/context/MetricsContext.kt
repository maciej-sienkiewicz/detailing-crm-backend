package com.carslab.crm.production.shared.observability.context

import com.carslab.crm.infrastructure.security.SecurityContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MetricsContext(
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(MetricsContext::class.java)

    companion object {
        const val UNKNOWN_COMPANY = "unknown"
        const val SYSTEM_COMPANY = "system"
        private val COMPANY_ID_CACHE = mutableMapOf<String, String>()
    }

    fun getCurrentCompanyId(): String {
        return try {
            val companyId = securityContext.getCurrentCompanyId()
            companyId.toString()
        } catch (e: Exception) {
            logger.debug("Failed to get company ID from security context: ${e.message}")
            UNKNOWN_COMPANY
        }
    }

    fun getCurrentCompanyIdOrSystem(): String {
        return try {
            val companyId = securityContext.getCurrentCompanyId()
            companyId.toString()
        } catch (e: Exception) {
            SYSTEM_COMPANY
        }
    }

    fun getCompanyIdCached(): String {
        val threadId = Thread.currentThread().id.toString()
        return COMPANY_ID_CACHE.getOrPut(threadId) {
            getCurrentCompanyId()
        }
    }

    fun clearCache() {
        COMPANY_ID_CACHE.clear()
    }

    fun isValidCompanyId(companyId: String?): Boolean {
        return !companyId.isNullOrBlank() &&
                companyId != UNKNOWN_COMPANY &&
                companyId.matches(Regex("\\d+"))
    }

    fun sanitizeCompanyId(companyId: Long?): String {
        return when {
            companyId == null -> UNKNOWN_COMPANY
            companyId <= 0 -> UNKNOWN_COMPANY
            companyId > 999999 -> "large_tenant"
            else -> companyId.toString()
        }
    }
}