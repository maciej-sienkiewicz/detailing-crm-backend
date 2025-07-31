package com.carslab.crm.modules.finances.infrastructure.service

import com.carslab.crm.signature.service.CachedSignatureData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class InvoiceSignatureCacheService {

    private val logger = LoggerFactory.getLogger(InvoiceSignatureCacheService::class.java)

    private val cache = ConcurrentHashMap<String, CachedInvoiceSignatureData>()
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor()

    init {
        cleanupExecutor.scheduleAtFixedRate(::cleanupExpiredEntries, 5, 5, TimeUnit.MINUTES)
    }

    fun cacheInvoiceSignature(sessionId: String, data: CachedInvoiceSignatureData) {
        logger.debug("Caching invoice signature for session: $sessionId")
        cache[sessionId] = data
    }

    fun getInvoiceSignature(sessionId: String): CachedInvoiceSignatureData? {
        val data = cache[sessionId]
        if (data?.isExpired() == true) {
            cache.remove(sessionId)
            return null
        }
        return data
    }

    fun updateInvoiceSignature(
        sessionId: String,
        updater: (CachedInvoiceSignatureData) -> CachedInvoiceSignatureData
    ): CachedInvoiceSignatureData? {
        val current = cache[sessionId]
        return if (current != null && !current.isExpired()) {
            val updated = updater(current)
            cache[sessionId] = updated
            updated
        } else {
            null
        }
    }

    fun removeInvoiceSignature(sessionId: String) {
        logger.debug("Removing invoice signature from cache: $sessionId")
        cache.remove(sessionId)
    }

    fun getCacheSize(): Int = cache.size

    private fun cleanupExpiredEntries() {
        val now = Instant.now()
        val expiredKeys = cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }

        expiredKeys.forEach { key ->
            cache.remove(key)
            logger.debug("Removed expired invoice signature cache entry: $key")
        }

        if (expiredKeys.isNotEmpty()) {
            logger.info("Cleaned up ${expiredKeys.size} expired invoice signature cache entries")
        }
    }
}

data class CachedInvoiceSignatureData(
    val sessionId: String,
    val invoiceId: String,
    val signatureImageBase64: String,
    val signatureImageBytes: ByteArray,
    val originalInvoiceBytes: ByteArray,
    val signedAt: Instant,
    val signerName: String,
    val tabletId: String,
    val companyId: Long,
    val metadata: Map<String, Any>,
    val cachedAt: Instant = Instant.now(),
    val expiresAt: Instant = Instant.now().plus(2, ChronoUnit.HOURS)
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CachedInvoiceSignatureData

        return sessionId == other.sessionId
    }

    override fun hashCode(): Int {
        return sessionId.hashCode()
    }
}

fun CachedSignatureData.toInvoiceSignatureData(): CachedInvoiceSignatureData {
    return CachedInvoiceSignatureData(
        sessionId = this.sessionId,
        invoiceId = this.metadata["invoiceId"] as? String ?: "",
        signatureImageBase64 = this.signatureImageBase64,
        signatureImageBytes = this.signatureImageBytes,
        originalInvoiceBytes = this.originalDocumentBytes,
        signedAt = this.signedAt,
        signerName = this.signerName,
        tabletId = this.tabletId.toString(),
        companyId = this.companyId,
        metadata = this.metadata
    )
}