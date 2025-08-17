package com.carslab.crm.production.modules.clients.infrastructure.repository

import com.carslab.crm.production.modules.clients.domain.model.Client
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.model.ClientWithStatistics
import com.carslab.crm.production.modules.clients.domain.repository.ClientRepository
import com.carslab.crm.production.modules.clients.domain.repository.ClientSearchCriteria
import com.carslab.crm.production.modules.clients.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.clients.infrastructure.mapper.toEntity
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class ClientRepositoryImpl(
    private val jpaRepository: ClientJpaRepository
) : ClientRepository {

    private val logger = LoggerFactory.getLogger(ClientRepositoryImpl::class.java)

    override fun save(client: Client): Client {
        logger.debug("Saving client: {} for company: {}", client.id.value, client.companyId)

        val entity = client.toEntity()
        val savedEntity = jpaRepository.save(entity)

        logger.debug("Client saved: {}", savedEntity.id)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: ClientId): Client? {
        logger.debug("Finding client by ID: {}", id.value)

        val result = jpaRepository.findById(id.value)
            .filter { it.active }
            .map { it.toDomain() }
            .orElse(null)

        if (result == null) {
            logger.debug("Client not found: {}", id.value)
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Client> {
        logger.debug("Finding clients for company: {}", companyId)

        val entities = jpaRepository.findByCompanyIdAndActiveTrue(companyId, pageable)
        val clients = entities.map { it.toDomain() }

        logger.debug("Found {} clients for company: {}", clients.numberOfElements, companyId)
        return clients
    }

    @Transactional(readOnly = true)
    override fun findByEmail(email: String, companyId: Long): Client? {
        logger.debug("Finding client by email: {} for company: {}", email, companyId)

        return jpaRepository.findByEmailAndCompanyIdAndActiveTrue(email, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByPhone(phone: String, companyId: Long): Client? {
        logger.debug("Finding client by phone: {} for company: {}", phone, companyId)

        return jpaRepository.findByPhoneAndCompanyIdAndActiveTrue(phone, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun existsByEmail(email: String, companyId: Long): Boolean {
        return jpaRepository.existsByEmailAndCompanyIdAndActiveTrue(email, companyId)
    }

    @Transactional(readOnly = true)
    override fun existsByPhone(phone: String, companyId: Long): Boolean {
        return jpaRepository.existsByPhoneAndCompanyIdAndActiveTrue(phone, companyId)
    }

    override fun deleteById(id: ClientId): Boolean {
        logger.debug("Soft deleting client: {}", id.value)

        return try {
            if (jpaRepository.existsById(id.value)) {
                jpaRepository.softDeleteByIdAndCompanyId(id.value, 0L, LocalDateTime.now())
                logger.debug("Client soft deleted: {}", id.value)
                true
            } else {
                logger.debug("Client not found for deletion: {}", id.value)
                false
            }
        } catch (e: Exception) {
            logger.error("Error deleting client: {}", id.value, e)
            false
        }
    }

    @Transactional(readOnly = true)
    override fun searchClients(
        companyId: Long,
        searchCriteria: ClientSearchCriteria,
        pageable: Pageable
    ): Page<Client> {
        logger.debug("Searching clients for company: {} with criteria", companyId)

        val offset = pageable.pageNumber * pageable.pageSize
        val limit = pageable.pageSize

        val entities = jpaRepository.searchClients(
            companyId = companyId,
            name = searchCriteria.name,
            email = searchCriteria.email,
            phone = searchCriteria.phone,
            company = searchCriteria.company,
            hasVehicles = searchCriteria.hasVehicles,
            minTotalRevenue = searchCriteria.minTotalRevenue,
            maxTotalRevenue = searchCriteria.maxTotalRevenue,
            minVisits = searchCriteria.minVisits,
            maxVisits = searchCriteria.maxVisits,
            limit = limit,
            offset = offset
        )

        val totalCount = jpaRepository.countSearchClients(
            companyId = companyId,
            name = searchCriteria.name,
            email = searchCriteria.email,
            phone = searchCriteria.phone,
            company = searchCriteria.company,
            hasVehicles = searchCriteria.hasVehicles,
            minTotalRevenue = searchCriteria.minTotalRevenue,
            maxTotalRevenue = searchCriteria.maxTotalRevenue,
            minVisits = searchCriteria.minVisits,
            maxVisits = searchCriteria.maxVisits
        )

        val clients = entities.map { it.toDomain() }
        logger.debug("Found {} clients matching criteria for company: {}", clients.size, companyId)

        return PageImpl(clients, pageable, totalCount)
    }

    @Transactional(readOnly = true)
    override fun searchClientsWithStatistics(
        companyId: Long,
        searchCriteria: ClientSearchCriteria,
        pageable: Pageable
    ): Page<ClientWithStatistics> {
        logger.debug("Searching clients with statistics for company: {} with criteria", companyId)

        val offset = pageable.pageNumber * pageable.pageSize
        val limit = pageable.pageSize

        val rawResults = jpaRepository.searchClientsWithStatisticsNative(
            companyId = companyId,
            name = searchCriteria.name,
            email = searchCriteria.email,
            phone = searchCriteria.phone,
            company = searchCriteria.company,
            hasVehicles = searchCriteria.hasVehicles,
            minTotalRevenue = searchCriteria.minTotalRevenue,
            maxTotalRevenue = searchCriteria.maxTotalRevenue,
            minVisits = searchCriteria.minVisits,
            maxVisits = searchCriteria.maxVisits,
            limit = limit,
            offset = offset
        )

        val totalCount = jpaRepository.countSearchClientsWithStatistics(
            companyId = companyId,
            name = searchCriteria.name,
            email = searchCriteria.email,
            phone = searchCriteria.phone,
            company = searchCriteria.company,
            hasVehicles = searchCriteria.hasVehicles,
            minTotalRevenue = searchCriteria.minTotalRevenue,
            maxTotalRevenue = searchCriteria.maxTotalRevenue,
            minVisits = searchCriteria.minVisits,
            maxVisits = searchCriteria.maxVisits
        )

        val clientsWithStats = rawResults.map { it.toDomain() }
        logger.debug("Found {} clients with statistics for company: {}", clientsWithStats.size, companyId)

        return PageImpl(clientsWithStats, pageable, totalCount)
    }

    override fun findByIds(ids: List<ClientId>, companyId: Long): List<Client> {
        if (ids.isEmpty()) return emptyList()

        logger.debug("Finding {} clients by IDs", ids.size)

        val entityIds = ids.map { it.value }
        val entities = jpaRepository.findByIdInAndCompanyIdAndActiveTrue(entityIds, companyId)
        val clients = entities.map { it.toDomain() }

        logger.debug("Found {} clients out of {} requested", clients.size, ids.size)
        return clients
    }
}