package com.carslab.crm.production.modules.clients.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.clients.application.dto.ClientWithStatisticsResponse
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.repository.ClientRepository
import com.carslab.crm.production.modules.clients.domain.repository.ClientSearchCriteria
import com.carslab.crm.production.modules.clients.domain.repository.ClientStatisticsRepository
import com.carslab.crm.production.modules.clients.domain.service.ClientDomainService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ClientQueryService(
    private val clientRepository: ClientRepository,
    private val clientStatisticsRepository: ClientStatisticsRepository,
    private val clientDomainService: ClientDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ClientQueryService::class.java)

    fun getClientsForCurrentCompany(pageable: Pageable): Page<ClientResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching clients for company: {}", companyId)

        val clients = clientRepository.findByCompanyId(companyId, pageable)
        logger.debug("Found {} clients for company: {}", clients.numberOfElements, companyId)

        return clients.map { ClientResponse.from(it) }
    }

    fun getClient(clientId: String): ClientWithStatisticsResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching client: {} for company: {}", clientId, companyId)

        val client = clientDomainService.getClientForCompany(ClientId.of(clientId.toLong()), companyId)
        val statistics = clientStatisticsRepository.findByClientId(client.id)

        logger.debug("Client found: {}", client.fullName)

        return ClientWithStatisticsResponse.from(client, statistics)
    }

    fun searchClients(
        name: String?,
        email: String?,
        phone: String?,
        company: String?,
        hasVehicles: Boolean?,
        minTotalRevenue: Double?,
        maxTotalRevenue: Double?,
        minVisits: Int?,
        maxVisits: Int?,
        pageable: Pageable
    ): Page<ClientResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Searching clients for company: {} with criteria", companyId)

        val searchCriteria = ClientSearchCriteria(
            name = name,
            email = email,
            phone = phone,
            company = company,
            hasVehicles = hasVehicles,
            minTotalRevenue = minTotalRevenue,
            maxTotalRevenue = maxTotalRevenue,
            minVisits = minVisits,
            maxVisits = maxVisits
        )

        val clients = clientRepository.searchClients(companyId, searchCriteria, pageable)
        logger.debug("Found {} clients matching criteria for company: {}", clients.numberOfElements, companyId)

        return clients.map { ClientResponse.from(it) }
    }
    
    fun findByIds(clientIds: List<ClientId>): List<ClientResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Finding clients by IDs for company: {} with IDs: {}", companyId, clientIds)

        val clients = clientDomainService.findByIds(clientIds.map { ClientId(it) }, companyId)

        logger.debug("Found {} clients by IDs for company: {}", clients.size, companyId)

        return clients.map { ClientResponse.from(it) }
    }
}