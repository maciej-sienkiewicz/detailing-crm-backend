// src/main/kotlin/com/carslab/crm/modules/employees/application/commands/models/EmployeeCommands.kt
package com.carslab.crm.modules.employees.application.commands.models

import com.carslab.crm.infrastructure.cqrs.Command
import com.carslab.crm.modules.employees.domain.model.*
import java.time.LocalDate

data class CreateEmployeeCommand(
    val fullName: String,
    val birthDate: LocalDate,
    val hireDate: LocalDate,
    val position: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val hourlyRate: Double?,
    val bonusFromRevenue: Double?,
    val isActive: Boolean,
    val workingHoursPerWeek: Double?,
    val contractType: ContractType?,
    val emergencyContact: EmergencyContact?,
    val notes: String?
) : Command<String>

data class UpdateEmployeeCommand(
    val id: String,
    val fullName: String,
    val birthDate: LocalDate,
    val hireDate: LocalDate,
    val position: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val hourlyRate: Double?,
    val bonusFromRevenue: Double?,
    val isActive: Boolean,
    val workingHoursPerWeek: Double?,
    val contractType: ContractType?,
    val emergencyContact: EmergencyContact?,
    val notes: String?
) : Command<String>

data class DeleteEmployeeCommand(
    val id: String
) : Command<Boolean>

data class CreateEmployeeDocumentCommand(
    val employeeId: String,
    val name: String,
    val type: String,
    val fileUrl: String?,
    val fileSize: Long?,
    val mimeType: String?
) : Command<String>

data class DeleteEmployeeDocumentCommand(
    val id: String
) : Command<Boolean>