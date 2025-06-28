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
import com.carslab.crm.modules.visits.api.mappers.ServiceMappers
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

        // Publish event if status changed
        if (existingProtocol.status != updatedProtocol.status) {
            eventPublisher.publish(
                ProtocolStatusChangedEvent(
                    protocolId = command.protocolId,
                    oldStatus = existingProtocol.status.name,
                    newStatus = updatedProtocol.status.name,
                    reason = "Protocol updated",
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

        // Publish status change event
        eventPublisher.publish(
            ProtocolStatusChangedEvent(
                protocolId = command.protocolId,
                oldStatus = oldStatus.name,
                newStatus = command.newStatus.name,
                reason = command.reason,
                companyId = securityContext.getCurrentCompanyId(),
                userId = securityContext.getCurrentUserId(),
                userName = securityContext.getCurrentUserName()
            )
        )

        logger.info("Successfully changed status of protocol ${command.protocolId} to ${command.newStatus}")
    }
}

@Service
class UpdateProtocolServicesCommandHandler(
    private val protocolServicesRepository: com.carslab.crm.modules.visits.domain.ports.ProtocolServicesRepository,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<UpdateProtocolServicesCommand, Unit> {

    private val logger = LoggerFactory.getLogger(UpdateProtocolServicesCommandHandler::class.java)

    @Transactional
    override fun handle(command: UpdateProtocolServicesCommand) {
        logger.info("Updating services for protocol: ${command.protocolId}")

        val serviceModels = command.services.map { serviceCommand ->
            ServiceMappers.toCreateServiceModel(serviceCommand)
        }

        val totalAmount = command.services.sumOf { it.finalPrice ?: it.basePrice }

        // Publish services updated event
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

        // Get protocol details before deletion for event
        val existingProtocol = protocolRepository.findById(ProtocolId(command.protocolId))
            ?: throw ResourceNotFoundException("Protocol", command.protocolId)

        val deleted = protocolRepository.deleteById(ProtocolId(command.protocolId))

        if (!deleted) {
            throw IllegalStateException("Failed to delete protocol: ${command.protocolId}")
        }

        // Publish deletion event
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

        // Validate protocol status
        if (existingProtocol.status != com.carslab.crm.domain.model.ProtocolStatus.READY_FOR_PICKUP) {
            throw IllegalStateException("Protocol must be in READY_FOR_PICKUP status to release vehicle")
        }

        // Change status to COMPLETED
        val completedProtocol = protocolDomainService.changeStatus(
            existingProtocol,
            com.carslab.crm.domain.model.ProtocolStatus.COMPLETED,
            "Vehicle released to client"
        )

        protocolRepository.save(completedProtocol)

        // Calculate total amount
        val totalAmount = existingProtocol.protocolServices
            .filter { it.approvalStatus == com.carslab.crm.domain.model.ApprovalStatus.APPROVED }
            .sumOf { it.finalPrice.amount }

        // Publish vehicle released event
        eventPublisher.publish(
            VehicleReleasedEvent(
                visitId = command.protocolId,
                protocolId = command.protocolId,
                clientId = existingProtocol.client.id.toString(),
                clientName = existingProtocol.client.name,
                vehicleId = existingProtocol.vehicle.id?.value.toString(),
                vehicleDisplayName = "${existingProtocol.vehicle.make} ${existingProtocol.vehicle.model}",
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