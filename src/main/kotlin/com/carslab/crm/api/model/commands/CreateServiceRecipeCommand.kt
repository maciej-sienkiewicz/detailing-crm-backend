package com.carslab.crm.api.model.commands

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Schema(description = "Request object for creating or updating a service")
data class CreateServiceRecipeCommand(
    @JsonProperty("id")
    @Schema(description = "Service ID - required for updates, ignored for creation")
    val id: String? = null,

    @JsonProperty("name")
    @field:NotBlank(message = "{validation.serviceRequest.name.notBlank}")
    @field:Size(min = 2, max = 100)
    @Schema(description = "Service name", example = "Detailing kompletny", required = true)
    val name: String,

    @JsonProperty("description")
    @field:Size(max = 500)
    @Schema(description = "Service description", example = "Pełny pakiet usług detailingowych: mycie, polerowanie, woskowanie.")
    val description: String? = null,

    @JsonProperty("price")
    @Schema(description = "Service price (net)", example = "1200.00", required = true)
    val price: BigDecimal,

    @JsonProperty("vat_rate")
    @Schema(description = "VAT rate (percentage)", example = "23", required = true)
    val vatRate: Int
)