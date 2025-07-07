// src/main/kotlin/com/carslab/crm/modules/employees/api/dto/EmployeeRequests.kt
package com.carslab.crm.modules.employees.api.dto

import com.carslab.crm.modules.employees.domain.model.*
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*
import jakarta.validation.Valid
import java.time.LocalDate

data class EmployeeCreatePayloadDto(
    @field:NotBlank(message = "Full name is required")
    @field:Size(max = 200, message = "Full name cannot exceed 200 characters")
    @JsonProperty("full_name")
    val fullName: String,

    @field:NotNull(message = "Birth date is required")
    @JsonProperty("birth_date")
    val birthDate: LocalDate,

    @field:NotNull(message = "Hire date is required")
    @JsonProperty("hire_date")
    val hireDate: LocalDate,

    @field:NotBlank(message = "Position is required")
    @field:Size(max = 100, message = "Position cannot exceed 100 characters")
    @JsonProperty("position")
    val position: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    @JsonProperty("email")
    val email: String,

    @field:NotBlank(message = "Phone is required")
    @field:Size(max = 20, message = "Phone cannot exceed 20 characters")
    @JsonProperty("phone")
    val phone: String,

    @field:NotNull(message = "Role is required")
    @JsonProperty("role")
    val role: UserRole,

    @field:DecimalMin(value = "0.0", message = "Hourly rate cannot be negative")
    @JsonProperty("hourly_rate")
    val hourlyRate: Double? = null,

    @field:DecimalMin(value = "0.0", message = "Bonus cannot be negative")
    @field:DecimalMax(value = "100.0", message = "Bonus cannot exceed 100%")
    @JsonProperty("bonus_from_revenue")
    val bonusFromRevenue: Double? = null,

    @field:NotNull(message = "Active status is required")
    @JsonProperty("is_active")
    val isActive: Boolean,

    @field:DecimalMin(value = "0.1", message = "Working hours must be positive")
    @field:DecimalMax(value = "168.0", message = "Working hours cannot exceed 168 per week")
    @JsonProperty("working_hours_per_week")
    val workingHoursPerWeek: Double? = null,

    @field:Valid
    @JsonProperty("contract_type")
    val contractType: ContractType? = null,

    @field:Valid
    @JsonProperty("emergency_contact")
    val emergencyContact: EmergencyContactDto? = null,

    @field:Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @JsonProperty("notes")
    val notes: String? = null
)

data class EmployeeUpdatePayloadDto(
    @field:NotBlank(message = "ID is required")
    @JsonProperty("id")
    val id: String,

    @field:NotBlank(message = "Full name is required")
    @field:Size(max = 200, message = "Full name cannot exceed 200 characters")
    @JsonProperty("full_name")
    val fullName: String,

    @field:NotNull(message = "Birth date is required")
    @JsonProperty("birth_date")
    val birthDate: LocalDate,

    @field:NotNull(message = "Hire date is required")
    @JsonProperty("hire_date")
    val hireDate: LocalDate,

    @field:NotBlank(message = "Position is required")
    @field:Size(max = 100, message = "Position cannot exceed 100 characters")
    @JsonProperty("position")
    val position: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    @JsonProperty("email")
    val email: String,

    @field:NotBlank(message = "Phone is required")
    @field:Size(max = 20, message = "Phone cannot exceed 20 characters")
    @JsonProperty("phone")
    val phone: String,

    @field:NotNull(message = "Role is required")
    @JsonProperty("role")
    val role: UserRole,

    @field:DecimalMin(value = "0.0", message = "Hourly rate cannot be negative")
    @JsonProperty("hourly_rate")
    val hourlyRate: Double? = null,

    @field:DecimalMin(value = "0.0", message = "Bonus cannot be negative")
    @field:DecimalMax(value = "100.0", message = "Bonus cannot exceed 100%")
    @JsonProperty("bonus_from_revenue")
    val bonusFromRevenue: Double? = null,

    @field:NotNull(message = "Active status is required")
    @JsonProperty("is_active")
    val isActive: Boolean,

    @field:DecimalMin(value = "0.1", message = "Working hours must be positive")
    @field:DecimalMax(value = "168.0", message = "Working hours cannot exceed 168 per week")
    @JsonProperty("working_hours_per_week")
    val workingHoursPerWeek: Double? = null,

    @JsonProperty("contract_type")
    val contractType: ContractType? = null,

    @field:Valid
    @JsonProperty("emergency_contact")
    val emergencyContact: EmergencyContactDto? = null,

    @field:Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @JsonProperty("notes")
    val notes: String? = null
)

data class EmergencyContactDto(
    @field:NotBlank(message = "Emergency contact name is required")
    @field:Size(max = 200, message = "Name cannot exceed 200 characters")
    @JsonProperty("name")
    val name: String,

    @field:NotBlank(message = "Emergency contact phone is required")
    @field:Size(max = 20, message = "Phone cannot exceed 20 characters")
    @JsonProperty("phone")
    val phone: String
)

data class DocumentUploadPayloadDto(
    @field:NotBlank(message = "Employee ID is required")
    @JsonProperty("employee_id")
    val employeeId: String,

    @field:NotBlank(message = "Document name is required")
    @field:Size(max = 255, message = "Name cannot exceed 255 characters")
    @JsonProperty("name")
    val name: String,

    @field:NotBlank(message = "Document type is required")
    @field:Size(max = 100, message = "Type cannot exceed 100 characters")
    @JsonProperty("type")
    val type: String,

    @JsonProperty("file")
    val file: org.springframework.web.multipart.MultipartFile? = null
)