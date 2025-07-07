package com.carslab.crm.modules.employees.domain.model

import com.carslab.crm.modules.employees.domain.model.shared.AuditInfo
import java.time.LocalDate

data class EmployeeId(val value: String) {
    companion object {
        fun generate(): EmployeeId = EmployeeId(java.util.UUID.randomUUID().toString())
        fun of(value: String): EmployeeId = EmployeeId(value)
    }
}

enum class UserRole {
    ADMIN, MANAGER, EMPLOYEE
}

enum class ContractType {
    EMPLOYMENT, B2B, MANDATE
}

data class Employee(
    val id: EmployeeId,
    val companyId: Long,
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
    val notes: String?,
    val audit: AuditInfo = AuditInfo()
) {
    fun validateBusinessRules() {
        require(fullName.isNotBlank()) { "Employee full name cannot be blank" }
        require(email.isNotBlank()) { "Employee email cannot be blank" }
        require(phone.isNotBlank()) { "Employee phone cannot be blank" }
        require(position.isNotBlank()) { "Employee position cannot be blank" }
        require(isValidEmail(email)) { "Invalid email format" }
        require(birthDate.isBefore(LocalDate.now())) { "Birth date cannot be in the future" }
        require(hireDate.isBefore(LocalDate.now().plusDays(1))) { "Hire date cannot be in the future" }

        hourlyRate?.let { rate ->
            require(rate >= 0) { "Hourly rate cannot be negative" }
        }

        bonusFromRevenue?.let { bonus ->
            require(bonus >= 0 && bonus <= 100) { "Bonus from revenue must be between 0 and 100 percent" }
        }

        workingHoursPerWeek?.let { hours ->
            require(hours > 0 && hours <= 168) { "Working hours per week must be between 0 and 168" }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"))
    }

    fun calculateAge(): Int {
        val today = LocalDate.now()
        var age = today.year - birthDate.year
        if (today.monthValue < birthDate.monthValue ||
            (today.monthValue == birthDate.monthValue && today.dayOfMonth < birthDate.dayOfMonth)) {
            age--
        }
        return age
    }

    fun calculateTenureInMonths(): Int {
        val today = LocalDate.now()
        return (today.year - hireDate.year) * 12 + (today.monthValue - hireDate.monthValue)
    }
}

data class EmergencyContact(
    val name: String,
    val phone: String
) {
    init {
        require(name.isNotBlank()) { "Emergency contact name cannot be blank" }
        require(phone.isNotBlank()) { "Emergency contact phone cannot be blank" }
    }
}

data class CreateEmployee(
    val companyId: Long,
    val fullName: String,
    val birthDate: LocalDate,
    val hireDate: LocalDate,
    val position: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val hourlyRate: Double? = null,
    val bonusFromRevenue: Double? = null,
    val isActive: Boolean = true,
    val workingHoursPerWeek: Double? = null,
    val contractType: ContractType? = null,
    val emergencyContact: EmergencyContact? = null,
    val notes: String? = null
)