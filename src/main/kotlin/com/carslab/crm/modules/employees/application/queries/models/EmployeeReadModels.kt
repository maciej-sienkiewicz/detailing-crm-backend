package com.carslab.crm.modules.employees.application.queries.models

import com.carslab.crm.modules.employees.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime

data class EmployeeReadModel(
    val id: String,
    val fullName: String,
    val position: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val isActive: Boolean,
    val hireDate: String,
    val lastLoginDate: String? = null,
    val hourlyRate: Double? = null,
    val bonusFromRevenue: Double? = null,
    val workingHoursPerWeek: Double? = null,
    val contractType: ContractType? = null
)

data class EmployeeDetailReadModel(
    val id: String,
    val fullName: String,
    val birthDate: String,
    val hireDate: String,
    val position: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val hourlyRate: Double? = null,
    val bonusFromRevenue: Double? = null,
    val isActive: Boolean,
    val workingHoursPerWeek: Double? = null,
    val contractType: ContractType? = null,
    val emergencyContact: EmergencyContactReadModel? = null,
    val notes: String? = null,
    val age: Int,
    val tenureInMonths: Int,
    val createdAt: String,
    val updatedAt: String
)

data class EmergencyContactReadModel(
    val name: String,
    val phone: String
)

data class EmployeeDocumentReadModel(
    val id: String,
    val employeeId: String,
    val name: String,
    val type: String,
    val uploadDate: String,
    val fileUrl: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null
)

data class EmployeeStatisticsReadModel(
    val totalEmployees: Int,
    val activeEmployees: Int,
    val inactiveEmployees: Int,
    val averageAge: Double,
    val averageTenure: Double,
    val roleDistribution: Map<UserRole, Int>,
    val contractTypeDistribution: Map<ContractType, Int>
)