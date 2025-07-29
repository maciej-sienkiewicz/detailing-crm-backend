package com.carslab.crm.modules.visits.infrastructure.persistence.repository.adapter

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.view.calendar.CalendarColorId
import com.carslab.crm.modules.visits.domain.ports.ProtocolRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolServiceJpaRepository
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class JpaProtocolRepositoryAdapter(
    private val protocolJpaRepository: ProtocolJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val clientJpaRepository: ClientJpaRepository,
    private val protocolServiceJpaRepository: ProtocolServiceJpaRepository
) : ProtocolRepository {

    override fun save(protocol: CreateProtocolRootModel): ProtocolId {
        val companyId = getCurrentCompanyId()

        val vehicleId = protocol.vehicle.id?.toLong()
            ?: throw IllegalStateException("Vehicle ID is required")

        val clientId = protocol.client.id?.toLong()
            ?: throw IllegalStateException("Client ID is required")

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

        val protocolEntity = if (protocolJpaRepository.existsById(protocol.id.value.toLong())) {
            val existingEntity = protocolJpaRepository.findByCompanyIdAndId(companyId, protocol.id.value.toLong())
                .orElse(null) ?: throw IllegalStateException("Protocol not found or access denied")

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
            throw IllegalStateException("Cannot save new protocol via this method - use CreateProtocolRootModel")
        }

        protocolJpaRepository.save(protocolEntity)
        return protocol
    }

    override fun findById(id: ProtocolId): CarReceptionProtocol? {
        val companyId = getCurrentCompanyId()
        val entity = protocolJpaRepository.findByCompanyIdAndId(companyId, id.value.toLong())
            .orElse(null) ?: return null

        return convertEntityToDomain(entity)
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

    override fun updateAuditLog(id: ProtocolId) {
        val companyId = getCurrentCompanyId()

        protocolJpaRepository.findByCompanyIdAndId(companyId = companyId, id = id.value.toLong())
            .ifPresent { entity ->
                entity.updatedAt = LocalDateTime.now()
                entity.statusUpdatedAt = LocalDateTime.now()
                protocolJpaRepository.save(entity)
            }
    }

    private fun convertEntityToDomain(entity: ProtocolEntity): CarReceptionProtocol {
        val companyId = getCurrentCompanyId()

        val client = clientJpaRepository.findByIdAndCompanyId(companyId = companyId, id = entity.clientId)
            .orElseThrow { IllegalStateException("Client not found") }

        val vehicle = vehicleJpaRepository.findByIdAndCompanyId(companyId = companyId, id = entity.vehicleId)
            .orElseThrow { IllegalStateException("Vehicle not found") }

        val services = protocolServiceJpaRepository.findByProtocolIdAndCompanyId(entity.id!!, companyId)
            .map { serviceEntity ->
                ProtocolService(
                    id = serviceEntity.id.toString(),
                    name = serviceEntity.name,
                    basePrice = Money(serviceEntity.basePrice.toDouble()),
                    discount = if (serviceEntity.discountType != null && serviceEntity.discountValue != null) {
                        Discount(
                            type = serviceEntity.discountType!!,
                            value = serviceEntity.discountValue!!.toDouble(),
                            calculatedAmount = Money(serviceEntity.basePrice.toDouble() - serviceEntity.finalPrice.toDouble())
                        )
                    } else null,
                    finalPrice = Money(serviceEntity.finalPrice.toDouble()),
                    approvalStatus = serviceEntity.approvalStatus,
                    note = serviceEntity.note,
                    quantity = serviceEntity.quantity.toLong()
                )
            }

        return CarReceptionProtocol(
            id = ProtocolId(entity.id.toString()),
            title = entity.title,
            vehicle = VehicleDetails(
                id = com.carslab.crm.modules.clients.domain.model.VehicleId(vehicle.id!!),
                make = vehicle.make,
                model = vehicle.model,
                licensePlate = vehicle.licensePlate,
                productionYear = vehicle.year ?: 0,
                vin = vehicle.vin,
                color = vehicle.color,
                mileage = vehicle.mileage
            ),
            client = ClientDetails(
                id = client.id!!,
                name = "${client.firstName} ${client.lastName}".trim(),
                email = client.email,
                phone = client.phone,
                companyName = client.company,
                taxId = client.taxId,
                address = client.address,
            ),
            period = ServicePeriod(
                startDate = entity.startDate,
                endDate = entity.endDate
            ),
            status = entity.status,
            protocolServices = services,
            notes = entity.notes,
            referralSource = entity.referralSource,
            otherSourceDetails = entity.otherSourceDetails,
            documents = Documents(
                keysProvided = entity.keysProvided,
                documentsProvided = entity.documentsProvided
            ),
            mediaItems = emptyList(),
            audit = AuditInfo(
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                statusUpdatedAt = entity.statusUpdatedAt,
                appointmentId = entity.appointmentId
            ),
            calendarColorId = CalendarColorId(entity.calendarColorId)
        )
    }

    private fun getCurrentCompanyId(): Long {
        return (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
    }
}