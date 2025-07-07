// src/main/kotlin/com/carslab/crm/modules/employees/infrastructure/persistence/entity/EmployeeEntity.kt
package com.carslab.crm.modules.employees.infrastructure.persistence.entity

import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.domain.model.shared.AuditInfo
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "employees",
    indexes = [
        Index(name = "idx_employees_company_id", columnList = "company_id"),
        Index(name = "idx_employees_email", columnList = "email"),
        Index(name = "idx_employees_active", columnList = "is_active"),
        Index(name = "idx_employees_role", columnList = "role")
    ]
)
class EmployeeEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "full_name", nullable = false)
    var fullName: String,

    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate,

    @Column(name = "hire_date", nullable = false)
    var hireDate: LocalDate,

    @Column(name = "position", nullable = false)
    var position: String,

    @Column(name = "email", nullable = false)
    var email: String,

    @Column(name = "phone", nullable = false)
    var phone: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: UserRole,

    @Column(name = "hourly_rate")
    var hourlyRate: Double? = null,

    @Column(name = "bonus_from_revenue")
    var bonusFromRevenue: Double? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean,

    @Column(name = "working_hours_per_week")
    var workingHoursPerWeek: Double? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type")
    var contractType: ContractType? = null,

    @Column(name = "emergency_contact_name")
    var emergencyContactName: String? = null,

    @Column(name = "emergency_contact_phone")
    var emergencyContactPhone: String? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "updated_by")
    var updatedBy: String? = null,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
) {

    fun toDomain(): Employee {
        val emergencyContact = if (emergencyContactName != null && emergencyContactPhone != null) {
            EmergencyContact(emergencyContactName!!, emergencyContactPhone!!)
        } else null

        return Employee(
            id = EmployeeId.of(id),
            companyId = companyId,
            fullName = fullName,
            birthDate = birthDate,
            hireDate = hireDate,
            position = position,
            email = email,
            phone = phone,
            role = role,
            hourlyRate = hourlyRate,
            bonusFromRevenue = bonusFromRevenue,
            isActive = isActive,
            workingHoursPerWeek = workingHoursPerWeek,
            contractType = contractType,
            emergencyContact = emergencyContact,
            notes = notes,
            audit = AuditInfo(
                createdAt = createdAt,
                updatedAt = updatedAt,
                createdBy = createdBy,
                updatedBy = updatedBy,
                version = version
            )
        )
    }

    companion object {
        fun fromDomain(employee: Employee): EmployeeEntity = EmployeeEntity(
            id = employee.id.value,
            companyId = employee.companyId,
            fullName = employee.fullName,
            birthDate = employee.birthDate,
            hireDate = employee.hireDate,
            position = employee.position,
            email = employee.email,
            phone = employee.phone,
            role = employee.role,
            hourlyRate = employee.hourlyRate,
            bonusFromRevenue = employee.bonusFromRevenue,
            isActive = employee.isActive,
            workingHoursPerWeek = employee.workingHoursPerWeek,
            contractType = employee.contractType,
            emergencyContactName = employee.emergencyContact?.name,
            emergencyContactPhone = employee.emergencyContact?.phone,
            notes = employee.notes,
            createdAt = employee.audit.createdAt,
            updatedAt = employee.audit.updatedAt,
            createdBy = employee.audit.createdBy,
            updatedBy = employee.audit.updatedBy,
            version = employee.audit.version
        )

        fun fromCreateDomain(createEmployee: CreateEmployee, employeeId: String): EmployeeEntity = EmployeeEntity(
            id = employeeId,
            companyId = createEmployee.companyId,
            fullName = createEmployee.fullName,
            birthDate = createEmployee.birthDate,
            hireDate = createEmployee.hireDate,
            position = createEmployee.position,
            email = createEmployee.email,
            phone = createEmployee.phone,
            role = createEmployee.role,
            hourlyRate = createEmployee.hourlyRate,
            bonusFromRevenue = createEmployee.bonusFromRevenue,
            isActive = createEmployee.isActive,
            workingHoursPerWeek = createEmployee.workingHoursPerWeek,
            contractType = createEmployee.contractType,
            emergencyContactName = createEmployee.emergencyContact?.name,
            emergencyContactPhone = createEmployee.emergencyContact?.phone,
            notes = createEmployee.notes
        )
    }

    fun updateFromDomain(employee: Employee) {
        fullName = employee.fullName
        birthDate = employee.birthDate
        hireDate = employee.hireDate
        position = employee.position
        email = employee.email
        phone = employee.phone
        role = employee.role
        hourlyRate = employee.hourlyRate
        bonusFromRevenue = employee.bonusFromRevenue
        isActive = employee.isActive
        workingHoursPerWeek = employee.workingHoursPerWeek
        contractType = employee.contractType
        emergencyContactName = employee.emergencyContact?.name
        emergencyContactPhone = employee.emergencyContact?.phone
        notes = employee.notes
        updatedAt = employee.audit.updatedAt
        updatedBy = employee.audit.updatedBy
        version = employee.audit.version
    }
}