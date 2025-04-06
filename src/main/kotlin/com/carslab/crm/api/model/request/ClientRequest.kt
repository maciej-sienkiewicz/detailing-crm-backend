package com.carslab.crm.api.model.request

import com.carslab.crm.infrastructure.validation.AtLeastOneNotBlank
import com.carslab.crm.infrastructure.validation.ValidPhoneNumber
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@AtLeastOneNotBlank(
    fieldNames = ["email", "phone"],
    message = "{validation.clientRequest.contactRequired}"
)
@Schema(description = "Request object for creating or updating a client")
data class ClientRequest(
    @JsonProperty("id")
    @Schema(description = "Client ID - required for updates, ignored for creation")
    var id: String? = null,

    @JsonProperty("first_name")
    @field:NotBlank(message = "{validation.clientRequest.firstName.notBlank}")
    @field:Size(min = 2, max = 50)
    @Schema(description = "Client's first name", example = "John", required = true)
    val firstName: String,

    @JsonProperty("last_name")
    @field:NotBlank(message = "{validation.clientRequest.lastName.notBlank}")
    @field:Size(min = 2, max = 50)
    @Schema(description = "Client's last name", example = "Smith", required = true)
    val lastName: String,

    @JsonProperty("email")
    @field:Email(message = "{validation.clientRequest.email.format}")
    @field:Size(max = 100)
    @Schema(description = "Client's email address", example = "john.smith@example.com")
    val email: String,

    @JsonProperty("phone")
    @field:ValidPhoneNumber(message = "{validation.clientRequest.phone.format}")
    @field:Size(max = 20)
    val phone: String,

    @JsonProperty("address")
    @field:Size(max = 200)
    @Schema(description = "Client's address")
    val address: String? = null,

    @JsonProperty("company")
    @field:Size(max = 100)
    @Schema(description = "Client's company name")
    val company: String? = null,

    @JsonProperty("tax_id")
    @field:Size(max = 30)
    @Schema(description = "Client's tax identification number")
    val taxId: String? = null,

    @JsonProperty("notes")
    @field:Size(max = 1000)
    @Schema(description = "Additional notes about the client")
    val notes: String? = null
)