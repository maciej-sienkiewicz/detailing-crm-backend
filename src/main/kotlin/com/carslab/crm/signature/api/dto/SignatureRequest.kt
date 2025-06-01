package com.carslab.crm.signature.api.dto

import com.carslab.crm.validation.SafeString
import com.carslab.crm.validation.ValidLicensePlate
import jakarta.validation.constraints.*
import java.time.Instant
import java.util.UUID

data class CreateSignatureSessionRequest(
    @field:NotNull
    val workstationId: UUID,

    @field:NotNull
    val customerId: UUID,

    @field:NotBlank
    @field:Size(min = 2, max = 100)
    @field:SafeString
    val customerName: String,

    @field:NotBlank
    @field:Size(min = 1, max = 50)
    @field:SafeString
    val vehicleMake: String,

    @field:NotBlank
    @field:Size(min = 1, max = 50)
    @field:SafeString
    val vehicleModel: String,

    @field:NotBlank
    @field:ValidLicensePlate
    val licensePlate: String,

    @field:Size(max = 17)
    @field:Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "Invalid VIN format")
    val vin: String? = null,

    @field:NotBlank
    @field:Size(min = 2, max = 100)
    @field:SafeString
    val serviceType: String,

    @field:NotNull
    val documentId: UUID,

    @field:NotBlank
    @field:Size(min = 2, max = 50)
    @field:SafeString
    val documentType: String
)

data class SignatureSubmission(
    @field:NotBlank
    @field:Pattern(regexp = "^[a-fA-F0-9-]{36}$")
    val sessionId: String,

    @field:NotBlank
    @field:Pattern(regexp = "^data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+$")
    @field:Size(max = 5_000_000) // 5MB limit
    val signatureImage: String,

    @field:NotNull
    @field:PastOrPresent
    val signedAt: Instant,

    @field:NotNull
    val deviceId: UUID
)