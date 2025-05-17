package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.Client
import com.carslab.crm.domain.model.ClientDetails
import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.create.client.CreateClientModel
import com.carslab.crm.domain.port.ClientRepository
import com.carslab.crm.infrastructure.persistence.entity.ClientEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.ClientJpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository

@Repository
class JpaClientRepositoryAdapter(private val clientJpaRepository: ClientJpaRepository) : ClientRepository {

    override fun save(clientDetails: CreateClientModel): ClientDetails {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        clientJpaRepository.flush()
        val entity = ClientEntity.fromDomain(clientDetails)
        entity.companyId = companyId

        val savedEntity = clientJpaRepository.save(entity)
        clientJpaRepository.flush()
        return savedEntity.toDomain()
    }

    override fun updateOrSave(clientDetails: ClientDetails): ClientDetails {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val entity = if (clientDetails.id.value > 0) {
            clientJpaRepository.flush()
            val existingEntity = clientJpaRepository.findByCompanyIdAndId(companyId, clientDetails.id.value)
                .orElse(null) ?: ClientEntity.fromDomain(clientDetails)
            clientJpaRepository.flush()

            // Update fields
            existingEntity.firstName = clientDetails.firstName
            existingEntity.lastName = clientDetails.lastName
            existingEntity.email = clientDetails.email
            existingEntity.phone = clientDetails.phone
            existingEntity.address = clientDetails.address
            existingEntity.company = clientDetails.company
            existingEntity.taxId = clientDetails.taxId
            existingEntity.notes = clientDetails.notes
            existingEntity.updatedAt = clientDetails.audit.updatedAt

            existingEntity
        } else {
            val newEntity = ClientEntity.fromDomain(clientDetails)
            newEntity.companyId = companyId
            newEntity
        }

        clientJpaRepository.flush()
        val savedEntity = clientJpaRepository.save(entity)
        clientJpaRepository.flush()
        return savedEntity.toDomain()
    }

    override fun findById(id: ClientId): ClientDetails? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return clientJpaRepository.findByCompanyIdAndId(companyId, id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByIds(ids: List<ClientId>): List<ClientDetails> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return ids.mapNotNull { id ->
            clientJpaRepository.findByCompanyIdAndId(companyId, id.value)
                .map { it.toDomain() }
                .orElse(null)
        }
    }

    override fun findAll(): List<ClientDetails> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return clientJpaRepository.findByCompanyId(companyId)
            .map { it.toDomain() }
    }

    override fun deleteById(id: ClientId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val entity = clientJpaRepository.findByCompanyIdAndId(companyId, id.value).orElse(null) ?: return false
        clientJpaRepository.delete(entity)
        return true
    }

    override fun findByName(name: String): List<ClientDetails> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return clientJpaRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseAndCompanyId(name, name, companyId)
            .map { it.toDomain() }
    }

    override fun findByEmail(email: String): List<ClientDetails> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return clientJpaRepository.findByEmailContainingIgnoreCaseAndCompanyId(email, companyId)
            .map { it.toDomain() }
    }

    override fun findByPhone(phone: String): List<ClientDetails> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return clientJpaRepository.findByPhoneContainingAndCompanyId(phone, companyId)
            .map { it.toDomain() }
    }

    override fun findClient(client: Client): ClientDetails? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val results = clientJpaRepository.findByEmailOrPhoneAndCompanyId(client.email, client.phone, companyId)
        return results.firstOrNull()?.toDomain()
    }

    override fun findClient(email: String?, phoneNumber: String?): ClientDetails? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val results = clientJpaRepository.findByEmailOrPhoneAndCompanyId(email, phoneNumber, companyId)
        return results.firstOrNull()?.toDomain()
    }
}