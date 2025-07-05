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
import com.carslab.crm.modules.visits.domain.services.ProtocolUpdateDomainService
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
    private val protocolUpdateDomainService: ProtocolUpdateDomainService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<UpdateProtocolCommand, Unit> {

    private val logger = LoggerFactory.getLogger(UpdateProtocolCommandHandler::class.java)

    override fun handle(command: UpdateProtocolCommand) {
        logger.info("Updating protocol: ${command.protocolId}")

        val result = protocolUpdateDomainService.updateProtocol(command)

        protocolEventPublisher.publishUpdateEvents(result, securityContext.getCurrentUser())

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
    private val vehicleApplicationService: VehicleApplicationService,
    private val unifiedDocumentRepository: UnifiedDocumentRepository,
) : CommandHandler<ReleaseVehicleCommand, Unit> {

    private val logger = LoggerFactory.getLogger(ReleaseVehicleCommandHandler::class.java)

    @Transactional
    override fun handle(command: ReleaseVehicleCommand) {
        logger.info("Releasing vehicle for protocol: ${command.protocolId}")

        val existingProtocol: CarReceptionProtocol = protocolRepository.findById(ProtocolId(command.protocolId))
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
            .filter { it.approvalStatus == ApprovalStatus.APPROVED }
            .sumOf { it.finalPrice.amount }
        
        clientApplicationService.updateClientStatistics(
            clientId = ClientId(existingProtocol.client.id!!),
            totalGmv = totalAmount.toBigDecimal(),
        )
        vehicleApplicationService.updateVehicleStatistics(
            id = existingProtocol.vehicle.id!!.value,
            gmv = totalAmount.toBigDecimal(),
        )

        createInvoiceDocument(
            existingProtocol = existingProtocol,
            releaseDetails = command
        )

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

    private fun createInvoiceDocument(existingProtocol: CarReceptionProtocol, releaseDetails: ReleaseVehicleCommand) {
        val gross = existingProtocol.protocolServices.filter { it.approvalStatus == ApprovalStatus.APPROVED }
            .sumOf { it.finalPrice.amount }.toBigDecimal()
        val nett = gross / 1.23.toBigDecimal()
        val totalTax = gross - nett
        val items = existingProtocol.protocolServices.filter { it.approvalStatus == ApprovalStatus.APPROVED }
            .map {
                DocumentItem(
                    name = it.name,
                    description = it.note,
                    quantity = 1.toBigDecimal(),
                    unitPrice = it.finalPrice.amount.toBigDecimal(),
                    taxRate = 23.toBigDecimal(),
                    totalNet = it.finalPrice.amount.toBigDecimal() / 1.23.toBigDecimal(),
                    totalGross = it.finalPrice.amount.toBigDecimal()
                )
            }
        unifiedDocumentRepository.save(
            UnifiedFinancialDocument(
                id = UnifiedDocumentId.generate(),
                number = "",
                type = when (releaseDetails.documentType.lowercase()) {
                    DocumentType.INVOICE.toString().lowercase() -> DocumentType.INVOICE
                    DocumentType.RECEIPT.toString().lowercase() -> DocumentType.RECEIPT
                    DocumentType.OTHER.toString().lowercase() -> DocumentType.OTHER
                    else -> throw IllegalStateException("Unknown document type ${releaseDetails.documentType}")
                },
                title = "Faktura za wizytę",
                description = "",
                issuedDate = LocalDate.now(),
                dueDate = LocalDate.now().plusDays(14),
                sellerName = "Detailing Studio",
                sellerTaxId = "123456789",
                sellerAddress = "ul. Kowalskiego 4/14, 00-001 Warszawa",
                buyerName = existingProtocol.client.name,
                buyerTaxId = existingProtocol.client.taxId,
                buyerAddress = "ul. Kliencka 2/14, 00-001 Gdańsk",
                status = DocumentStatus.NOT_PAID,
                direction = TransactionDirection.INCOME,
                paymentMethod = when (releaseDetails.paymentMethod.lowercase()) {
                    PaymentMethod.CASH.toString().lowercase() -> PaymentMethod.CASH
                    PaymentMethod.CARD.toString().lowercase() -> PaymentMethod.CARD
                    else -> throw IllegalStateException("Unknown payment method ${releaseDetails.paymentMethod}")
                },
                totalNet = nett,
                totalTax = totalTax,
                totalGross = gross,
                paidAmount = BigDecimal.ZERO,
                currency = "PLN",
                notes = "",
                protocolId = existingProtocol.id.value,
                protocolNumber = existingProtocol.id.value,
                visitId = null,
                items = items,
                attachment = null,
                audit = Audit(
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )
        )
    }
}