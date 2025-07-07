// src/main/kotlin/com/carslab/crm/modules/employees/infrastructure/persistence/repository/EmployeeJpaRepository.kt
package com.carslab.crm.modules.employees.infrastructure.persistence.repository

import com.carslab.crm.modules.employees.infrastructure.persistence.entity.EmployeeEntity
import com.carslab.crm.modules.employees.domain.model.UserRole
import com.carslab.crm.modules.employees.domain.model.ContractType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface EmployeeJpaRepository : JpaRepository<EmployeeEntity, String>, JpaSpecificationExecutor<EmployeeEntity> {

    fun findByCompanyId(companyId: Long): List<EmployeeEntity>

    fun findByEmailAndCompanyId(email: String, companyId: Long): EmployeeEntity?

    fun existsByEmailAndCompanyId(email: String, companyId: Long): Boolean

    fun findByCompanyIdAndIsActive(companyId: Long, isActive: Boolean): List<EmployeeEntity>

    fun countByCompanyId(companyId: Long): Long

    fun countByCompanyIdAndIsActive(companyId: Long, isActive: Boolean): Long

    fun countByCompanyIdAndRole(companyId: Long, role: UserRole): Long

    /**
     * Calculate average age using native PostgreSQL query
     */
    @Query(
        value = """
            SELECT AVG(EXTRACT(YEAR FROM AGE(CURRENT_DATE, birth_date))) 
            FROM employees 
            WHERE company_id = :companyId AND is_active = true
        """,
        nativeQuery = true
    )
    fun getAverageAge(@Param("companyId") companyId: Long): Double?

    /**
     * Calculate average tenure in months using native PostgreSQL query
     */
    @Query(
        value = """
            SELECT AVG(
                EXTRACT(YEAR FROM AGE(CURRENT_DATE, hire_date)) * 12 + 
                EXTRACT(MONTH FROM AGE(CURRENT_DATE, hire_date))
            ) 
            FROM employees 
            WHERE company_id = :companyId AND is_active = true
        """,
        nativeQuery = true
    )
    fun getAverageTenureInMonths(@Param("companyId") companyId: Long): Double?

    /**
     * Get role distribution using native query
     */
    @Query(
        value = """
            SELECT role, COUNT(*) 
            FROM employees 
            WHERE company_id = :companyId AND is_active = true 
            GROUP BY role
        """,
        nativeQuery = true
    )
    fun getRoleDistribution(@Param("companyId") companyId: Long): List<Array<Any>>

    /**
     * Get contract type distribution using native query
     */
    @Query(
        value = """
            SELECT COALESCE(contract_type, 'UNKNOWN'), COUNT(*) 
            FROM employees 
            WHERE company_id = :companyId AND is_active = true 
            GROUP BY contract_type
        """,
        nativeQuery = true
    )
    fun getContractTypeDistribution(@Param("companyId") companyId: Long): List<Array<Any>>
}