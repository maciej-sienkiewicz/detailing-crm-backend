// src/main/kotlin/com/carslab/crm/modules/visits/api/mappers/ProtocolApiMappers.kt
package com.carslab.crm.modules.visits.api.mappers

import com.carslab.crm.modules.visits.api.dto.*
import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.*
import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.domain.model.ProtocolStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ProtocolApiMappers {

    private val DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME

    fun toCreateCommand(request: CreateProtocolRequest): CreateProtocolCommand {
        return CreateProtocolCommand(
            title = request.title,
            calendarColorId = request.calendarColorId,
            vehicle = toCreateVehicleCommand(request.vehicle),
            client = toCreateClientCommand(request.client),
            period = CreatePeriodCommand(
                startDate = LocalDateTime.parse(request.startDate),
                endDate = request.endDate?.let { LocalDateTime.parse(it) } ?: LocalDateTime.parse(request.startDate).plusHours(8)
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

    fun toDetailResponse(readModel: ProtocolDetailReadModel): ProtocolDetailResponse {
        return ProtocolDetailResponse(
            id = readModel.id,
            title = readModel.title,
            calendarColorId = readModel.calendarColorId,
            vehicle = toVehicleResponse(readModel.vehicle),
            client = toClientResponse(readModel.client),
            period = toPeriodResponse(readModel.period),
            status = readModel.status,
            services = readModel.services.map { toServiceResponse(it) },
            notes = readModel.notes,
            createdAt = readModel.audit.createdAt,
            updatedAt = readModel.audit.updatedAt
        )
    }

    fun toListResponse(readModel: ProtocolListReadModel): ProtocolListResponse {
        return ProtocolListResponse(
            id = readModel.id,
            title = readModel.title,
            vehicle = toVehicleBasicResponse(readModel.vehicle),
            client = toClientBasicResponse(readModel.client),
            status = readModel.status,
            totalAmount = readModel.totalAmount,
            lastUpdate = readModel.lastUpdate
        )
    }

    private fun toCreateVehicleCommand(request: CreateVehicleRequest): CreateVehicleCommand {
        return CreateVehicleCommand(
            brand = request.make,
            model = request.model,
            licensePlate = request.licensePlate,
            productionYear = request.productionYear,
            vin = request.vin,
            color = request.color,
            mileage = request.mileage
        )
    }

    private fun toCreateClientCommand(request: CreateClientRequest): CreateClientCommand {
        return CreateClientCommand(
            name = request.name,
            email = request.email,
            phone = request.phone,
            companyName = request.companyName,
            taxId = request.taxId
        )
    }

    private fun toCreateServiceCommand(request: CreateServiceRequest): CreateServiceCommand {
        return CreateServiceCommand(
            name = request.name,
            basePrice = request.price,
            quantity = request.quantity,
            discountType = request.discountType,
            discountValue = request.discountValue,
            finalPrice = request.discountValue?.let { discount ->
                when (request.discountType) {
                    "PERCENTAGE" -> request.price * (1 - discount / 100)
                    "AMOUNT" -> request.price - discount
                    "FIXED_PRICE" -> discount
                    else -> request.price
                }
            } ?: request.price,
            note = request.note
        )
    }

    private fun toVehicleResponse(vehicle: VehicleReadModel): VehicleResponse {
        return VehicleResponse(
            make = vehicle.make,
            model = vehicle.model,
            licensePlate = vehicle.licensePlate,
            productionYear = vehicle.productionYear,
            color = vehicle.color
        )
    }

    private fun toVehicleBasicResponse(vehicle: VehicleBasicReadModel): VehicleBasicResponse {
        return VehicleBasicResponse(
            make = vehicle.make,
            model = vehicle.model,
            licensePlate = vehicle.licensePlate
        )
    }

    private fun toClientResponse(client: ClientReadModel): ClientResponse {
        return ClientResponse(
            name = client.name,
            email = client.email,
            phone = client.phone,
            companyName = client.companyName
        )
    }

    private fun toClientBasicResponse(client: ClientBasicReadModel): ClientBasicResponse {
        return ClientBasicResponse(
            name = client.name
        )
    }

    private fun toPeriodResponse(period: PeriodReadModel): PeriodResponse {
        return PeriodResponse(
            startDate = period.startDate,
            endDate = period.endDate
        )
    }

    private fun toServiceResponse(service: ServiceReadModel): ServiceResponse {
        return ServiceResponse(
            id = service.id,
            name = service.name,
            price = service.basePrice,
            quantity = service.quantity,
            finalPrice = service.finalPrice,
            status = service.approvalStatus
        )
    }
}