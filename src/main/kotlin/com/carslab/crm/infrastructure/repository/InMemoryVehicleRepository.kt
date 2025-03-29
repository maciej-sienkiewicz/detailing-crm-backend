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

    override fun deleteById(id: VehicleId): Boolean {
        // Usuwanie pojazdu po ID
        return vehicles.remove(id.value) != null
    }

    override fun findByVinOrLicensePlate(vin: String?, licensePlate: String?): Vehicle? {
        return vehicles.values.firstOrNull { (vin != null && it.vin == vin) || (licensePlate != null && licensePlate == it.licensePlate) }
    }

    override fun findByIds(ids: List<VehicleId>): List<Vehicle> {
        return ids.mapNotNull { vehicles[it.value] }
    }
}