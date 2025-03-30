package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.Client
import com.carslab.crm.domain.model.ClientDetails
import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.port.ClientRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementacja repozytorium klientów w pamięci.
 * Użyteczna do testów i wczesnych etapów rozwoju aplikacji.
 */
@Repository
class InMemoryClientRepository : ClientRepository {

    // Używamy ConcurrentHashMap dla thread safety
    private val clients = ConcurrentHashMap<String, ClientDetails>()

    override fun save(client: ClientDetails): ClientDetails {
        clients[client.id.value.toString()] = client
        return client
    }

    override fun findById(id: ClientId): ClientDetails? {
        return clients[id.value.toString()]
    }

    override fun findByIds(ids: List<ClientId>): List<ClientDetails> {
        return ids.mapNotNull { clients[it.value.toString()] }
    }

    override fun findAll(): List<ClientDetails> {
        return clients.values.toList()
    }

    override fun deleteById(id: ClientId): Boolean {
        return clients.remove(id.value.toString()) != null
    }

    override fun findByName(name: String): List<ClientDetails> {
        val lowerName = name.lowercase()
        return clients.values.filter {
            it.fullName.lowercase().contains(lowerName) ||
                    it.firstName.lowercase().contains(lowerName) ||
                    it.lastName.lowercase().contains(lowerName)
        }
    }

    override fun findByEmail(email: String): List<ClientDetails> {
        val lowerEmail = email.lowercase()
        return clients.values.filter {
            it.email.lowercase().contains(lowerEmail)
        }
    }

    override fun findByPhone(phone: String): List<ClientDetails> {
        val cleanedPhone = phone.replace(" ", "")
        return clients.values.filter {
            it.phone.replace(" ", "").contains(cleanedPhone)
        }
    }

    override fun findClient(client: Client): ClientDetails? {
        val lowerEmail = client.email?.lowercase()
        val cleanedPhone = client.phone?.replace(" ", "")
        return clients.values.find { c ->
            lowerEmail?.let { c.email.lowercase().contains(it) } ?: false || cleanedPhone?.let {  c.phone.replace(" ", "").contains(it) } ?: false
        }
    }

    override fun findClient(email: String?, phoneNumber: String?): ClientDetails? {
        val lowerEmail = email?.lowercase()
        val cleanedPhone = phoneNumber?.replace(" ", "")
        return clients.values.find { c ->
            lowerEmail?.let { c.email.lowercase().contains(it) } ?: false || cleanedPhone?.let {  c.phone.replace(" ", "").contains(it) } ?: false
        }
    }
}