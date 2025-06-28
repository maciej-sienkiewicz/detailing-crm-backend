// src/main/kotlin/com/carslab/crm/modules/visits/api/mappers/ServiceMappers.kt
package com.carslab.crm.modules.visits.api.mappers

import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateServiceCommand
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.domain.model.*

object ServiceMappers {

    fun toCreateServiceModel(command: CreateServiceCommand): CreateServiceModel {
        val basePrice = command.basePrice
        val discountValue = command.discountValue ?: 0.0
        val discountType = command.discountType?.let { mapApiDiscountTypeToDomain(it) }

        // Calculate final price if not provided
        val finalPrice = if (command.finalPrice != null) {
            command.finalPrice
        } else if (discountValue > 0 && discountType != null) {
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
            approvalStatus = mapApiApprovalStatusToDomain(command.approvalStatus),
            note = command.note,
            quantity = command.quantity
        )
    }

    private fun mapApiDiscountTypeToDomain(apiDiscountType: String): DiscountType {
        return when (apiDiscountType.uppercase()) {
            "PERCENTAGE" -> DiscountType.PERCENTAGE
            "AMOUNT" -> DiscountType.AMOUNT
            "FIXED_PRICE" -> DiscountType.FIXED_PRICE
            else -> throw IllegalArgumentException("Unknown discount type: $apiDiscountType")
        }
    }

    private fun mapApiApprovalStatusToDomain(apiApprovalStatus: String): ApprovalStatus {
        return when (apiApprovalStatus.uppercase()) {
            "PENDING" -> ApprovalStatus.PENDING
            "APPROVED" -> ApprovalStatus.APPROVED
            "REJECTED" -> ApprovalStatus.REJECTED
            else -> ApprovalStatus.PENDING // Default fallback
        }
    }

    private fun calculateDiscountedPrice(basePrice: Double, discountValue: Double, discountType: DiscountType): Double {
        return when (discountType) {
            DiscountType.PERCENTAGE -> basePrice * (1 - discountValue / 100)
            DiscountType.AMOUNT -> basePrice - discountValue
            DiscountType.FIXED_PRICE -> discountValue
        }
    }
}