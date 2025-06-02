package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.clients.domain.model.Client
import com.carslab.crm.clients.domain.model.ClientId
import com.carslab.crm.clients.domain.port.ClientRepository
import com.carslab.crm.clients.domain.port.ClientSearchCriteria
import com.carslab.crm.infrastructure.persistence.entity.ClientEntity
import com.carslab.crm.clients.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class ClientRepositoryAdapter(
    private val clientJpaRepository: ClientJpaRepository,
    private val securityContext: SecurityContext
) : ClientRepository {

    override fun save(client: Client): Client {
        val companyId = securityContext.getCurrentCompanyId()
        val entity = if (client.id.value > 0) {
            val existing = clientJpaRepository.findByIdAndCompanyId(client.id.value, companyId)
                .orElseThrow { IllegalArgumentException("Client not found or access denied") }
            updateEntityFromDomain(existing, client)
            existing
        } else {
            // Create new
            ClientEntity.fromDomain(client, companyId)
        }

        val saved = clientJpaRepository.save(entity)
        return saved.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: ClientId): Client? {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepository.findByIdAndCompanyId(id.value, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByIds(ids: List<ClientId>): List<Client> {
        val companyId = securityContext.getCurrentCompanyId()
        return ids.mapNotNull { id ->
            clientJpaRepository.findByIdAndCompanyId(id.value, companyId)
                .map { it.toDomain() }
                .orElse(null)
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<Client> {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepository.findByCompanyId(companyId, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByEmail(email: String): Client? {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepository.findByEmailAndCompanyId(email, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByPhone(phone: String): Client? {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepository.findByPhoneAndCompanyId(phone, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByEmailOrPhone(email: String?, phone: String?): Client? {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepository.findByEmailOrPhoneAndCompanyId(email, phone, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun searchClients(criteria: ClientSearchCriteria, pageable: Pageable): Page<Client> {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepository.searchClients(
            criteria.name, criteria.email, criteria.phone, criteria.company, companyId, pageable
        ).map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun existsById(id: ClientId): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepository.findByIdAndCompanyId(id.value, companyId).isPresent
    }

    override fun deleteById(id: ClientId): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        val deleted = clientJpaRepository.softDeleteByIdAndCompanyId(id.value, companyId, LocalDateTime.now())
        return deleted > 0
    }

    @Transactional(readOnly = true)
    override fun count(): Long {
        val companyId = securityContext.getCurrentCompanyId()
        return clientJpaRepository.countByCompanyId(companyId)
    }

    private fun updateEntityFromDomain(entity: ClientEntity, domain: Client) {
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