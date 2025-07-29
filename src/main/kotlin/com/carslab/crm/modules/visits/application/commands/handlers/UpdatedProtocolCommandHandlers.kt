package com.carslab.crm.modules.visits.application.commands.handlers

import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.api.model.DocumentType
import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.domain.model.ApprovalStatus
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.domain.ports.ProtocolRepository
import com.carslab.crm.modules.visits.domain.services.ProtocolDomainService
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import com.carslab.crm.infrastructure.events.EventPublisher
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.modules.visits.domain.events.*
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolService
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.view.finance.DocumentItem
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.modules.clients.domain.ClientApplicationService
import com.carslab.crm.modules.clients.domain.VehicleApplicationService
import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.visits.api.mappers.ServiceMappers
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.UpdateServiceCommand
import com.carslab.crm.modules.visits.domain.exceptions.ProtocolNotFoundException
import com.carslab.crm.modules.visits.domain.services.ClientStatisticsService
import com.carslab.crm.modules.visits.domain.services.ProtocolServicesService
import com.carslab.crm.modules.visits.domain.services.ProtocolUpdateDomainService
import com.carslab.crm.modules.visits.domain.services.VehicleStatisticsService
import com.carslab.crm.modules.visits.domain.valueobjects.ServicesUpdateResult
import com.carslab.crm.modules.visits.infrastructure.events.CurrentUser
import com.carslab.crm.modules.visits.infrastructure.events.ProtocolEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class UpdateProtocolCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val protocolServicesService: ProtocolServicesService,
    private val protocolDomainService: ProtocolDomainService,
    private val clientStatisticsService: ClientStatisticsService,
    private val vehicleStatisticsService: VehicleStatisticsService,
    private val clientApplicationService: ClientApplicationService,
    private val vehicleApplicationService: VehicleApplicationService,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<UpdateProtocolCommand, Unit> {

    private val logger = LoggerFactory.getLogger(UpdateProtocolCommandHandler::class.java)

    @Transactional
    override fun handle(command: UpdateProtocolCommand) {
        logger.info("Updating protocol: ${command.protocolId}")

        val protocolId = ProtocolId(command.protocolId)
        val existingProtocol = protocolRepository.findById(protocolId)
            ?: throw ProtocolNotFoundException(protocolId)

        // 1. Sprawdź czy usługi się rzeczywiście zmieniły
        val servicesChanged = if (command.services.isNotEmpty()) {
            checkIfServicesActuallyChanged(protocolId, command.services)
        } else false

        // 2. Wykonaj aktualizacje
        val servicesUpdateResult = if (servicesChanged) {
            protocolServicesService.updateProtocolServices(protocolId, command.services)
        } else {
            ServicesUpdateResult.noChanges()
        }

        val updatedProtocol = protocolDomainService.updateProtocol(existingProtocol, command)
        protocolRepository.save(updatedProtocol)

        val statusChanged = existingProtocol.status != updatedProtocol.status

        // 3. Aktualizuj statystyki przy przejściu do IN_PROGRESS
        if (statusChanged && updatedProtocol.status == ProtocolStatus.IN_PROGRESS && existingProtocol.status != ProtocolStatus.IN_PROGRESS) {
            updateStatistics(updatedProtocol)
        }

        // 4. Publikuj eventy - tylko te które są potrzebne
        val user = getCurrentUser()

        if (statusChanged) {
            publishStatusEvent(existingProtocol.status, updatedProtocol, user)
        }

        if (servicesChanged) {
            publishServicesEvent(protocolId, servicesUpdateResult, user)
        }

        logger.info("Successfully updated protocol: ${command.protocolId}")
    }

    private fun checkIfServicesActuallyChanged(
        protocolId: ProtocolId,
        newServices: List<UpdateServiceCommand>
    ): Boolean {
        val currentServices = protocolServicesService.getCurrentServices(protocolId)

        if (currentServices.size != newServices.size) return true

        val currentByName = currentServices.associateBy { it.name }

        return newServices.any { newService ->
            val current = currentByName[newService.name] ?: return true

            // Porównaj kluczowe pola
            current.basePrice.amount != newService.basePrice ||
                    current.finalPrice?.amount != (newService.finalPrice ?: newService.basePrice) ||
                    current.quantity != newService.quantity ||
                    current.approvalStatus.name != newService.approvalStatus ||
                    current.note != newService.note
        }
    }

    private fun updateStatistics(protocol: CarReceptionProtocol) {
        try {
            clientStatisticsService.updateLastVisitDate(ClientId.of(protocol.client.id!!))
            clientApplicationService.updateClientStatistics(clientId = ClientId.of(protocol.client.id!!), counter = 1L)

            vehicleStatisticsService.updateLastVisitDate(protocol.vehicle.id!!)
            vehicleApplicationService.updateVehicleStatistics(id = protocol.vehicle.id!!.value, counter = 1L)
        } catch (e: Exception) {
            logger.warn("Failed to update statistics for protocol: ${protocol.id.value}", e)
        }
    }

    private fun publishStatusEvent(
        oldStatus: ProtocolStatus,
        updatedProtocol: CarReceptionProtocol,
        user: CurrentUser
    ) {
        when (updatedProtocol.status) {
            ProtocolStatus.IN_PROGRESS -> {
                eventPublisher.publish(ProtocolWorkStartedEvent(
                    protocolId = updatedProtocol.id.value,
                    protocolTitle = updatedProtocol.title,
                    assignedTechnicians = listOf(user.name),
                    estimatedCompletionTime = updatedProtocol.period.endDate.toString(),
                    plannedServices = updatedProtocol.protocolServices.map { it.name },
                    companyId = user.companyId,
                    userId = user.id,
                    userName = user.name,
                    clientId = updatedProtocol.client.id.toString(),
                    clientName = updatedProtocol.client.name,
                    vehicleId = updatedProtocol.vehicle.id.toString(),
                    vehicleDisplayName = "${updatedProtocol.vehicle.make} ${updatedProtocol.vehicle.model}"
                ))
            }
            ProtocolStatus.READY_FOR_PICKUP -> {
                eventPublisher.publish(ProtocolReadyForPickupEvent(
                    protocolId = updatedProtocol.id.value,
                    protocolTitle = updatedProtocol.title,
                    clientId = updatedProtocol.client.id.toString(),
                    clientName = updatedProtocol.client.name,
                    vehicleInfo = "${updatedProtocol.vehicle.make} ${updatedProtocol.vehicle.model}",
                    totalAmount = updatedProtocol.protocolServices.sumOf { it.finalPrice.amount },
                    servicesCompleted = updatedProtocol.protocolServices.map { it.name },
                    companyId = user.companyId,
                    userId = user.id,
                    userName = user.name
                ))
            }
            else -> {
                eventPublisher.publish(ProtocolStatusChangedEvent(
                    protocolId = updatedProtocol.id.value,
                    oldStatus = oldStatus.name,
                    newStatus = updatedProtocol.status.name,
                    reason = null,
                    protocolTitle = updatedProtocol.title,
                    companyId = user.companyId,
                    userId = user.id,
                    userName = user.name
                ))
            }
        }
    }

    private fun publishServicesEvent(
        protocolId: ProtocolId,
        updateResult: ServicesUpdateResult,
        user: CurrentUser
    ) {
        eventPublisher.publish(ProtocolServicesUpdatedEvent(
            protocolId = protocolId.value,
            servicesCount = updateResult.updatedCount,
            totalAmount = updateResult.totalAmount,
            changedServices = updateResult.serviceNames,
            companyId = user.companyId,
            userId = user.id,
            userName = user.name
        ))
    }

    private fun getCurrentUser(): CurrentUser {
        return CurrentUser(
            id = securityContext.getCurrentUserId()!!,
            name = securityContext.getCurrentUserName() ?: "Unknown",
            companyId = securityContext.getCurrentCompanyId()
        )
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
            ProtocolService(
                id = UUID.randomUUID().toString(),
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
    private val securityContext: SecurityContext,
    private val clientApplicationService: ClientApplicationService,
    private val vehicleApplicationService: VehicleApplicationService
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
            protocol = existingProtocol,
            newStatus = ProtocolStatus.COMPLETED,
            reason = "Vehicle released to client with payment method: ${command.paymentMethod}"
        )

        protocolRepository.save(completedProtocol)

        val totalAmount = existingProtocol.protocolServices
            .filter { it.approvalStatus == ApprovalStatus.APPROVED }
            .sumOf { it.finalPrice.amount }
            .toBigDecimal()

        // Aktualizuj statystyki
        updateStatistics(existingProtocol, totalAmount)

        eventPublisher.publish(VehicleServiceCompletedEvent(
            protocolId = command.protocolId,
            protocolTitle = existingProtocol.title,
            clientId = existingProtocol.client.id!!,
            clientName = existingProtocol.client.name,
            clientTaxId = existingProtocol.client.taxId,
            clientAddress = existingProtocol.client.address,
            paymentIn = command.paymentDays,
            services = if(command.overridenItems.isEmpty()) { existingProtocol.protocolServices
                .filter { it.approvalStatus == ApprovalStatus.APPROVED }
                .map { ServiceItem(
                    name = it.name,
                    description = it.note,
                    quantity = 1.toBigDecimal(),
                    unitPrice = it.finalPrice.amount.toBigDecimal(),
                    taxRate = 23.toBigDecimal(),
                    totalNet = (it.finalPrice.amount / 1.23).toBigDecimal(),
                    totalGross = it.finalPrice.amount.toBigDecimal()
                )} } else command.overridenItems.map { 
                    ServiceItem(
                    name = it.name,
                    description = null,
                    quantity = 1.toBigDecimal(),
                    unitPrice = it.basePrice.toBigDecimal(),
                    taxRate = 23.toBigDecimal(),
                    totalNet = (it.finalPrice ?: it.basePrice).toBigDecimal() / 1.23.toBigDecimal(),
                    totalGross = (it.finalPrice ?: it.basePrice).toBigDecimal()
                )
            },
            totalNet = (totalAmount / 1.23.toBigDecimal()),
            totalTax = totalAmount - (totalAmount / 1.23.toBigDecimal()),
            totalGross = totalAmount,
            paymentMethod = command.paymentMethod,
            documentType = command.documentType,
            companyId = securityContext.getCurrentCompanyId(),
            userId = securityContext.getCurrentUserId(),
            userName = securityContext.getCurrentUserName()
        ))

        // Event o zwolnieniu pojazdu
        eventPublisher.publish(VehicleReleasedEvent(
            visitId = command.protocolId,
            visitTitle = existingProtocol.title,
            clientId = existingProtocol.client.id.toString(),
            clientName = existingProtocol.client.name,
            vehicleId = existingProtocol.vehicle.id?.value.toString(),
            vehicleDisplayName = "${existingProtocol.vehicle.make} ${existingProtocol.vehicle.model} (${existingProtocol.vehicle.licensePlate})",
            paymentMethod = command.paymentMethod,
            totalAmount = totalAmount.toDouble(),
            releaseNotes = command.additionalNotes,
            companyId = securityContext.getCurrentCompanyId(),
            userId = securityContext.getCurrentUserId(),
            userName = securityContext.getCurrentUserName()
        ))

        logger.info("Successfully released vehicle for protocol: ${command.protocolId}")
    }

    private fun updateStatistics(protocol: CarReceptionProtocol, totalAmount: BigDecimal) {
        try {
            clientApplicationService.updateClientStatistics(
                clientId = ClientId(protocol.client.id!!),
                totalGmv = totalAmount
            )
            vehicleApplicationService.updateVehicleStatistics(
                id = protocol.vehicle.id!!.value,
                gmv = totalAmount
            )
        } catch (e: Exception) {
            logger.warn("Failed to update statistics for protocol: ${protocol.id.value}", e)
        }
    }
}