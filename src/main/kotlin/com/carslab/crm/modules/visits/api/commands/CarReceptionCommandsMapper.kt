package com.carslab.crm.api.mapper

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.modules.visits.api.request.ApiDiscountType
import com.carslab.crm.modules.visits.api.request.ApiReferralSource
import com.carslab.crm.modules.visits.api.request.ServiceApprovalStatus
import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.protocol.*
import com.carslab.crm.modules.visits.api.commands.*
import java.time.format.DateTimeFormatter

object CarReceptionDtoMapper {
    private val DATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME

    fun mapApiStatusToDomain(apiStatus: ApiProtocolStatus?): ProtocolStatus {
        return when (apiStatus) {
            ApiProtocolStatus.SCHEDULED -> ProtocolStatus.SCHEDULED
            ApiProtocolStatus.PENDING_APPROVAL -> ProtocolStatus.PENDING_APPROVAL
            ApiProtocolStatus.IN_PROGRESS -> ProtocolStatus.IN_PROGRESS
            ApiProtocolStatus.READY_FOR_PICKUP -> ProtocolStatus.READY_FOR_PICKUP
            ApiProtocolStatus.COMPLETED -> ProtocolStatus.COMPLETED
            ApiProtocolStatus.CANCELLED -> ProtocolStatus.CANCELLED
            null -> ProtocolStatus.SCHEDULED
        }
    }

    fun toBasicDto(protocol: CarReceptionProtocol): CarReceptionBasicDto {
        return CarReceptionBasicDto(
            id = protocol.id.value,
            createdAt = protocol.audit.createdAt.format(DATETIME_FORMATTER),
            updatedAt = protocol.audit.updatedAt.format(DATETIME_FORMATTER),
            statusUpdatedAt = protocol.audit.statusUpdatedAt.format(DATETIME_FORMATTER),
            status = mapDomainStatusToApi(protocol.status)
        )
    }

    fun CreateVehicleImageCommand.fromCreateImageCommand() =
        CreateMediaTypeModel(
            type = MediaType.PHOTO,
            name = name ?: "Unknown",
            description = description,
            location = location,
            tags = tags
        )

    fun UpdateVehicleImageCommand.fromCreateImageCommand() =
        UpdateMediaTypeMode(
            name = name ?: "Unknown",
            description = description,
            location = location,
            tags = tags
        )

    fun mapCreateServiceCommandToService(command: CreateServiceCommand): CreateServiceModel {
        val basePrice = command.price
        val discountValue = command.discountValue ?: 0.0
        val discountType = command.discountType?.let { mapApiDiscountTypeToDomain(it) }

        val finalPrice = command.finalPrice ?: if (discountValue > 0 && discountType != null) {
            calculateDiscountedPrice(basePrice, discountValue, discountType)
        } else {
            basePrice
        }

        return CreateServiceModel(
            name = command.name,
            basePrice = Money(basePrice),
            discount = if (discountValue > 0 && discountType != null) {
                Discount(
                    type = discountType,
                    value = discountValue,
                    calculatedAmount = Money(basePrice - finalPrice)
                )
            } else null,
            finalPrice = Money(finalPrice),
            approvalStatus = command.approvalStatus?.let { mapApiApprovalStatusToDomain(it) } ?: ApprovalStatus.PENDING,
            note = command.note,
            quantity = command.quantity
        )
    }

    private fun calculateDiscountedPrice(basePrice: Double, discountValue: Double, discountType: DiscountType): Double {
        return when (discountType) {
            DiscountType.PERCENTAGE -> basePrice * (1 - discountValue / 100)
            DiscountType.AMOUNT -> basePrice - discountValue
            DiscountType.FIXED_PRICE -> discountValue
        }
    }

    private fun mapDomainStatusToApi(domainStatus: ProtocolStatus): ApiProtocolStatus {
        return when (domainStatus) {
            ProtocolStatus.SCHEDULED -> ApiProtocolStatus.SCHEDULED
            ProtocolStatus.PENDING_APPROVAL -> ApiProtocolStatus.PENDING_APPROVAL
            ProtocolStatus.IN_PROGRESS -> ApiProtocolStatus.IN_PROGRESS
            ProtocolStatus.READY_FOR_PICKUP -> ApiProtocolStatus.READY_FOR_PICKUP
            ProtocolStatus.COMPLETED -> ApiProtocolStatus.COMPLETED
            ProtocolStatus.CANCELLED -> ApiProtocolStatus.CANCELLED
        }
    }

    private fun mapApiDiscountTypeToDomain(apiDiscountType: ApiDiscountType): DiscountType {
        return when (apiDiscountType) {
            ApiDiscountType.PERCENTAGE -> DiscountType.PERCENTAGE
            ApiDiscountType.AMOUNT -> DiscountType.AMOUNT
            ApiDiscountType.FIXED_PRICE -> DiscountType.FIXED_PRICE
        }
    }

    private fun mapApiApprovalStatusToDomain(apiApprovalStatus: ServiceApprovalStatus): ApprovalStatus {
        return when (apiApprovalStatus) {
            ServiceApprovalStatus.PENDING -> ApprovalStatus.PENDING
            ServiceApprovalStatus.APPROVED -> ApprovalStatus.APPROVED
            ServiceApprovalStatus.REJECTED -> ApprovalStatus.REJECTED
        }
    }
}