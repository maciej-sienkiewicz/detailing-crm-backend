package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.modules.clients.domain.model.Client
import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.CreateClient
import com.carslab.crm.modules.clients.domain.port.ClientRepositoryDeprecated
import com.carslab.crm.modules.clients.domain.port.ClientSearchCriteria
import com.carslab.crm.modules.clients.infrastructure.persistence.entity.ClientEntityDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.ClientJpaRepositoryDeprecated
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class ClientRepositoryAdapter(
    private val clientJpaRepositoryDeprecated: ClientJpaRepositoryDeprecated,
    private val securityContext: SecurityContext
) : ClientRepositoryDeprecated {

    override fun save(client: Client): Client {
        val companyId = securityContext.getCurrentCompanyId()
        
        val existingEntity = clientJpaRepositoryDeprecated.findByIdAndCompanyId(client.id.value, companyId)
            .orElseThrow { IllegalArgumentException("Client not found or access denied") }

        updateEntityFromDomain(existingEntity, client)

        val saved = clientJpaRepositoryDeprecated.save(existingEntity)
        return saved.toDomain()
    }

    override fun saveNew(client: CreateClient): Client {
        val companyId = securityContext.getCurrentCompanyId()
        val entity = ClientEntityDeprecated.fromDomain(client, companyId)

        val saved = clientJpaRepositoryDeprecated.save(entity)
        return saved.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: ClientId): Client? {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepositoryDeprecated.findByIdAndCompanyId(id.value, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByIds(ids: List<ClientId>): List<Client> {
        val companyId = securityContext.getCurrentCompanyId()
        return ids.mapNotNull { id ->
            clientJpaRepositoryDeprecated.findByIdAndCompanyId(id.value, companyId)
                .map { it.toDomain() }
                .orElse(null)
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<Client> {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepositoryDeprecated.findByCompanyId(companyId, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByEmail(email: String): Client? {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepositoryDeprecated.findByEmailAndCompanyId(email, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByPhone(phone: String): Client? {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepositoryDeprecated.findByPhoneAndCompanyId(phone, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByEmailOrPhone(email: String?, phone: String?): Client? {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepositoryDeprecated.findByEmailOrPhoneAndCompanyId(email, phone, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun searchClients(criteria: ClientSearchCriteria, pageable: Pageable): Page<Client> {
        val companyId = securityContext.getCurrentCompanyId()

        // Normalize search criteria
        val normalizedCriteria = normalizeCriteria(criteria)

        val offset = pageable.pageNumber * pageable.pageSize
        val limit = pageable.pageSize

        val clients = clientJpaRepositoryDeprecated.searchClientsNative(
            normalizedCriteria.name,
            normalizedCriteria.email,
            normalizedCriteria.phone,
            normalizedCriteria.company,
            normalizedCriteria.hasVehicles,
            normalizedCriteria.minTotalRevenue,
            normalizedCriteria.maxTotalRevenue,
            normalizedCriteria.minVisits,
            normalizedCriteria.maxVisits,
            companyId,
            limit,
            offset
        ).map { it.toDomain() }

        val total = clientJpaRepositoryDeprecated.countSearchClients(
            normalizedCriteria.name,
            normalizedCriteria.email,
            normalizedCriteria.phone,
            normalizedCriteria.company,
            normalizedCriteria.hasVehicles,
            normalizedCriteria.minTotalRevenue,
            normalizedCriteria.maxTotalRevenue,
            normalizedCriteria.minVisits,
            normalizedCriteria.maxVisits,
            companyId
        )

        return PageImpl(clients, pageable, total)
    }

    /**
     * Normalizes search criteria for better matching:
     * - Trims whitespace
     * - Converts to lowercase for case-insensitive search
     * - Normalizes phone numbers by removing spaces and country codes
     */
    private fun normalizeCriteria(criteria: ClientSearchCriteria): ClientSearchCriteria {
        return ClientSearchCriteria(
            name = criteria.name?.trim()?.takeIf { it.isNotBlank() }?.lowercase(),
            email = criteria.email?.trim()?.takeIf { it.isNotBlank() }?.lowercase(),
            phone = criteria.phone?.let { normalizePhoneNumber(it) },
            company = criteria.company?.trim()?.takeIf { it.isNotBlank() }?.lowercase(),
            hasVehicles = criteria.hasVehicles,
            minTotalRevenue = criteria.minTotalRevenue,
            maxTotalRevenue = criteria.maxTotalRevenue,
            minVisits = criteria.minVisits,
            maxVisits = criteria.maxVisits
        )
    }

    /**
     * Normalizes phone number by:
     * - Removing all spaces
     * - Removing common country codes (+48, 0048, 48)
     * - Keeping only digits
     */
    private fun normalizePhoneNumber(phone: String): String? {
        val trimmed = phone.trim()
        if (trimmed.isBlank()) return null

        // Remove all non-digit characters except +
        val digitsOnly = trimmed.replace(Regex("[^+\\d]"), "")

        // Remove Polish country codes
        return when {
            digitsOnly.startsWith("+48") -> digitsOnly.substring(3)
            digitsOnly.startsWith("0048") -> digitsOnly.substring(4)
            digitsOnly.startsWith("48") && digitsOnly.length > 9 -> digitsOnly.substring(2)
            digitsOnly.startsWith("+") -> digitsOnly.substring(1)
            else -> digitsOnly
        }.takeIf { it.isNotBlank() }
    }
    
    @Transactional(readOnly = true)
    override fun existsById(id: ClientId): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepositoryDeprecated.findByIdAndCompanyId(id.value, companyId).isPresent
    }

    override fun deleteById(id: ClientId): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        val deleted = clientJpaRepositoryDeprecated.softDeleteByIdAndCompanyId(id.value, companyId, LocalDateTime.now())
        return deleted > 0
    }

    @Transactional(readOnly = true)
    override fun count(): Long {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepositoryDeprecated.countByCompanyId(companyId)
    }

    private fun updateEntityFromDomain(entity: ClientEntityDeprecated, domain: Client) {
        entity.firstName = domain.firstName
        entity.lastName = domain.lastName
        entity.email = domain.email
        entity.phone = domain.phone
        entity.address = domain.address
        entity.company = domain.company
        entity.taxId = domain.taxId
        entity.notes = domain.notes
        entity.updatedAt = domain.audit.updatedAt
        entity.updatedBy = domain.audit.updatedBy
    }
}