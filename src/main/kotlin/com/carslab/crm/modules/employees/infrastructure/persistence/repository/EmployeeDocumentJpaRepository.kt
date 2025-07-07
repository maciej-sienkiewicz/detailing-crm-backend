package com.carslab.crm.modules.employees.infrastructure.persistence.repository

import com.carslab.crm.modules.employees.infrastructure.persistence.entity.EmployeeDocumentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmployeeDocumentJpaRepository : JpaRepository<EmployeeDocumentEntity, String> {

    fun findByEmployeeIdOrderByCreatedAtDesc(employeeId: String): List<EmployeeDocumentEntity>

    fun findByEmployeeIdAndCompanyIdOrderByCreatedAtDesc(
        employeeId: String,
        companyId: Long
    ): List<EmployeeDocumentEntity>

    fun findByCompanyId(companyId: Long): List<EmployeeDocumentEntity>

    fun countByEmployeeId(employeeId: String): Long

    fun findByStorageId(storageId: String): EmployeeDocumentEntity?

    fun existsByStorageId(storageId: String): Boolean
}
