package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.port.CarReceptionRepository
import org.springframework.stereotype.Repository
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementacja repozytorium protokołów w pamięci.
 * Użyteczna do testów i wczesnych etapów rozwoju aplikacji.
 */

data class ProtocolEntity(
    val id: ProtocolId,
    val vehicleId: VehicleId,
    val clientId: ClientId,
    val period: ServicePeriod,
    val status: ProtocolStatus,
    val notes: String?,
    val createdAt: LocalDateTime,
)


@Repository
class InMemoryCarReceptionRepository : CarReceptionRepository {


    private val clock = Clock.system(ZoneId.systemDefault())

    // Używamy ConcurrentHashMap dla thread safety
    private val protocols = ConcurrentHashMap<String, CarReceptionProtocol>()
    private val entities = ConcurrentHashMap<ProtocolId, ProtocolEntity>()

    override fun save(protocol: CreateProtocolRootModel): ProtocolId {
        return protocol.let {
            ProtocolEntity(
                id = it.id,
                vehicleId = VehicleId(it.vehicle.id?.toLong() ?: throw IllegalStateException("Vehicle ID is required")),
                clientId = ClientId(it.client.id?.toLong() ?: throw IllegalStateException("Client ID is required")),
                period = it.period,
                status = it.status,
                notes = it.notes,
                createdAt = clock.instant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            )
        }
            .also { entities[it.id] = it }
            .id
    }

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