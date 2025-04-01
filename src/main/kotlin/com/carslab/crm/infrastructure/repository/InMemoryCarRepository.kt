package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.view.protocol.ProtocolView
import com.carslab.crm.domain.port.CarReceptionRepository
import com.carslab.crm.domain.port.ClientRepository
import com.carslab.crm.domain.port.VehicleRepository
import org.springframework.stereotype.Repository
import java.time.Clock
import java.time.LocalDate
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
class InMemoryCarReceptionRepository(
    private val vehicleRepository: VehicleRepository,
    private val clientRepository: ClientRepository
) : CarReceptionRepository {

    private val clock = Clock.system(ZoneId.systemDefault())

    // Używamy ConcurrentHashMap dla thread safety
    private val protocols = ConcurrentHashMap<String, CarReceptionProtocol>()
    private val entities = ConcurrentHashMap<String, ProtocolEntity>()

    override fun save(protocol: CreateProtocolRootModel): ProtocolId {
        val entity = ProtocolEntity(
            id = protocol.id,
            vehicleId = VehicleId(protocol.vehicle.id?.toLong() ?: throw IllegalStateException("Vehicle ID is required")),
            clientId = ClientId(protocol.client.id?.toLong() ?: throw IllegalStateException("Client ID is required")),
            period = protocol.period,
            status = protocol.status,
            notes = protocol.notes,
            createdAt = clock.instant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        )
        entities[entity.id.value] = entity
        return entity.id
    }

    override fun save(protocol: CarReceptionProtocol): CarReceptionProtocol {
        // Save to the protocol map
        protocols[protocol.id.value] = protocol

        // Create or update the entity
        val entity = entities[protocol.id.value] ?: ProtocolEntity(
            id = protocol.id,
            vehicleId = VehicleId(0), // This will be updated if client has a vehicle ID
            clientId = ClientId(protocol.client.id ?: 0),
            period = protocol.period,
            status = protocol.status,
            notes = protocol.notes,
            createdAt = protocol.audit.createdAt
        )

        // Update with new values
        val updatedEntity = entity.copy(
            status = protocol.status,
            notes = protocol.notes,
            period = protocol.period
        )

        entities[protocol.id.value] = updatedEntity
        return protocol
    }

    override fun findById(id: ProtocolId): ProtocolView? {
        return entities[id.value]?.toDomainObject()
    }

    override fun findAll(): List<CarReceptionProtocol> {
        return protocols.values.toList()
    }

    override fun findByStatus(status: ProtocolStatus): List<CarReceptionProtocol> {
        val entityIds = entities.values
            .filter { it.status == status }
            .map { it.id.value }

        return entityIds.mapNotNull { protocols[it] }
    }

    override fun findByClientName(clientName: String): List<CarReceptionProtocol> {
        // Get client IDs that match the name from client repository
        val matchingClientIds = clientRepository.findByName(clientName).map { it.id }

        // Filter entities by these client IDs
        val entityIds = entities.values
            .filter { entity -> matchingClientIds.any { it.value == entity.clientId.value } }
            .map { it.id.value }

        return entityIds.mapNotNull { protocols[it] }
    }

    override fun findByLicensePlate(licensePlate: String): List<CarReceptionProtocol> {
        // Get vehicles that match the license plate
        val vehicles = vehicleRepository.findAll().filter {
            it.licensePlate?.contains(licensePlate, ignoreCase = true) == true
        }

        // Find entities associated with these vehicles
        val entityIds = entities.values
            .filter { entity -> vehicles.any { it.id.value == entity.vehicleId.value } }
            .map { it.id.value }

        return entityIds.mapNotNull { protocols[it] }
    }

    override fun deleteById(id: ProtocolId): Boolean {
        entities.remove(id.value)
        return protocols.remove(id.value) != null
    }

    override fun searchProtocols(
        clientName: String?,
        clientId: Long?,
        licensePlate: String?,
        status: ProtocolStatus?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): List<ProtocolView> {
        // Start with all entities
        var filteredEntities = entities.values

        // Apply client name filter using client repository
        if (!clientName.isNullOrBlank()) {
            val matchingClientIds = clientRepository.findByName(clientName).map { it.id.value }
            filteredEntities = filteredEntities.filter { entity ->
                matchingClientIds.contains(entity.clientId.value)
            }
                .toMutableList()
        }

        // Apply client ID filter
        if (clientId != null) {
            filteredEntities = filteredEntities.filter { entity ->
                entity.clientId.value == clientId
            }
                .toMutableList()
        }

        // Apply license plate filter using vehicle repository
        if (!licensePlate.isNullOrBlank()) {
            val matchingVehicleIds = vehicleRepository.findAll()
                .filter { it.licensePlate?.contains(licensePlate, ignoreCase = true) == true }
                .map { it.id.value }

            filteredEntities = filteredEntities.filter { entity ->
                matchingVehicleIds.contains(entity.vehicleId.value)
            }
                .toMutableList()
        }

        // Apply status filter directly
        if (status != null) {
            filteredEntities = filteredEntities.filter { entity ->
                entity.status == status
            }
                .toMutableList()
        }

        // Apply date filters
        if (startDate != null) {
            filteredEntities = filteredEntities.filter { entity ->
                !entity.period.endDate.isBefore(startDate)
            }
                .toMutableList()
        }

        if (endDate != null) {
            filteredEntities = filteredEntities.filter { entity ->
                !entity.period.startDate.isAfter(endDate)
            }
                .toMutableList()
        }

        // Map filtered entity IDs to protocols
        return filteredEntities
            .map { it.toDomainObject() }
    }

    private fun ProtocolEntity.toDomainObject() =
        ProtocolView(
            id = id,
            vehicleId = vehicleId,
            clientId = clientId,
            period = period,
            status = status,
            notes = notes,
            createdAt = createdAt
        )
}