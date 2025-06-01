package com.carslab.crm.signature.infrastructure.scheduler

import com.carslab.crm.signature.service.SignatureService
import com.carslab.crm.signature.service.TabletPairingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SignatureScheduler(
    private val tabletPairingService: TabletPairingService,
    private val signatureService: SignatureService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    fun cleanupExpiredPairingCodes() {
        try {
            tabletPairingService.cleanupExpiredCodes()
            logger.debug("Cleaned up expired pairing codes")
        } catch (e: Exception) {
            logger.error("Error cleaning up expired pairing codes", e)
        }
    }

    @Scheduled(fixedRate = 60000) // Every 1 minute
    fun cleanupExpiredSignatureSessions() {
        try {
            signatureService.cleanupExpiredSessions()
            logger.debug("Cleaned up expired signature sessions")
        } catch (e: Exception) {
            logger.error("Error cleaning up expired signature sessions", e)
        }
    }
}