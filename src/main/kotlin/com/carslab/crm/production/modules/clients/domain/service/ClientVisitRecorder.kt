package com.carslab.crm.production.modules.clients.domain.service

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.repository.ClientStatisticsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ClientVisitRecorder(
    private val clientStatisticsRepository: ClientStatisticsRepository
) {
    private val logger = LoggerFactory.getLogger(ClientVisitRecorder::class.java)

    fun recordVisit(clientId: ClientId) {
        logger.debug("Recording visit for client: {}", clientId.value)

        clientStatisticsRepository.incrementVisitCount(clientId)
        clientStatisticsRepository.setLastVisitDate(clientId, LocalDateTime.now())

        logger.info("Visit recorded for client: {}", clientId.value)
    }
}