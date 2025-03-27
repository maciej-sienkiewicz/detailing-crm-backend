package com.carslab.crm.domain.port

import com.carslab.crm.domain.model.Client
import com.carslab.crm.domain.model.ClientDetails
import com.carslab.crm.domain.model.ClientId

/**
 * Interfejs repozytorium klientów.
 */
interface ClientRepository {
    /**
     * Zapisuje klienta w repozytorium.
     */
    fun save(ClientDetails: ClientDetails): ClientDetails

    /**
     * Znajduje klienta po ID.
     */
    fun findById(id: ClientId): ClientDetails?

    /**
     * Zwraca wszystkich klientów.
     */
    fun findAll(): List<ClientDetails>

    /**
     * Usuwa klienta po ID.
     * @return true, jeśli klient został usunięty, false w przeciwnym razie
     */
    fun deleteById(id: ClientId): Boolean

    /**
     * Wyszukuje klientów po imieniu lub nazwisku.
     */
    fun findByName(name: String): List<ClientDetails>

    /**
     * Wyszukuje klientów po adresie email.
     */
    fun findByEmail(email: String): List<ClientDetails>

    /**
     * Wyszukuje klientów po numerze telefonu.
     */
    fun findByPhone(phone: String): List<ClientDetails>

    fun findClient(client: Client): ClientDetails?
}