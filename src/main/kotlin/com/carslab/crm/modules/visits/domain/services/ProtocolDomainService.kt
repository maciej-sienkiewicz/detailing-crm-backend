package com.carslab.crm.modules.visits.domain.services

import com.carslab.crm.modules.visits.application.commands.models.CreateProtocolCommand
import com.carslab.crm.modules.visits.application.commands.models.UpdateProtocolCommand
import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.protocol.CreateMediaTypeModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolClientModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolVehicleModel
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.domain.model.view.calendar.CalendarColorId
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateClientCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateMediaCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateServiceCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateVehicleCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.UpdateServiceCommand
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ProtocolDomainService {

    fun createProtocol(command: CreateProtocolCommand): CreateProtocolRootModel {
        val protocolId = ProtocolId.generate()
        val now = LocalDateTime.now()

        return CreateProtocolRootModel(
            id = protocolId,
            title = command.title,
            calendarColorId = CalendarColorId(command.calendarColorId),
            vehicle = mapToCreateVehicleModel(command.vehicle),
            client = mapToCreateClientModel(command.client),
            period = ServicePeriod(
                startDate = command.period.startDate,
                endDate = command.period.endDate
            ),
            status = command.status,
            services = command.services.map { mapToCreateServiceModel(it) },
            notes = command.notes,
            referralSource = command.referralSource?.let { ReferralSource.valueOf(it) },
            otherSourceDetails = command.otherSourceDetails,
            documents = Documents(
                keysProvided = command.documents.keysProvided,
                documentsProvided = command.documents.documentsProvided
            ),
            mediaItems = command.mediaItems.map { mapToCreateMediaModel(it) },
            audit = AuditInfo(
                createdAt = now,
                updatedAt = now,
                statusUpdatedAt = now,
                appointmentId = command.appointmentId
            )
        )
    }

    fun updateProtocol(existingProtocol: CarReceptionProtocol, command: UpdateProtocolCommand): CarReceptionProtocol {
        val now = LocalDateTime.now()

        validateUpdateCommand(command, existingProtocol)

        val updatedServices = command.services.map { serviceCommand ->
            val serviceModel = mapToUpdateServiceModel(serviceCommand)
            ProtocolService(
                id = serviceCommand.id.ifEmpty { java.util.UUID.randomUUID().toString() },
                name = serviceModel.name,
                basePrice = serviceModel.basePrice,
                discount = serviceModel.discount,
                finalPrice = serviceModel.finalPrice,
                approvalStatus = serviceModel.approvalStatus,
                note = serviceModel.note,
                quantity = serviceModel.quantity
            )
        }

        return existingProtocol.copy(
            title = command.title,
            calendarColorId = CalendarColorId(command.calendarColorId),
            period = ServicePeriod(
                startDate = command.period.startDate,
                endDate = command.period.endDate
            ),
            status = command.status,
            protocolServices = updatedServices,
            notes = command.notes,
            referralSource = command.referralSource?.let { ReferralSource.valueOf(it) },
            otherSourceDetails = command.otherSourceDetails,
            documents = Documents(
                keysProvided = command.documents.keysProvided,
                documentsProvided = command.documents.documentsProvided
            ),
            audit = existingProtocol.audit.copy(
                updatedAt = now,
                statusUpdatedAt = if (existingProtocol.status != command.status) now else existingProtocol.audit.statusUpdatedAt,
                appointmentId = command.appointmentId
            )
        )
    }

    fun changeStatus(protocol: CarReceptionProtocol, newStatus: ProtocolStatus, reason: String?): CarReceptionProtocol {
        validateStatusTransition(protocol.status, newStatus)

        return protocol.copy(
            status = newStatus,
            audit = protocol.audit.copy(
                updatedAt = LocalDateTime.now(),
                statusUpdatedAt = LocalDateTime.now()
            )
        )
    }

    private fun validateUpdateCommand(command: UpdateProtocolCommand, existingProtocol: CarReceptionProtocol) {
        if (command.period.startDate.isAfter(command.period.endDate)) {
            throw IllegalArgumentException("Start date cannot be after end date")
        }

        if (existingProtocol.status == ProtocolStatus.COMPLETED && command.status != ProtocolStatus.COMPLETED) {
            throw IllegalStateException("Cannot change status from COMPLETED")
        }

        if (existingProtocol.status == ProtocolStatus.CANCELLED && command.status != ProtocolStatus.CANCELLED) {
            throw IllegalStateException("Cannot change status from CANCELLED")
        }
    }

    private fun validateStatusTransition(currentStatus: ProtocolStatus, newStatus: ProtocolStatus) {
        when (currentStatus) {
            ProtocolStatus.SCHEDULED -> {
                if (newStatus !in listOf(
                        ProtocolStatus.IN_PROGRESS,
                        ProtocolStatus.CANCELLED,
                        ProtocolStatus.PENDING_APPROVAL
                    )
                ) {
                    throw IllegalStateException("Cannot transition from SCHEDULED to $newStatus")
                }
            }

            ProtocolStatus.PENDING_APPROVAL -> {
                if (newStatus !in listOf(
                        ProtocolStatus.SCHEDULED,
                        ProtocolStatus.IN_PROGRESS,
                        ProtocolStatus.CANCELLED
                    )
                ) {
                    throw IllegalStateException("Cannot transition from PENDING_APPROVAL to $newStatus")
                }
            }

            ProtocolStatus.IN_PROGRESS -> {
                if (newStatus !in listOf(ProtocolStatus.READY_FOR_PICKUP, ProtocolStatus.CANCELLED)) {
                    throw IllegalStateException("Cannot transition from IN_PROGRESS to $newStatus")
                }
            }

            ProtocolStatus.READY_FOR_PICKUP -> {
                if (newStatus !in listOf(ProtocolStatus.COMPLETED, ProtocolStatus.IN_PROGRESS)) {
                    throw IllegalStateException("Cannot transition from READY_FOR_PICKUP to $newStatus")
                }
            }

            ProtocolStatus.COMPLETED -> {
                throw IllegalStateException("Cannot change status from COMPLETED")
            }

            ProtocolStatus.CANCELLED -> {
                throw IllegalStateException("Cannot change status from CANCELLED")
            }
        }
    }

    private fun mapToCreateVehicleModel(command: CreateVehicleCommand): CreateProtocolVehicleModel {
        return CreateProtocolVehicleModel(
            id = command.id,
            brand = command.brand,
            model = command.model,
            licensePlate = command.licensePlate,
            productionYear = command.productionYear,
            vin = command.vin,
            color = command.color,
            mileage = command.mileage
        )
    }

    private fun mapToCreateClientModel(command: CreateClientCommand): CreateProtocolClientModel {
        return CreateProtocolClientModel(
            id = command.id,
            name = command.name,
            email = command.email,
            phone = command.phone,
            companyName = command.companyName,
            taxId = command.taxId
        )
    }

    private fun mapToCreateServiceModel(command: CreateServiceCommand): CreateServiceModel {
        val basePrice = command.basePrice
        val finalPrice = command.finalPrice ?: basePrice

        val discount = if (command.discountType != null && command.discountValue != null && command.discountValue > 0) {
            val discountType = DiscountType.valueOf(command.discountType)
            val calculatedAmount = when (discountType) {
                DiscountType.PERCENTAGE -> basePrice * (command.discountValue / 100)
                DiscountType.AMOUNT -> command.discountValue
                DiscountType.FIXED_PRICE -> basePrice - command.discountValue
            }

            Discount(
                type = discountType,
                value = command.discountValue,
                calculatedAmount = Money(calculatedAmount)
            )
        } else null

        return CreateServiceModel(
            name = command.name,
            basePrice = Money(basePrice),
            discount = discount,
            finalPrice = Money(finalPrice),
            approvalStatus = ApprovalStatus.valueOf(command.approvalStatus),
            note = command.note,
            quantity = command.quantity
        )
    }

    private fun mapToUpdateServiceModel(command: UpdateServiceCommand): CreateServiceModel {
        val basePrice = command.basePrice
        val finalPrice = command.finalPrice ?: basePrice

        val discount = if (command.discountType != null && command.discountValue != null && command.discountValue > 0) {
            val discountType = DiscountType.valueOf(command.discountType)
            val calculatedAmount = when (discountType) {
                DiscountType.PERCENTAGE -> basePrice * (command.discountValue / 100)
                DiscountType.AMOUNT -> command.discountValue
                DiscountType.FIXED_PRICE -> basePrice - command.discountValue
            }

            Discount(
                type = discountType,
                value = command.discountValue,
                calculatedAmount = Money(calculatedAmount)
            )
        } else null

        return CreateServiceModel(
            name = command.name,
            basePrice = Money(basePrice),
            discount = discount,
            finalPrice = Money(finalPrice),
            approvalStatus = ApprovalStatus.valueOf(command.approvalStatus),
            note = command.note,
            quantity = command.quantity
        )
    }

    private fun mapToCreateMediaModel(command: CreateMediaCommand): CreateMediaTypeModel {
        return CreateMediaTypeModel(
            type = MediaType.valueOf(command.type),
            name = command.name,
            description = command.description,
            location = command.location,
            tags = command.tags
        )
    }
}