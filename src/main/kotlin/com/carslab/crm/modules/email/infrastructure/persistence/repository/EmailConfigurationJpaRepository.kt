package com.carslab.crm.modules.email.infrastructure.persistence.repository

import com.carslab.crm.modules.email.infrastructure.persistence.entity.EmailConfigurationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface EmailConfigurationJpaRepository : JpaRepository<EmailConfigurationEntity, Long> {

    fun findByCompanyId(companyId: Long): Optional<EmailConfigurationEntity>

    fun existsByCompanyId(companyId: Long): Boolean

    @Modifying
    @Query("DELETE FROM EmailConfigurationEntity e WHERE e.companyId = :companyId")
    fun deleteByCompanyId(@Param("companyId") companyId: Long): Int

    @Query("SELECT e FROM EmailConfigurationEntity e WHERE e.companyId = :companyId AND e.isEnabled = true")
    fun findEnabledByCompanyId(@Param("companyId") companyId: Long): Optional<EmailConfigurationEntity>

    @Query("SELECT COUNT(e) FROM EmailConfigurationEntity e WHERE e.isEnabled = true")
    fun countEnabledConfigurations(): Long
}