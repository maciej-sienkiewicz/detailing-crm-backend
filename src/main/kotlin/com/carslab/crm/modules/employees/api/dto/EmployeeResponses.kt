// src/main/kotlin/com/carslab/crm/modules/employees/api/dto/EmployeeResponses.kt
package com.carslab.crm.modules.employees.api.dto

import com.carslab.crm.modules.employees.application.queries.models.*
import com.carslab.crm.modules.employees.domain.model.*
import com.fasterxml.jackson.annotation.JsonProperty

data class EmployeeListItemDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("full_name")
    val fullName: String,

    @JsonProperty("position")
    val position: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("phone")
    val phone: String,

    @JsonProperty("role")
    val role: UserRole,

    @JsonProperty("is_active")
    val isActive: Boolean,

    @JsonProperty("hire_date")
    val hireDate: String,

    @JsonProperty("last_login_date")
    val lastLoginDate: String? = null,

    @JsonProperty("hourly_rate")
    val hourlyRate: Double? = null,

    @JsonProperty("bonus_from_revenue")
    val bonusFromRevenue: Double? = null,

    @JsonProperty("working_hours_per_week")
    val workingHoursPerWeek: Double? = null,

    @JsonProperty("contract_type")
    val contractType: ContractType? = null
) {
    companion object {
        fun from(readModel: EmployeeReadModel): EmployeeListItemDto = EmployeeListItemDto(
            id = readModel.id,
            fullName = readModel.fullName,
            position = readModel.position,
            email = readModel.email,
            phone = readModel.phone,
            role = readModel.role,
            isActive = readModel.isActive,
            hireDate = readModel.hireDate,
            lastLoginDate = readModel.lastLoginDate,
            hourlyRate = readModel.hourlyRate,
            bonusFromRevenue = readModel.bonusFromRevenue,
            workingHoursPerWeek = readModel.workingHoursPerWeek,
            contractType = readModel.contractType
        )
    }
}

data class ExtendedEmployeeDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("full_name")
    val fullName: String,

    @JsonProperty("birth_date")
    val birthDate: String,

    @JsonProperty("hire_date")
    val hireDate: String,

    @JsonProperty("position")
    val position: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("phone")
    val phone: String,

    @JsonProperty("role")
    val role: UserRole,

    @JsonProperty("hourly_rate")
    val hourlyRate: Double? = null,

    @JsonProperty("bonus_from_revenue")
    val bonusFromRevenue: Double? = null,

    @JsonProperty("is_active")
    val isActive: Boolean,

    @JsonProperty("working_hours_per_week")
    val workingHoursPerWeek: Double? = null,

    @JsonProperty("contract_type")
    val contractType: ContractType? = null,

    @JsonProperty("emergency_contact")
    val emergencyContact: EmergencyContactDto? = null,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("age")
    val age: Int,

    @JsonProperty("tenure_in_months")
    val tenureInMonths: Int,

    @JsonProperty("created_at")
    val createdAt: String,

    @JsonProperty("updated_at")
    val updatedAt: String
) {
    companion object {
        fun from(readModel: EmployeeDetailReadModel): ExtendedEmployeeDto {
            val emergencyContact = readModel.emergencyContact?.let {
                EmergencyContactDto(it.name, it.phone)
            }

            return ExtendedEmployeeDto(
                id = readModel.id,
                fullName = readModel.fullName,
                birthDate = readModel.birthDate,
                hireDate = readModel.hireDate,
                position = readModel.position,
                email = readModel.email,
                phone = readModel.phone,
                role = readModel.role,
                hourlyRate = readModel.hourlyRate,
                bonusFromRevenue = readModel.bonusFromRevenue,
                isActive = readModel.isActive,
                workingHoursPerWeek = readModel.workingHoursPerWeek,
                contractType = readModel.contractType,
                emergencyContact = emergencyContact,
                notes = readModel.notes,
                age = readModel.age,
                tenureInMonths = readModel.tenureInMonths,
                createdAt = readModel.createdAt,
                updatedAt = readModel.updatedAt
            )
        }
    }
}

data class EmployeeDocumentDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("employee_id")
    val employeeId: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("upload_date")
    val uploadDate: String,

    @JsonProperty("download_url")
    val downloadUrl: String? = null,

    @JsonProperty("file_size")
    val fileSize: Long? = null,

    @JsonProperty("mime_type")
    val mimeType: String? = null
) {
    companion object {
        fun from(readModel: EmployeeDocumentReadModel): EmployeeDocumentDto = EmployeeDocumentDto(
            id = readModel.id,
            employeeId = readModel.employeeId,
            name = readModel.name,
            type = readModel.type,
            description = readModel.description,
            uploadDate = readModel.uploadDate,
            downloadUrl = readModel.downloadUrl,
            fileSize = readModel.fileSize,
            mimeType = readModel.mimeType
        )
    }
}

data class EmployeeStatisticsDto(
    @JsonProperty("total_employees")
    val totalEmployees: Int,

    @JsonProperty("active_employees")
    val activeEmployees: Int,

    @JsonProperty("inactive_employees")
    val inactiveEmployees: Int,

    @JsonProperty("average_age")
    val averageAge: Double,

    @JsonProperty("average_tenure")
    val averageTenure: Double,

    @JsonProperty("role_distribution")
    val roleDistribution: Map<UserRole, Int>,

    @JsonProperty("contract_type_distribution")
    val contractTypeDistribution: Map<ContractType, Int>
) {
    companion object {
        fun from(readModel: EmployeeStatisticsReadModel): EmployeeStatisticsDto = EmployeeStatisticsDto(
            totalEmployees = readModel.totalEmployees,
            activeEmployees = readModel.activeEmployees,
            inactiveEmployees = readModel.inactiveEmployees,
            averageAge = readModel.averageAge,
            averageTenure = readModel.averageTenure,
            roleDistribution = readModel.roleDistribution,
            contractTypeDistribution = readModel.contractTypeDistribution
        )
    }
}