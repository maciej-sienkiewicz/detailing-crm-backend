package com.carslab.crm.production.modules.visits.application.service.command.mapper

import com.carslab.crm.modules.visits.api.commands.UpdateCarReceptionCommand
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import com.carslab.crm.production.modules.visits.domain.command.CreateServiceCommand
import com.carslab.crm.production.modules.visits.domain.command.CreateVisitCommand
import com.carslab.crm.production.modules.visits.domain.command.DeliveryPerson
import com.carslab.crm.production.modules.visits.domain.command.UpdateServiceCommand
import com.carslab.crm.production.modules.visits.domain.command.UpdateVisitCommand
import com.carslab.crm.production.modules.visits.domain.orchestration.ClientDetails
import com.carslab.crm.production.modules.visits.domain.orchestration.VehicleDetails
import com.carslab.crm.production.modules.visits.domain.orchestration.VisitEntities
import com.carslab.crm.production.modules.visits.infrastructure.mapper.EnumMappers
import com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class VisitCommandMapper {

    fun mapCreateCommand(request: CreateVisitRequest, companyId: Long, entities: VisitEntities): CreateVisitCommand {
        return CreateVisitCommand(
            companyId = companyId,
            title = request.title,
            client = entities.client,
            vehicle = entities.vehicle,
            startDate = LocalDateTime.parse(request.startDate),
            endDate = request.endDate?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
            status = request.status?.let { EnumMappers.mapFromApiProtocolStatus(it) }
                ?: com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.SCHEDULED,
            services = request.selectedServices?.map { mapCreateServiceCommand(it) } ?: emptyList(),
            notes = request.notes,
            referralSource = request.referralSource?.let { EnumMappers.mapFromApiReferralSource(it) },
            appointmentId = request.appointmentId,
            calendarColorId = request.calendarColorId,
            keysProvided = request.keysProvided ?: false,
            documentsProvided = request.documentsProvided ?: false,
            deliveryPerson = request.deliveryPerson?.let { mapDeliveryPerson(it) }
        )
    }

    fun mapUpdateCommand(request: UpdateCarReceptionCommand): UpdateVisitCommand {
        return UpdateVisitCommand(
            title = request.title,
            startDate = LocalDateTime.parse(request.startDate),
            endDate = request.endDate?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
            services = request.selectedServices?.map { mapUpdateServiceCommand(it) } ?: emptyList(),
            notes = request.notes,
            referralSource = EnumMappers.mapToReferralSource(request.referralSource?.toString()),
            appointmentId = request.appointmentId,
            calendarColorId = request.calendarColorId,
            keysProvided = request.keysProvided ?: false,
            documentsProvided = request.documentsProvided ?: false,
            status = EnumMappers.mapToVisitStatus(request.status.toString()),
            deliveryPerson = request.deliveryPerson?.let { mapApiDeliveryPerson(it) },
            sendWithEmail = request.sendProtocolWithEmail
        )
    }

    fun mapClientDetails(request: CreateVisitRequest): ClientDetails {
        return ClientDetails(
            ownerId = request.ownerId,
            email = request.email,
            phone = request.phone,
            name = request.ownerName,
            companyName = request.companyName,
            taxId = request.taxId,
            address = request.address
        )
    }

    fun mapVehicleDetails(request: CreateVisitRequest): VehicleDetails {
        return VehicleDetails(
            make = request.make,
            model = request.model,
            licensePlate = request.licensePlate!!,
            productionYear = request.productionYear,
            vin = request.vin,
            color = request.color,
            mileage = request.mileage,
            ownerId = request.ownerId
        )
    }

    private fun mapCreateServiceCommand(service: com.carslab.crm.modules.visits.api.commands.CreateServiceCommand): CreateServiceCommand {
        return CreateServiceCommand(
            id = service.id,
            name = service.name,
            basePrice = CalculationUtils.anyToBigDecimal(service.price),
            quantity = service.quantity,
            discountType = EnumMappers.mapToDiscountType(service.discountType?.toString()),
            discountValue = service.discountValue?.let { CalculationUtils.anyToBigDecimal(it) },
            finalPrice = service.finalPrice?.let { CalculationUtils.anyToBigDecimal(it) },
            approvalStatus = EnumMappers.mapToServiceApprovalStatus(service.approvalStatus?.toString()),
            note = service.note
        )
    }

    private fun mapUpdateServiceCommand(service: com.carslab.crm.modules.visits.api.commands.UpdateServiceCommand): UpdateServiceCommand {
        return UpdateServiceCommand(
            id = service.id,
            name = service.name,
            basePrice = CalculationUtils.anyToBigDecimal(service.price),
            quantity = service.quantity,
            discountType = EnumMappers.mapToDiscountType(service.discountType?.toString()),
            discountValue = service.discountValue?.let { CalculationUtils.anyToBigDecimal(it) },
            finalPrice = service.finalPrice?.let { CalculationUtils.anyToBigDecimal(it) },
            approvalStatus = EnumMappers.mapToServiceApprovalStatus(service.approvalStatus?.toString()),
            note = service.note
        )
    }

    private fun mapDeliveryPerson(deliveryPerson: com.carslab.crm.production.modules.visits.application.dto.DeliveryPerson): DeliveryPerson {
        return DeliveryPerson(
            id = deliveryPerson.id,
            name = deliveryPerson.name,
            phone = deliveryPerson.phone
        )
    }

    private fun mapApiDeliveryPerson(deliveryPerson: DeliveryPerson): DeliveryPerson {
        return DeliveryPerson(
            id = deliveryPerson.id,
            name = deliveryPerson.name,
            phone = deliveryPerson.phone
        )
    }
}