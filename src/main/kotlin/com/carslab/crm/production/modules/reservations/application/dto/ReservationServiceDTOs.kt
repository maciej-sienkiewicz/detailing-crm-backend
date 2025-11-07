package com.carslab.crm.production.modules.reservations.application.dto

import com.carslab.crm.production.shared.presentation.dto.DiscountDto
import com.carslab.crm.production.shared.presentation.dto.DiscountResponseDto
import com.carslab.crm.production.shared.presentation.dto.PriceDto
import com.carslab.crm.production.shared.presentation.dto.PriceResponseDto
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * Request do tworzenia usługi w rezerwacji.
 *
 * Frontend przesyła intencję cenową i rabatową, backend wykonuje obliczenia.
 */
data class CreateReservationServiceRequest(
    @field:NotBlank(message = "Service ID is required")
    @field:Size(max = 36, message = "Service ID cannot exceed 36 characters")
    @JsonProperty("service_id")
    val serviceId: String,

    @field:NotBlank(message = "Service name is required")
    @field:Size(max = 100, message = "Service name cannot exceed 100 characters")
    @JsonProperty("name")
    val name: String,

    @field:NotNull(message = "Base price is required")
    @field:Valid
    @JsonProperty("base_price")
    val basePrice: PriceDto,

    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    @JsonProperty("quantity")
    val quantity: Long = 1,

    @field:Valid
    @JsonProperty("discount")
    val discount: DiscountDto? = null,

    @field:Size(max = 500, message = "Note cannot exceed 500 characters")
    @JsonProperty("note")
    val note: String? = null
)

/**
 * Response dla usługi w rezerwacji.
 *
 * Zawiera pełne informacje finansowe z zastosowanym rabatem.
 */
data class ReservationServiceResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("base_price")
    val basePrice: PriceResponseDto,

    @JsonProperty("quantity")
    val quantity: Long,

    @JsonProperty("unit_price")
    val unitPrice: PriceResponseDto,

    @JsonProperty("final_price")
    val finalPrice: PriceResponseDto,

    @JsonProperty("discount")
    val discount: DiscountResponseDto?,

    @JsonProperty("note")
    val note: String?
)

/**
 * Request do dodawania usług do istniejącej rezerwacji.
 */
data class AddServicesToReservationRequest(
    @field:NotNull(message = "Services list cannot be null")
    @field:Size(min = 1, message = "At least one service is required")
    @field:Valid
    @JsonProperty("services")
    val services: List<CreateReservationServiceRequest>
)

/**
 * Request do usunięcia usługi z rezerwacji.
 */
data class RemoveServiceFromReservationRequest(
    @field:NotBlank(message = "Service ID is required")
    @JsonProperty("service_id")
    val serviceId: String
)

/**
 * Request do aktualizacji usług w rezerwacji.
 */
data class UpdateReservationServicesRequest(
    @field:NotNull(message = "Services list cannot be null")
    @field:Valid
    @JsonProperty("services")
    val services: List<UpdateReservationServiceRequest>
)

/**
 * Request do aktualizacji pojedynczej usługi.
 */
data class UpdateReservationServiceRequest(
    @field:NotBlank(message = "Service ID is required")
    @JsonProperty("service_id")
    val serviceId: String,

    @field:NotBlank(message = "Service name is required")
    @field:Size(max = 100, message = "Service name cannot exceed 100 characters")
    @JsonProperty("name")
    val name: String,

    @field:NotNull(message = "Base price is required")
    @field:Valid
    @JsonProperty("base_price")
    val basePrice: PriceDto,

    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    @JsonProperty("quantity")
    val quantity: Long,

    @field:Valid
    @JsonProperty("discount")
    val discount: DiscountDto? = null,

    @field:Size(max = 500, message = "Note cannot exceed 500 characters")
    @JsonProperty("note")
    val note: String? = null
)