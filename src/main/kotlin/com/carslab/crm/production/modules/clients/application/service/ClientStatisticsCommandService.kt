package com.carslab.crm.production.modules.clients.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.service.ClientDomainService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
@Transactional
class ClientStatisticsCommandService(
    private val clientDomainService: ClientDomainService,
) {
    
    fun recordVisit(clientId: String) {
        clientDomainService.recordVisit(ClientId(clientId.toLong()))
    }
}