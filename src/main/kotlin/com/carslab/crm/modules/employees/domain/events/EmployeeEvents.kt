package com.carslab.crm.modules.employees.domain.events

import com.carslab.crm.infrastructure.events.BaseDomainEvent
import com.carslab.crm.modules.employees.domain.model.ContractType
import com.carslab.crm.modules.employees.domain.model.UserRole
import java.time.LocalDate

data class EmployeeCreatedEvent(
    val employeeId: String,
    val fullName: String,
    val position: String,
    val email: String,
    val role: UserRole,
    val hireDate: LocalDate,
    val hourlyRate: Double?,
    val bonusFromRevenue: Double?,
    val workingHoursPerWeek: Double?,
    val contractType: ContractType?,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = employeeId,
    aggregateType = "EMPLOYEE",
    eventType = "EMPLOYEE_CREATED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "fullName" to fullName,
        "position" to position,
        "email" to email,
        "role" to role.name,
        "hireDate" to hireDate.toString(),
        "hourlyRate" to hourlyRate,
        "bonusFromRevenue" to bonusFromRevenue,
        "workingHoursPerWeek" to workingHoursPerWeek,
        "contractType" to contractType?.name
    ) + additionalMetadata
) {
    fun hasSalaryInformation(): Boolean {
        return hourlyRate != null && hourlyRate > 0.0
    }

    fun calculateMonthlyGrossSalary(): Double? {
        return hourlyRate?.let { rate ->
            val hoursPerWeek = workingHoursPerWeek ?: 40.0
            val hoursPerMonth = hoursPerWeek * 4.33
            rate * hoursPerMonth
        }
    }
}

data class EmployeeUpdatedEvent(
    val employeeId: String,
    val fullName: String,
    val position: String,
    val previousHourlyRate: Double?,
    val newHourlyRate: Double?,
    val previousWorkingHours: Double?,
    val newWorkingHours: Double?,
    val contractType: ContractType?,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = employeeId,
    aggregateType = "EMPLOYEE",
    eventType = "EMPLOYEE_UPDATED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "fullName" to fullName,
        "position" to position,
        "previousHourlyRate" to previousHourlyRate,
        "newHourlyRate" to newHourlyRate,
        "previousWorkingHours" to previousWorkingHours,
        "newWorkingHours" to newWorkingHours,
        "contractType" to contractType?.name
    ) + additionalMetadata
) {
    fun hasSalaryChanged(): Boolean {
        return previousHourlyRate != newHourlyRate || previousWorkingHours != newWorkingHours
    }

    fun calculatePreviousMonthlyGrossSalary(): Double? {
        return previousHourlyRate?.let { rate ->
            val hoursPerWeek = previousWorkingHours ?: 40.0
            val hoursPerMonth = hoursPerWeek * 4.33
            rate * hoursPerMonth
        }
    }

    fun calculateNewMonthlyGrossSalary(): Double? {
        return newHourlyRate?.let { rate ->
            val hoursPerWeek = newWorkingHours ?: 40.0
            val hoursPerMonth = hoursPerWeek * 4.33
            rate * hoursPerMonth
        }
    }
}

data class EmployeeDeactivatedEvent(
    val employeeId: String,
    val fullName: String,
    val position: String,
    val hourlyRate: Double?,
    val workingHoursPerWeek: Double?,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = employeeId,
    aggregateType = "EMPLOYEE",
    eventType = "EMPLOYEE_DEACTIVATED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "fullName" to fullName,
        "position" to position,
        "hourlyRate" to hourlyRate,
        "workingHoursPerWeek" to workingHoursPerWeek
    ) + additionalMetadata
)