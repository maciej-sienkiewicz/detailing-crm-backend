package com.carslab.crm.clients.domain.port

import com.carslab.crm.clients.domain.model.Client
import com.carslab.crm.clients.domain.model.ClientId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ClientRepository {
    fun save(client: Client): Client
    fun findById(id: ClientId): Client?
    fun findByIds(ids: List<ClientId>): List<Client>
    fun findAll(pageable: Pageable): Page<Client>
    fun findByEmail(email: String): Client?
    fun findByPhone(phone: String): Client?
    fun findByEmailOrPhone(email: String?, phone: String?): Client?
    fun searchClients(criteria: ClientSearchCriteria, pageable: Pageable): Page<Client>
    fun existsById(id: ClientId): Boolean
    fun deleteById(id: ClientId): Boolean
    fun count(): Long
}

data class ClientSearchCriteria(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val company: String? = null,
    val hasVehicles: Boolean? = null
)