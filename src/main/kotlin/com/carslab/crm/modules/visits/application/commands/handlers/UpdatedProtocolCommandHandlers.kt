package com.carslab.crm.modules.visits.application.commands.handlers

import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.domain.ports.ProtocolRepository
import com.carslab.crm.modules.visits.domain.services.ProtocolDomainService
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import com.carslab.crm.infrastructure.events.EventPublisher
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.modules.visits.domain.events.*
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.modules.visits.api.mappers.ServiceMappers
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UpdateProtocolCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val protocolDomainService: ProtocolDomainService,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<UpdateProtocolCommand, Unit> {

    private val logger = LoggerFactory.getLogger(UpdateProtocolCommandHandler::class.java)

    @Transactional
    override fun handle(command: UpdateProtocolCommand) {
        logger.info("Updating protocol: ${command.protocolId}")

        val existingProtocol = protocolRepository.findById(ProtocolId(command.protocolId))
            ?: throw ResourceNotFoundException("Protocol", command.protocolId)

        val updatedProtocol = protocolDomainService.updateProtocol(existingProtocol, command)
        protocolRepository.save(updatedProtocol)

        if (existingProtocol.status != updatedProtocol.status) {
            eventPublisher.publish(
                ProtocolStatusChangedEvent(
                    protocolId = command.protocolId,
                    oldStatus = existingProtocol.status.name,
                    newStatus = updatedProtocol.status.name,
                    reason = "Protocol updated",
                    protocolTitle = updatedProtocol.title,
                    companyId = securityContext.getCurrentCompanyId(),
                    userId = securityContext.getCurrentUserId(),
                    userName = securityContext.getCurrentUserName()
                )
            )
        }

        logger.info("Successfully updated protocol: ${command.protocolId}")
    }
}

@Service
class ChangeProtocolStatusCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val protocolDomainService: ProtocolDomainService,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<ChangeProtocolStatusCommand, Unit> {

    private val logger = LoggerFactory.getLogger(ChangeProtocolStatusCommandHandler::class.java)

    @Transactional
    override fun handle(command: ChangeProtocolStatusCommand) {
        logger.info("Changing status of protocol ${command.protocolId} to ${command.newStatus}")

        val existingProtocol = protocolRepository.findById(ProtocolId(command.protocolId))
            ?: throw ResourceNotFoundException("Protocol", command.protocolId)

        val oldStatus = existingProtocol.status
        val updatedProtocol = protocolDomainService.changeStatus(existingProtocol, command.newStatus, command.reason)

        protocolRepository.save(updatedProtocol)

        eventPublisher.publish(
            ProtocolStatusChangedEvent(
                protocolId = command.protocolId,
                oldStatus = oldStatus.name,
                newStatus = command.newStatus.name,
                reason = command.reason,
                protocolTitle = updatedProtocol.title,
                companyId = securityContext.getCurrentCompanyId(),
                userId = securityContext.getCurrentUserId(),
                userName = securityContext.getCurrentUserName()
            )
        )

        if (command.newStatus == ProtocolStatus.READY_FOR_PICKUP) {
            eventPublisher.publish(
                ProtocolReadyForPickupEvent(
                    protocolId = command.protocolId,
                    protocolTitle = updatedProtocol.title,
                    clientId = updatedProtocol.client.id?.toString(),
                    clientName = updatedProtocol.client.name,
                    vehicleInfo = "${updatedProtocol.vehicle.make} ${updatedProtocol.vehicle.model} (${updatedProtocol.vehicle.licensePlate})",
                    totalAmount = updatedProtocol.protocolServices.sumOf { it.finalPrice.amount },
                    servicesCompleted = updatedProtocol.protocolServices.map { it.name },
                    companyId = securityContext.getCurrentCompanyId(),
                    userId = securityContext.getCurrentUserId(),
                    userName = securityContext.getCurrentUserName()
                )
            )
        } else {
            eventPublisher.publish(
                ProtocolStatusChangedEvent(
                    protocolId = command.protocolId,
                    oldStatus = oldStatus.name,
                    newStatus = command.newStatus.name,
                    reason = command.reason,
                    protocolTitle = updatedProtocol.title,
                    companyId = securityContext.getCurrentCompanyId(),
                    userId = securityContext.getCurrentUserId(),
                    userName = securityContext.getCurrentUserName()
                )
            )
        }
        
        logger.info("Successfully changed status of protocol ${command.protocolId} to ${command.newStatus}")
    }
}

@Service
class UpdateProtocolServicesCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<UpdateProtocolServicesCommand, Unit> {

    private val logger = LoggerFactory.getLogger(UpdateProtocolServicesCommandHandler::class.java)

    @Transactional
    override fun handle(command: UpdateProtocolServicesCommand) {
        logger.info("Updating services for protocol: ${command.protocolId}")

        val existingProtocol = protocolRepository.findById(ProtocolId(command.protocolId))
            ?: throw ResourceNotFoundException("Protocol", command.protocolId)

        val serviceModels = command.services.map { serviceCommand ->
            ServiceMappers.toCreateServiceModel(serviceCommand)
        }

        val updatedServices = serviceModels.map { serviceModel ->
            com.carslab.crm.domain.model.ProtocolService(
                id = java.util.UUID.randomUUID().toString(),
                name = serviceModel.name,
                basePrice = serviceModel.basePrice,
                discount = serviceModel.discount,
                finalPrice = serviceModel.finalPrice,
                approvalStatus = serviceModel.approvalStatus,
                note = serviceModel.note,
                quantity = serviceModel.quantity
            )
        }

        val updatedProtocol = existingProtocol.copy(
            protocolServices = updatedServices,
            audit = existingProtocol.audit.copy(updatedAt = LocalDateTime.now())
        )

        protocolRepository.save(updatedProtocol)

        val totalAmount = command.services.sumOf { it.finalPrice ?: it.basePrice }

        eventPublisher.publish(
            ProtocolServicesUpdatedEvent(
                protocolId = command.protocolId,
                servicesCount = command.services.size,
                totalAmount = totalAmount,
                changedServices = command.services.map { it.name },
                companyId = securityContext.getCurrentCompanyId(),
                userId = securityContext.getCurrentUserId(),
                userName = securityContext.getCurrentUserName()
            )
        )

        logger.info("Successfully updated services for protocol: ${command.protocolId}")
    }
}

@Service
class DeleteProtocolCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<DeleteProtocolCommand, Unit> {

    private val logger = LoggerFactory.getLogger(DeleteProtocolCommandHandler::class.java)

    @Transactional
    override fun handle(command: DeleteProtocolCommand) {
        logger.info("Deleting protocol: ${command.protocolId}")

        val existingProtocol = protocolRepository.findById(ProtocolId(command.protocolId))
            ?: throw ResourceNotFoundException("Protocol", command.protocolId)

        val deleted = protocolRepository.deleteById(ProtocolId(command.protocolId))

        if (!deleted) {
            throw IllegalStateException("Failed to delete protocol: ${command.protocolId}")
        }

        eventPublisher.publish(
            ProtocolDeletedEvent(
                protocolId = command.protocolId,
                protocolTitle = existingProtocol.title,
                clientName = existingProtocol.client.name,
                vehicleInfo = "${existingProtocol.vehicle.make} ${existingProtocol.vehicle.model} (${existingProtocol.vehicle.licensePlate})",
                deletionReason = "Manual deletion",
                companyId = securityContext.getCurrentCompanyId(),
                userId = securityContext.getCurrentUserId(),
                userName = securityContext.getCurrentUserName()
            )
        )

        logger.info("Successfully deleted protocol: ${command.protocolId}")
    }
}

@Service
class ReleaseVehicleCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val protocolDomainService: ProtocolDomainService,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<ReleaseVehicleCommand, Unit> {

    private val logger = LoggerFactory.getLogger(ReleaseVehicleCommandHandler::class.java)

    @Transactional
    override fun handle(command: ReleaseVehicleCommand) {
        logger.info("Releasing vehicle for protocol: ${command.protocolId}")

        val existingProtocol = protocolRepository.findById(ProtocolId(command.protocolId))
            ?: throw ResourceNotFoundException("Protocol", command.protocolId)

        if (existingProtocol.status != ProtocolStatus.READY_FOR_PICKUP) {
            throw IllegalStateException("Protocol must be in READY_FOR_PICKUP status to release vehicle")
        }

        val completedProtocol = protocolDomainService.changeStatus(
            existingProtocol,
            ProtocolStatus.COMPLETED,
            "Vehicle released to client with payment method: ${command.paymentMethod}"
        )

        protocolRepository.save(completedProtocol)

        val totalAmount = existingProtocol.protocolServices
            .filter { it.approvalStatus == com.carslab.crm.domain.model.ApprovalStatus.APPROVED }
            .sumOf { it.finalPrice.amount }

        eventPublisher.publish(
            VehicleReleasedEvent(
                visitId = command.protocolId,
                visitTitle = existingProtocol.title,
                clientId = existingProtocol.client.id.toString(),
                clientName = existingProtocol.client.name,
                vehicleId = existingProtocol.vehicle.id?.value.toString(),
                vehicleDisplayName = "${existingProtocol.vehicle.make} ${existingProtocol.vehicle.model} (${existingProtocol.vehicle.licensePlate})",
                paymentMethod = command.paymentMethod,
                totalAmount = totalAmount,
                releaseNotes = command.additionalNotes,
                companyId = securityContext.getCurrentCompanyId(),
                userId = securityContext.getCurrentUserId(),
                userName = securityContext.getCurrentUserName()
            )
        )

        logger.info("Successfully released vehicle for protocol: ${command.protocolId}")
    }
}