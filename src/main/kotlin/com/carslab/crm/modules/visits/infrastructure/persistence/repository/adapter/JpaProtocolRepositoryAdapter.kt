package com.carslab.crm.modules.visits.infrastructure.persistence.repository.adapter

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.modules.visits.domain.ports.ProtocolRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleJpaRepository
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository

@Repository
class JpaProtocolRepositoryAdapter(
    private val protocolJpaRepository: ProtocolJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val clientJpaRepository: ClientJpaRepository
) : ProtocolRepository {

    override fun save(protocol: CreateProtocolRootModel): ProtocolId {
        val companyId = getCurrentCompanyId()

        val vehicleId = protocol.vehicle.id?.toLong()
            ?: throw IllegalStateException("Vehicle ID is required")

        val clientId = protocol.client.id?.toLong()
            ?: throw IllegalStateException("Client ID is required")

        // Verify entities exist and belong to the same company
        vehicleJpaRepository.findByIdAndCompanyId(companyId = companyId, id = vehicleId)
            .orElse(null) ?: throw IllegalStateException("Vehicle with ID $vehicleId not found or access denied")

        clientJpaRepository.findByIdAndCompanyId(companyId = companyId, id = clientId)
            .orElse(null) ?: throw IllegalStateException("Client with ID $clientId not found or access denied")

        val protocolEntity = ProtocolEntity(
            title = protocol.title,
            companyId = companyId,
            vehicleId = vehicleId,
            clientId = clientId,
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
            statusUpdatedAt = protocol.audit.statusUpdatedAt,
            calendarColorId = protocol.calendarColorId.value,
        )

        val savedEntity = protocolJpaRepository.save(protocolEntity)
        return ProtocolId(savedEntity.id.toString())
    }

    override fun save(protocol: CarReceptionProtocol): CarReceptionProtocol {
        val companyId = getCurrentCompanyId()

        // Find existing entity or create new one
        val protocolEntity = if (protocolJpaRepository.existsById(protocol.id.value.toLong())) {
            val existingEntity = protocolJpaRepository.findByCompanyIdAndId(companyId, protocol.id.value.toLong())
                .orElse(null) ?: throw IllegalStateException("Protocol not found or access denied")

            // Update existing entity
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
            existingEntity.calendarColorId = protocol.calendarColorId.value

            existingEntity
        } else {
            // This shouldn't happen in normal flow, but handle it gracefully
            throw IllegalStateException("Cannot save new protocol via this method - use CreateProtocolRootModel")
        }

        protocolJpaRepository.save(protocolEntity)
        return protocol
    }

    override fun findById(id: ProtocolId): CarReceptionProtocol? {
        // This method is not used in CQRS approach - queries go through read side
        // Return null or throw UnsupportedOperationException
        return null
    }

    override fun existsById(id: ProtocolId): Boolean {
        val companyId = getCurrentCompanyId()
        return protocolJpaRepository.findByCompanyIdAndId(companyId, id.value.toLong()).isPresent
    }

    override fun deleteById(id: ProtocolId): Boolean {
        val companyId = getCurrentCompanyId()
        val protocolIdLong = id.value.toLong()

        val entity = protocolJpaRepository.findByCompanyIdAndId(companyId, protocolIdLong)
            .orElse(null) ?: return false

        protocolJpaRepository.delete(entity)
        return true
    }

    private fun getCurrentCompanyId(): Long {
        return (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
    }
}