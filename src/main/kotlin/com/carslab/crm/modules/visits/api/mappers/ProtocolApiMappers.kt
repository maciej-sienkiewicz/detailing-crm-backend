package com.carslab.crm.modules.visits.api.mappers

import com.carslab.crm.modules.visits.api.dto.*
import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.*
import com.carslab.crm.modules.visits.api.commands.CreateServiceCommand
import com.carslab.crm.domain.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ProtocolApiMappers {

    private val DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME

    fun toCreateCommand(request: CreateProtocolRequest): CreateProtocolCommand {
        return CreateProtocolCommand(
            title = request.title,
            calendarColorId = request.calendarColorId,
            vehicle = CreateVehicleCommand(
                brand = request.vehicle.make,
                model = request.vehicle.model,
                licensePlate = request.vehicle.licensePlate,
                productionYear = request.vehicle.productionYear,
                vin = request.vehicle.vin,
                color = request.vehicle.color,
                mileage = request.vehicle.mileage
            ),
            client = CreateClientCommand(
                name = request.client.name,
                email = request.client.email,
                phone = request.client.phone,
                companyName = request.client.companyName,
                taxId = request.client.taxId
            ),
            period = CreatePeriodCommand(
                startDate = parseDateTime(request.startDate),
                endDate = request.endDate?.let { parseDateTime(it) } ?: parseDateTime(request.startDate).plusHours(8)
            ),
            status = request.status?.let { ProtocolStatus.valueOf(it) } ?: ProtocolStatus.SCHEDULED,
            services = request.services.map { toCreateServiceCommand(it) },
            notes = request.notes,
            referralSource = request.referralSource,
            documents = CreateDocumentsCommand(
                keysProvided = request.keysProvided ?: false,
                documentsProvided = request.documentsProvided ?: false
            ),
            appointmentId = request.appointmentId
        )
    }

    fun toUpdateCommand(request: UpdateProtocolRequest, protocolId: String): UpdateProtocolCommand {
        return UpdateProtocolCommand(
            protocolId = protocolId,
            title = request.title,
            calendarColorId = request.calendarColorId,
            vehicle = UpdateVehicleCommand(
                id = protocolId,
                brand = request.vehicle.make,
                model = request.vehicle.model,
                licensePlate = request.vehicle.licensePlate ?: "",
                productionYear = request.vehicle.productionYear,
                vin = request.vehicle.vin,
                color = request.vehicle.color,
                mileage = request.vehicle.mileage
            ),
            client = UpdateClientCommand(
                id = protocolId,
                name = request.client.name,
                email = request.client.email,
                phone = request.client.phone,
                companyName = request.client.companyName,
                taxId = request.client.taxId
            ),
            period = UpdatePeriodCommand(
                startDate = parseDateTime(request.startDate),
                endDate = request.endDate?.let { parseDateTime(it) } ?: parseDateTime(request.startDate).plusHours(8)
            ),
            status = ProtocolStatus.valueOf(request.status ?: "SCHEDULED"),
            services = request.services.map { toUpdateServiceCommand(it) },
            notes = request.notes,
            referralSource = request.referralSource,
            documents = UpdateDocumentsCommand(
                keysProvided = request.keysProvided ?: false,
                documentsProvided = request.documentsProvided ?: false
            ),
            appointmentId = request.appointmentId
        )
    }

    fun toCreateServiceCommand(request: CreateServiceRequest): com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateServiceCommand {
        return com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateServiceCommand(
            name = request.name,
            basePrice = request.price,
            quantity = request.quantity,
            discountType = request.discountType,
            discountValue = request.discountValue,
            finalPrice = request.finalPrice,
            approvalStatus = request.approvalStatus ?: "PENDING",
            note = request.note
        )
    }

    fun toCreateServiceCommand(apiCommand: CreateServiceCommand): com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateServiceCommand {
        return com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateServiceCommand(
            name = apiCommand.name,
            basePrice = apiCommand.price,
            quantity = apiCommand.quantity,
            discountType = apiCommand.discountType?.name,
            discountValue = apiCommand.discountValue,
            finalPrice = apiCommand.finalPrice,
            approvalStatus = apiCommand.approvalStatus?.name ?: "PENDING",
            note = apiCommand.note
        )
    }

    private fun toUpdateServiceCommand(request: CreateServiceRequest): UpdateServiceCommand {
        return UpdateServiceCommand(
            id = request.id ?: "",
            name = request.name,
            basePrice = request.price,
            quantity = request.quantity,
            discountType = request.discountType,
            discountValue = request.discountValue,
            finalPrice = request.finalPrice,
            approvalStatus = request.approvalStatus ?: "PENDING",
            note = request.note
        )
    }

    private fun parseDateTime(dateTimeString: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateTimeString, DATE_FORMATTER)
        } catch (e: Exception) {
            try {
                val date = java.time.LocalDate.parse(dateTimeString, java.time.format.DateTimeFormatter.ISO_DATE)
                LocalDateTime.of(date, java.time.LocalTime.of(8, 0))
            } catch (e2: Exception) {
                throw IllegalArgumentException("Invalid date format: $dateTimeString")
            }
        }
    }
}