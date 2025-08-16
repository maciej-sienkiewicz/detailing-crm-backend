package com.carslab.crm.modules.visits.domain.services

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class VisitValidationServiceDeprecated(
    private val visitJpaRepository: ProtocolJpaRepository,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitValidationServiceDeprecated::class.java)

    @Cacheable(
        value = ["visit-validation"],
        key = "#visitId.value + ':' + @securityContext.getCurrentCompanyId()"
    )
    fun validateVisitAccess(visitId: ProtocolId): VisitValidationResult {
        val currentCompanyId = securityContext.getCurrentCompanyId()

        logger.debug("Validating protocol access: {} for company: {}", visitId.value, currentCompanyId)

        return performDatabaseValidation(visitId, currentCompanyId)
    }

    /**
     * Fast company-only validation for less critical operations
     */
    @Cacheable(
        value = ["visitId-company"],
        key = "#visitId.value + ':' + @securityContext.getCurrentCompanyId()"
    )
    fun validateProtocolCompany(visitId: ProtocolId): Boolean {
        val currentCompanyId = securityContext.getCurrentCompanyId()

        logger.debug("Validating protocol company: {} for company: {}", visitId.value, currentCompanyId)

        return visitJpaRepository.findByCompanyIdAndId(id = visitId.value.toLong(), companyId = currentCompanyId).isPresent
    }
    
    /**
     * Cache invalidation when protocol is modified
     */
    @CacheEvict(
        value = ["visit-validation", "visit-company"],
        key = "#visitId.value + ':' + (#companyId ?: @securityContext.getCurrentCompanyId())"
    )
    fun invalidateProtocolCache(visitId: ProtocolId, companyId: Long? = null) {
        logger.debug("Invalidated cache for visit: {}", visitId.value)
    }
    
    @CacheEvict(value = ["visit-validation", "visit-company"], allEntries = true)
    fun clearAllCaches() {
        logger.info("Cleared all visit validation caches")
    }

    // Helper methods

    private fun getCachedValidationResult(visitId: ProtocolId): VisitValidationResult? {
        return try {
            // This will use the @Cacheable annotation
            validateVisitAccess(visitId)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun performDatabaseValidation(visitId: ProtocolId, companyId: Long): VisitValidationResult {
        return try {
            // Single optimized query
            val visit = visitJpaRepository.findByCompanyIdAndId(companyId, visitId.value.toLong())
                .orElse(null)

            when {
                visit == null -> {
                    logger.warn("Visit not found: {} for company: {}", visitId.value, companyId)
                    VisitValidationResult.notFound(visitId.value)
                }
                visit.companyId != companyId -> {
                    logger.warn("Access denied to visit: {} for company: {}", visitId.value, companyId)
                    VisitValidationResult.accessDenied(visitId.value, companyId)
                }
                else -> {
                    logger.debug("Visit validation successful: {}", visitId.value)
                    VisitValidationResult.valid(visitId.value, companyId, visit.status.name)
                }
            }
        } catch (e: Exception) {
            logger.error("Database validation failed for visit: {}", visitId.value, e)
            VisitValidationResult.error(visitId.value, e.message ?: "Database error")
        }
    }
}

/**
 * Simple validation result with basic expiration check
 */
data class VisitValidationResult(
    val visitId: String,
    val isValid: Boolean,
    val companyId: Long?,
    val status: String?,
    val errorMessage: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun throwIfInvalid() {
        if (!isValid) {
            throw when {
                errorMessage?.contains("not found") == true ->
                    IllegalArgumentException("Visit not found: $visitId")
                errorMessage?.contains("access denied") == true ->
                    SecurityException("Access denied to visit: $visitId")
                else ->
                    RuntimeException(errorMessage ?: "Visit validation failed: $visitId")
            }
        }
    }

    companion object {
        fun valid(visitId: String, companyId: Long, status: String) =
            VisitValidationResult(visitId, true, companyId, status, null)

        fun notFound(visitId: String) =
            VisitValidationResult(visitId, false, null, null, "Visit not found")

        fun accessDenied(visitId: String, companyId: Long) =
            VisitValidationResult(visitId, false, companyId, null, "Access denied for company: $companyId")

        fun error(visitId: String, message: String) =
            VisitValidationResult(visitId, false, null, null, message)
    }
}