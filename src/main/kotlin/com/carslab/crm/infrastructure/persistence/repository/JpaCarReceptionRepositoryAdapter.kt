package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.view.protocol.ProtocolView
import com.carslab.crm.domain.port.CarReceptionRepository
import com.carslab.crm.infrastructure.persistence.entity.ProtocolEntity
import com.carslab.crm.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.VehicleJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Repository
@Transactional
class JpaCarReceptionRepositoryAdapter(
    private val protocolJpaRepository: ProtocolJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val clientJpaRepository: ClientJpaRepository
) : CarReceptionRepository {

    override fun save(protocol: CreateProtocolRootModel): ProtocolId {
        val vehicleId = protocol.vehicle.id?.toLong()
            ?: throw IllegalStateException("Vehicle ID is required")

        val clientId = protocol.client.id?.toLong()
            ?: throw IllegalStateException("Client ID is required")

        val vehicleEntity = vehicleJpaRepository.findById(vehicleId).orElseThrow {
            IllegalStateException("Vehicle with ID $vehicleId not found")
        }

        val clientEntity = clientJpaRepository.findById(clientId).orElseThrow {
            IllegalStateException("Client with ID $clientId not found")
        }

        val protocolEntity = ProtocolEntity(
            title = protocol.title,
            vehicle = vehicleEntity,
            client = clientEntity,
            startDate = protocol.period.startDate,
            endDate = protocol.period.endDate,
            status = protocol.status,
            notes = protocol.notes,
            referralSource = protocol.referralSource,
            otherSourceDetails = protocol.otherSourceDetails,
            keysProvided = protocol.documents.keysProvided,
            documentsProvided = protocol.documents.documentsProvided,
            appointmentId = protocol.audit.appointmentId,
            createdAt = protocol.audit.createdAt,
            updatedAt = protocol.audit.updatedAt,
            statusUpdatedAt = protocol.audit.statusUpdatedAt
        )

        val savedEntity = protocolJpaRepository.save(protocolEntity)
        return ProtocolId(savedEntity.id.toString())
    }

    override fun save(protocol: CarReceptionProtocol): CarReceptionProtocol {
        // Find vehicle and client, or throw if not found
        val vehicleEntity = vehicleJpaRepository.findById(protocol.vehicle.make.toLong()).orElseThrow {
            IllegalStateException("Vehicle not found")
        }

        val clientEntity = protocol.client.id?.let { clientId ->
            clientJpaRepository.findById(clientId).orElseThrow {
                IllegalStateException("Client with ID $clientId not found")
            }
        } ?: throw IllegalStateException("Client ID is required")

        // Check if protocol exists
        val protocolEntity = if (protocolJpaRepository.existsById(protocol.id.value)) {
            val existingEntity = protocolJpaRepository.findById(protocol.id.value).get()

            // Update existing entity fields
            existingEntity.title = protocol.title
            existingEntity.startDate = protocol.period.startDate
            existingEntity.endDate = protocol.period.endDate
            existingEntity.status = protocol.status
            existingEntity.notes = protocol.notes
            existingEntity.referralSource = protocol.referralSource
            existingEntity.otherSourceDetails = protocol.otherSourceDetails
            existingEntity.keysProvided = protocol.documents.keysProvided
            existingEntity.documentsProvided = protocol.documents.documentsProvided
            existingEntity.appointmentId = protocol.audit.appointmentId
            existingEntity.updatedAt = protocol.audit.updatedAt
            existingEntity.statusUpdatedAt = protocol.audit.statusUpdatedAt

            existingEntity
        } else {
            // Create new entity
            ProtocolEntity(
                id = protocol.id.value.toLong(),
                title = protocol.title,
                vehicle = vehicleEntity,
                client = clientEntity,
                startDate = protocol.period.startDate,
                endDate = protocol.period.endDate,
                status = protocol.status,
                notes = protocol.notes,
                referralSource = protocol.referralSource,
                otherSourceDetails = protocol.otherSourceDetails,
                keysProvided = protocol.documents.keysProvided,
                documentsProvided = protocol.documents.documentsProvided,
                appointmentId = protocol.audit.appointmentId,
                createdAt = protocol.audit.createdAt,
                updatedAt = protocol.audit.updatedAt,
                statusUpdatedAt = protocol.audit.statusUpdatedAt
            )
        }

        val savedEntity = protocolJpaRepository.save(protocolEntity)

        // For simplicity, return the original protocol; in a real implementation,
        // you would convert the entity back to a domain object with associated services, etc.
        return protocol
    }

    override fun findById(id: ProtocolId): ProtocolView? {
        return protocolJpaRepository.findById(id.value)
            .map { it.toDomainView() }
            .orElse(null)
    }

    override fun findAll(): List<CarReceptionProtocol> {
        // This would typically involve converting entities to domain objects
        // with all associated data (services, comments, etc.)
        // For simplicity, we're returning an empty list
        return emptyList()
    }

    override fun findByStatus(status: ProtocolStatus): List<CarReceptionProtocol> {
        // For simplicity, we're returning an empty list
        return emptyList()
    }

    override fun findByClientName(clientName: String): List<CarReceptionProtocol> {
        // For simplicity, we're returning an empty list
        return emptyList()
    }

    override fun findByLicensePlate(licensePlate: String): List<CarReceptionProtocol> {
        // For simplicity, we're returning an empty list
        return emptyList()
    }

    override fun deleteById(id: ProtocolId): Boolean {
        return if (protocolJpaRepository.existsById(id.value)) {
            protocolJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }

    override fun searchProtocols(
        clientName: String?,
        clientId: Long?,
        licensePlate: String?,
        status: ProtocolStatus?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): List<ProtocolView> {
        return protocolJpaRepository.searchProtocols(
            clientName = clientName,
            clientId = clientId,
            licensePlate = licensePlate,
            status = status,
            startDate = startDate,
            endDate = endDate
        ).map { it.toDomainView() }
    }
}