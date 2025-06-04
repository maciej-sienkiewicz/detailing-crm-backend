package com.carslab.crm.signature.api.dto

import com.carslab.crm.validation.SafeString
import com.carslab.crm.validation.ValidLicensePlate
import jakarta.validation.constraints.*
import java.time.Instant
import java.util.UUID

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateSignatureSessionRequest(
    @field:NotNull
    val workstationId: Long,

    @field:NotBlank
    val customerName: String,

    @field:Valid
    val vehicleInfo: VehicleInfoRequest?,

    val serviceType: String?,
    val documentType: String?,
    val additionalNotes: String?
)

data class VehicleInfoRequest(
    val make: String?,
    val model: String?,
    val licensePlate: String?,
    val vin: String?,
    val year: Int?
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