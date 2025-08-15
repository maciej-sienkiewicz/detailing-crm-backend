package com.carslab.crm.production.modules.services.infrastructure.repository

import com.carslab.crm.production.modules.services.infrastructure.entity.ServiceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ServiceJpaRepository : JpaRepository<ServiceEntity, String> {

    fun findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(companyId: Long): List<ServiceEntity>

    fun findByIdAndIsActiveTrue(id: String): ServiceEntity?

    fun existsByCompanyIdAndNameAndIsActiveTrue(companyId: Long, name: String): Boolean

    @Modifying
    @Query("UPDATE ServiceEntity s SET s.isActive = false, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
    fun deactivateById(@Param("id") id: String)
}