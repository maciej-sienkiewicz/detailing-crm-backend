package com.carslab.crm.signature.api.dto

data class CancelSessionRequest(
    val reason: String? = null
)

enum class SignatureStatus {
    PENDING,
    SENT_TO_TABLET,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    ERROR
}