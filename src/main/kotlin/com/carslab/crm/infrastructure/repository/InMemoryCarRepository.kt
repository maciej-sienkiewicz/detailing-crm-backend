package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.port.CarReceptionRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementacja repozytorium protokołów w pamięci.
 * Użyteczna do testów i wczesnych etapów rozwoju aplikacji.
 */
@Repository
class InMemoryCarReceptionRepository : CarReceptionRepository {

    // Używamy ConcurrentHashMap dla thread safety
    private val protocols = ConcurrentHashMap<String, CarReceptionProtocol>()

    override fun save(protocol: CarReceptionProtocol): CarReceptionProtocol {
        protocols[protocol.id.value] = protocol
        return protocol
    }

    override fun findById(id: ProtocolId): CarReceptionProtocol? {
        return protocols[id.value]
    }

    override fun findAll(): List<CarReceptionProtocol> {
        return protocols.values.toList()
    }

    override fun findByStatus(status: ProtocolStatus): List<CarReceptionProtocol> {
        return protocols.values.filter { it.status == status }
    }

    override fun findByClientName(clientName: String): List<CarReceptionProtocol> {
        val lowerName = clientName.lowercase()
        return protocols.values.filter {
            it.client.name.lowercase().contains(lowerName) ||
                    it.client.companyName?.lowercase()?.contains(lowerName) == true
        }
    }

    override fun findByLicensePlate(licensePlate: String): List<CarReceptionProtocol> {
        val lowerPlate = licensePlate.lowercase().replace(" ", "")
        return protocols.values.filter {
            it.vehicle.licensePlate.lowercase().replace(" ", "").contains(lowerPlate)
        }
    }

    override fun deleteById(id: ProtocolId): Boolean {
        return protocols.remove(id.value) != null
    }
}