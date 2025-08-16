package com.carslab.crm.production.modules.clients.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateClientRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 100, message = "First name cannot exceed 100 characters")
    @JsonProperty("first_name")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 100, message = "Last name cannot exceed 100 characters")
    @JsonProperty("last_name")
    val lastName: String,

    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    val email: String? = null,

    @field:Size(max = 20, message = "Phone number cannot exceed 20 characters")
    val phone: String? = null,

    @field:Size(max = 500, message = "Address cannot exceed 500 characters")
    val address: String? = null,

    @field:Size(max = 200, message = "Company name cannot exceed 200 characters")
    val company: String? = null,

    @field:Size(max = 50, message = "Tax ID cannot exceed 50 characters")
    @JsonProperty("tax_id")
    val taxId: String? = null,

    val notes: String? = null
)

data class UpdateClientRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 100, message = "First name cannot exceed 100 characters")
    @JsonProperty("first_name")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 100, message = "Last name cannot exceed 100 characters")
    @JsonProperty("last_name")
    val lastName: String,

    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    val email: String,

    @field:Size(max = 20, message = "Phone number cannot exceed 20 characters")
    val phone: String,

    @field:Size(max = 500, message = "Address cannot exceed 500 characters")
    val address: String? = null,

    @field:Size(max = 200, message = "Company name cannot exceed 200 characters")
    val company: String? = null,

    @field:Size(max = 50, message = "Tax ID cannot exceed 50 characters")
    @JsonProperty("tax_id")
    val taxId: String? = null,

    val notes: String? = null
)