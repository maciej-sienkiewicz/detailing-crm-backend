package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.Client
import com.carslab.crm.domain.model.ClientDetails
import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.port.ClientRepository
import com.carslab.crm.infrastructure.persistence.entity.ClientEntity
import com.carslab.crm.infrastructure.persistence.repository.ClientJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaClientRepositoryAdapter(private val clientJpaRepository: ClientJpaRepository) : ClientRepository {

    override fun save(clientDetails: ClientDetails): ClientDetails {
        val entity = if (clientDetails.id.value > 0) {
            clientJpaRepository.flush()
            val existingEntity = clientJpaRepository.findById(clientDetails.id.value).orElse(null)
                ?: ClientEntity.fromDomain(clientDetails)
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
            ClientEntity.fromDomain(clientDetails)
        }

        clientJpaRepository.flush()
        val savedEntity = clientJpaRepository.save(entity)
        clientJpaRepository.flush()
        return savedEntity.toDomain()
    }

    override fun findById(id: ClientId): ClientDetails? {
        return clientJpaRepository.findById(id.value).map { it.toDomain() }.orElse(null)
    }

    override fun findByIds(ids: List<ClientId>): List<ClientDetails> {
        return clientJpaRepository.findAllById(ids.map { it.value }).map { it.toDomain() }
    }

    override fun findAll(): List<ClientDetails> {
        return clientJpaRepository.findAll().map { it.toDomain() }
    }

    override fun deleteById(id: ClientId): Boolean {
        return if (clientJpaRepository.existsById(id.value)) {
            clientJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }

    override fun findByName(name: String): List<ClientDetails> {
        return clientJpaRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(name, name)
            .map { it.toDomain() }
    }

    override fun findByEmail(email: String): List<ClientDetails> {
        return clientJpaRepository.findByEmailContainingIgnoreCase(email).map { it.toDomain() }
    }

    override fun findByPhone(phone: String): List<ClientDetails> {
        return clientJpaRepository.findByPhoneContaining(phone).map { it.toDomain() }
    }

    override fun findClient(client: Client): ClientDetails? {
        val results = clientJpaRepository.findByEmailOrPhone(client.email, client.phone)
        return results.firstOrNull()?.toDomain()
    }

    override fun findClient(email: String?, phoneNumber: String?): ClientDetails? {
        val results = clientJpaRepository.findByEmailOrPhone(email, phoneNumber)
        return results.firstOrNull()?.toDomain()
    }
}