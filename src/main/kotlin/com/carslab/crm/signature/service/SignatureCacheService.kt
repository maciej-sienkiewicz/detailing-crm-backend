// src/main/kotlin/com/carslab/crm/signature/service/SignatureCacheService.kt
package com.carslab.crm.signature.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class SignatureCacheService {

    private val logger = LoggerFactory.getLogger(SignatureCacheService::class.java)

    // Caffeine cache z 10-minutowym TTL
    private val cache: Cache<String, CachedSignatureData> = Caffeine.newBuilder()
        .maximumSize(1000) // Maksymalnie 1000 podpisów w cache
        .expireAfterWrite(Duration.ofMinutes(10)) // Automatyczne usuwanie po 10 minutach
        .removalListener<String, CachedSignatureData> { key, value, cause ->
            logger.info("Signature cache entry removed: key=$key, cause=$cause")
        }
        .recordStats() // Włącz statystyki
        .build()

    /**
     * Zapisz podpis w cache z automatycznym TTL
     */
    fun cacheSignature(sessionId: String, signatureData: CachedSignatureData) {
        cache.put(sessionId, signatureData)
        logger.info("Signature cached for session: $sessionId (expires in 10 minutes)")
    }

    /**
     * Pobierz podpis z cache
     */
    fun getSignature(sessionId: String): CachedSignatureData? {
        val data = cache.getIfPresent(sessionId)
        if (data != null) {
            logger.debug("Signature cache hit for session: $sessionId")
        } else {
            logger.debug("Signature cache miss for session: $sessionId")
        }
        return data
    }

    /**
     * Usuń podpis z cache (po użyciu)
     */
    fun removeSignature(sessionId: String): Boolean {
        val existed = cache.getIfPresent(sessionId) != null
        cache.invalidate(sessionId)
        if (existed) {
            logger.info("Signature manually removed from cache: $sessionId")
            return true
        }
        return false
    }

    /**
     * Sprawdź czy podpis istnieje w cache
     */
    fun hasSignature(sessionId: String): Boolean {
        return cache.getIfPresent(sessionId) != null
    }

    /**
     * Pobierz statystyki cache (Caffeine stats)
     */
    fun getCacheStats(): Map<String, Any> {
        val stats = cache.stats()
        val estimatedSize = cache.estimatedSize()

        return mapOf(
            "estimatedSize" to estimatedSize,
            "hitCount" to stats.hitCount(),
            "missCount" to stats.missCount(),
            "hitRate" to stats.hitRate(),
            "evictionCount" to stats.evictionCount(),
            "loadExceptionCount" to stats.loadFailureCount(),
            "averageLoadPenalty" to stats.averageLoadPenalty(),
            "timestamp" to Instant.now(),
            "ttlMinutes" to 10
        )
    }

    /**
     * Wyczyść cały cache (dla celów administracyjnych)
     */
    fun clearAll() {
        val sizeBefore = cache.estimatedSize()
        cache.invalidateAll()
        logger.info("Cache cleared manually. Removed approximately $sizeBefore entries")
    }

    /**
     * Pobierz wszystkie klucze w cache (dla debugowania)
     */
    fun getAllSessionIds(): Set<String> {
        return cache.asMap().keys
    }

    /**
     * Aktualizuj istniejący wpis w cache (np. dodaj podpis do istniejących danych)
     */
    fun updateSignature(sessionId: String, updater: (CachedSignatureData) -> CachedSignatureData): CachedSignatureData? {
        val existing = cache.getIfPresent(sessionId)
        return if (existing != null) {
            val updated = updater(existing)
            cache.put(sessionId, updated)
            logger.info("Signature updated in cache for session: $sessionId")
            updated
        } else {
            logger.warn("Attempted to update non-existent cache entry: $sessionId")
            null
        }
    }
}


data class CachedSignatureData(
    val sessionId: String,
    val signatureImageBase64: String, // PNG w base64
    val signatureImageBytes: ByteArray, // RAW bytes
    val originalDocumentBytes: ByteArray, // Oryginalny PDF
    val signedAt: Instant,
    val signerName: String,
    val protocolId: Long? = null,
    val tabletId: UUID,
    val companyId: Long,
    val metadata: Map<String, Any> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CachedSignatureData
        return sessionId == other.sessionId
    }

    override fun hashCode(): Int {
        return sessionId.hashCode()
    }
}