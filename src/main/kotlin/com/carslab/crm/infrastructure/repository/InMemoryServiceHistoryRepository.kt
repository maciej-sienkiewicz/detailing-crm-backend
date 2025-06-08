package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.ServiceHistory
import com.carslab.crm.domain.model.ServiceHistoryId
import com.carslab.crm.modules.clients.domain.model.VehicleId
import com.carslab.crm.domain.port.ServiceHistoryRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementacja repozytorium historii serwisowej przechowująca dane w pamięci.
 * Używana głównie do testów i rozwoju aplikacji.
 */
@Repository
class InMemoryServiceHistoryRepository : ServiceHistoryRepository {

    // Mapa przechowująca wpisy historii serwisowej z kluczem jako ID wpisu
    private val serviceHistoryEntries = ConcurrentHashMap<String, ServiceHistory>()

    override fun save(serviceHistory: ServiceHistory): ServiceHistory {
        // Zapisanie wpisu historii serwisowej w mapie
        serviceHistoryEntries[serviceHistory.id.value] = serviceHistory
        return serviceHistory
    }

    override fun findAll(): List<ServiceHistory> {
        // Zwrócenie wszystkich wpisów historii serwisowej jako lista
        return serviceHistoryEntries.values.toList()
    }

    override fun findById(id: ServiceHistoryId): ServiceHistory? {
        // Wyszukiwanie wpisu historii serwisowej po ID
        return serviceHistoryEntries[id.value]
    }

    override fun findByVehicleId(vehicleId: VehicleId): List<ServiceHistory> {
        // Wyszukiwanie wpisów historii serwisowej dla danego pojazdu
        return serviceHistoryEntries.values.filter { serviceHistory ->
            serviceHistory.vehicleId.value == vehicleId.value
        }.sortedByDescending { it.date } // Sortowanie po dacie, od najnowszych
    }

    override fun deleteById(id: ServiceHistoryId): Boolean {
        // Usuwanie wpisu historii serwisowej po ID
        return serviceHistoryEntries.remove(id.value) != null
    }
}