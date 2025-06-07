package com.carslab.crm.clients.domain.port

import com.carslab.crm.clients.domain.model.ClientId
import com.carslab.crm.clients.domain.model.VehicleId

/**
 * Port repozytorium dla asocjacji pomiędzy klientami a pojazdami.
 */
interface ClientVehicleRepository {
    /**
     * Znajduje pojazdy dla danego klienta.
     */
    fun findVehiclesByClientId(clientId: ClientId): List<VehicleId>

    /**
     * Znajduje właścicieli dla danego pojazdu.
     */
    fun findOwnersByVehicleId(vehicleId: VehicleId): List<ClientId>

    /**
     * Znajduje pojazdy dla listy klientów.
     */
    fun findVehiclesByOwnerIds(ownerIds: List<ClientId>): Map<ClientId, List<VehicleId>>

    /**
     * Tworzy nową asocjację pomiędzy pojazdem a klientem.
     */
    fun newAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean

    /**
     * Usuwa asocjację pomiędzy pojazdem a klientem.
     */
    fun removeAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean

    /**
     * Sprawdza czy istnieje asocjacja pomiędzy pojazdem a klientem.
     */
    fun hasAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean

    /**
     * Liczy ilość właścicieli dla danego pojazdu.
     */
    fun countOwnersForVehicle(vehicleId: VehicleId): Int
}