package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.Vehicle
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.port.VehicleRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementacja repozytorium pojazdów przechowująca dane w pamięci.
 * Używana głównie do testów i rozwoju aplikacji.
 */
@Repository
class InMemoryVehicleRepository : VehicleRepository {

    // Mapa przechowująca pojazdy z kluczem jako ID pojazdu
    private val vehicles = ConcurrentHashMap<Long, Vehicle>()

    override fun save(vehicle: Vehicle): Vehicle {
        // Zapisanie pojazdu w mapie
        vehicles[vehicle.id.value] = vehicle
        return vehicle
    }

    override fun findAll(): List<Vehicle> {
        // Zwrócenie wszystkich pojazdów jako lista
        return vehicles.values.toList()
    }

    override fun findById(id: VehicleId): Vehicle? {
        // Wyszukiwanie pojazdu po ID
        return vehicles[id.value]
    }

    override fun findByOwnerId(ownerId: String): List<Vehicle> {
        // Wyszukiwanie pojazdów, które należą do danego właściciela
        return vehicles.values.filter { vehicle ->
            vehicle.ownerIds.contains(ownerId)
        }
    }

    override fun deleteById(id: VehicleId): Boolean {
        // Usuwanie pojazdu po ID
        return vehicles.remove(id.value) != null
    }

    override fun findByVinOrLicensePlate(vin: String?, licensePlate: String): Vehicle? {
        return vehicles.values.firstOrNull { (vin != null && it.vin == vin) || (licensePlate != null && licensePlate == it.licensePlate) }
    }

    override fun findByClientIds(ids: Set<Long>): Map<Long, List<Vehicle>> {
        return vehicles.values
            .filter { vehicle -> vehicle.ownerIds.any { ownerId -> ownerId.toLongOrNull() in ids } }
            .flatMap { vehicle ->
                vehicle.ownerIds
                    .mapNotNull { it.toLongOrNull() }
                    .filter { it in ids }
                    .map { ownerId -> ownerId to vehicle }
            }
            .groupBy({ it.first }, { it.second })
    }
}