package com.carslab.crm.finances.infrastructure.repository.fixedcosts

import com.carslab.crm.finances.infrastructure.entity.BreakevenConfigurationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface BreakevenConfigurationJpaRepository : JpaRepository<BreakevenConfigurationEntity, String> {

    fun findByCompanyIdAndIsActiveTrue(companyId: Long): Optional<BreakevenConfigurationEntity>

    fun findByCompanyIdOrderByCreatedAtDesc(companyId: Long): List<BreakevenConfigurationEntity>

    @Modifying
    @Query("UPDATE BreakevenConfigurationEntity e SET e.isActive = false WHERE e.companyId = :companyId")
    fun deactivateAllForCompany(@Param("companyId") companyId: Long): Int
}