package com.carslab.crm.production.modules.clients.domain.repository

import com.carslab.crm.production.modules.clients.domain.model.Client
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.model.ClientWithStatistics
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ClientRepository {
    fun save(client: Client): Client
    fun findById(id: ClientId): Client?
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Client>
    fun findByEmail(email: String, companyId: Long): Client?
    fun findByPhone(phone: String, companyId: Long): Client?
    fun existsByEmail(email: String, companyId: Long): Boolean
    fun existsByPhone(phone: String, companyId: Long): Boolean
    fun deleteById(id: ClientId): Boolean
    fun searchClients(
        companyId: Long,
        searchCriteria: ClientSearchCriteria,
        pageable: Pageable
    ): Page<Client>
    fun searchClientsWithStatistics(
        companyId: Long,
        searchCriteria: ClientSearchCriteria,
        pageable: Pageable
    ): Page<ClientWithStatistics>
    fun findByIds(ids: List<ClientId>, companyId: Long): List<Client>
}

data class ClientSearchCriteria(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val company: String? = null,
    val hasVehicles: Boolean? = null,
    val minTotalRevenue: Double? = null,
    val maxTotalRevenue: Double? = null,
    val minVisits: Int? = null,
    val maxVisits: Int? = null
)