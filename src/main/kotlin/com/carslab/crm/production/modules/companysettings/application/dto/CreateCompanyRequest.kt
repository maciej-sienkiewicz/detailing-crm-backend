package com.carslab.crm.production.modules.companysettings.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateCompanyRequest(
    @field:NotBlank(message = "Company name is required")
    @field:Size(max = 200, message = "Company name cannot exceed 200 characters")
    @JsonProperty("company_name")
    val companyName: String,

    @field:NotBlank(message = "Tax ID is required")
    @field:Size(max = 20, message = "Tax ID cannot exceed 20 characters")
    @JsonProperty("tax_id")
    val taxId: String,

    @field:Size(max = 500, message = "Address cannot exceed 500 characters")
    @JsonProperty("address")
    val address: String,

    @field:Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @JsonProperty("phone")
    val phone: String? = null,

    @field:Size(max = 255, message = "Website cannot exceed 255 characters")
    @JsonProperty("website")
    val website: String? = null,

    @field:Size(max = 50, message = "Bank account number cannot exceed 50 characters")
    @JsonProperty("bank_account_number")
    val bankAccountNumber: String? = null,

    @field:Size(max = 100, message = "Bank name cannot exceed 100 characters")
    @JsonProperty("bank_name")
    val bankName: String? = null,

    @field:Size(max = 20, message = "SWIFT code cannot exceed 20 characters")
    @JsonProperty("swift_code")
    val swiftCode: String? = null,

    @field:Size(max = 200, message = "Account holder name cannot exceed 200 characters")
    @JsonProperty("account_holder_name")
    val accountHolderName: String? = null
)