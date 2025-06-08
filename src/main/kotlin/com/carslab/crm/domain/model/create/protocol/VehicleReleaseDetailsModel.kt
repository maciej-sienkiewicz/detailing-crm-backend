package com.carslab.crm.domain.model.create.protocol

import java.time.LocalDateTime

enum class PaymentMethod {
    CASH,
    CARD;

    companion object {
        fun fromString(value: String): PaymentMethod {
            return when (value.uppercase()) {
                "CASH" -> CASH
                "CARD" -> CARD
                else -> throw IllegalArgumentException("Unknown payment method: $value")
            }
        }
    }
}

enum class DocumentType {
    INVOICE,
    RECEIPT,
    OTHER;

    companion object {
        fun fromString(value: String): DocumentType {
            return when (value.uppercase()) {
                "INVOICE" -> INVOICE
                "RECEIPT" -> RECEIPT
                "OTHER" -> OTHER
                else -> throw IllegalArgumentException("Unknown document type: $value")
            }
        }
    }
}

data class VehicleReleaseDetailsModel(
    val paymentMethod: PaymentMethod,
    val documentType: DocumentType,
    val releaseDate: LocalDateTime = LocalDateTime.now(),
    val additionalNotes: String? = null
)