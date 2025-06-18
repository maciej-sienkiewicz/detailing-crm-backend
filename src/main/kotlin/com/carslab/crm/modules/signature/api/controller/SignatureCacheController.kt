// src/main/kotlin/com/carslab/crm/signature/api/controller/SignatureCacheController.kt
package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.signature.service.ProtocolSignatureService
import com.carslab.crm.signature.service.SignatureCacheService
import com.carslab.crm.signature.service.SignatureException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/signatures")
class SignatureCacheController(
    private val protocolSignatureService: ProtocolSignatureService,
    private val signatureCacheService: SignatureCacheService,
    private val securityContext: SecurityContext
) : BaseController() {

    /**
     * Pobierz podpisany dokument z cache
     */
    @GetMapping("/{sessionId}/sign/{visitId}")
    fun getSignedDocument(
        @PathVariable sessionId: String,
    ): ResponseEntity<ByteArray> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val signedDocument = protocolSignatureService.getSignedDocument(sessionId, companyId)

            if (signedDocument != null) {
                ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"signed-protocol-${sessionId}.pdf\"")
                    .body(signedDocument)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error downloading signed document for session $sessionId", e)
            throw SignatureException("Failed to download signed document", e)
        }
    }

    /**
     * Pobierz obraz podpisu z cache
     */
    @GetMapping("/{sessionId}/signature-image")
    fun getSignatureImage(
        @PathVariable sessionId: String
    ): ResponseEntity<ByteArray> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val signatureImage = protocolSignatureService.getSignatureImage(sessionId, companyId)

            if (signatureImage != null) {
                ResponseEntity.ok()
                    .header("Content-Type", "image/png")
                    .header("Content-Disposition", "inline; filename=\"signature-${sessionId}.png\"")
                    .body(signatureImage)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error downloading signature image for session $sessionId", e)
            throw SignatureException("Failed to download signature image", e)
        }
    }

    /**
     * Sprawdź status podpisu w cache
     */
    @GetMapping("/{sessionId}/status")
    fun getSignatureStatus(
        @PathVariable sessionId: String
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val status = protocolSignatureService.getSignatureStatus(sessionId, companyId)
            ok(status)
        } catch (e: Exception) {
            logger.error("Error getting signature status for session $sessionId", e)
            throw SignatureException("Failed to get signature status", e)
        }
    }

    /**
     * Usuń podpis z cache (po pobraniu)
     */
    @DeleteMapping("/{sessionId}/cache")
    fun clearSignatureCache(
        @PathVariable sessionId: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val removed = signatureCacheService.removeSignature(sessionId)

            ok(mapOf(
                "success" to removed,
                "sessionId" to sessionId,
                "message" to if (removed) "Signature cache cleared" else "Signature not found in cache",
                "timestamp" to java.time.Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error clearing signature cache for session $sessionId", e)
            throw SignatureException("Failed to clear signature cache", e)
        }
    }

    /**
     * Pobierz statystyki cache
     */
    @GetMapping("/cache/stats")
    fun getCacheStats(): ResponseEntity<Map<String, Any>> {
        return try {
            val stats = signatureCacheService.getCacheStats()
            ok(stats)
        } catch (e: Exception) {
            logger.error("Error getting cache stats", e)
            throw SignatureException("Failed to get cache stats", e)
        }
    }

    /**
     * Pobierz wszystkie aktywne sesje w cache (admin endpoint)
     */
    @GetMapping("/cache/sessions")
    fun getActiveSessions(): ResponseEntity<Map<String, Any>> {
        return try {
            val sessionIds = signatureCacheService.getAllSessionIds()
            ok(mapOf(
                "activeSessions" to sessionIds,
                "count" to sessionIds.size,
                "timestamp" to java.time.Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error getting active sessions", e)
            throw SignatureException("Failed to get active sessions", e)
        }
    }

    /**
     * Wyczyść cały cache (admin endpoint)
     */
    @DeleteMapping("/cache/clear-all")
    fun clearAllCache(): ResponseEntity<Map<String, Any>> {
        return try {
            signatureCacheService.clearAll()
            ok(mapOf(
                "success" to true,
                "message" to "All cache cleared",
                "timestamp" to java.time.Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error clearing all cache", e)
            throw SignatureException("Failed to clear cache", e)
        }
    }
}