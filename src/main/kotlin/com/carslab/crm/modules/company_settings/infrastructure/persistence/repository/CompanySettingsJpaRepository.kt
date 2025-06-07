package com.carslab.crm.company_settings.infrastructure.persistence.repository

import com.carslab.crm.company_settings.infrastructure.persistence.entity.CompanySettingsEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface CompanySettingsJpaRepository : JpaRepository<CompanySettingsEntity, Long> {

    @Query("SELECT cs FROM CompanySettingsEntity cs WHERE cs.companyId = :companyId AND cs.active = true")
    fun findByCompanyIdAndActiveTrue(@Param("companyId") companyId: Long): Optional<CompanySettingsEntity>

    @Query("SELECT cs FROM CompanySettingsEntity cs WHERE cs.id = :id AND cs.active = true")
    fun findByIdAndActiveTrue(@Param("id") id: Long): Optional<CompanySettingsEntity>

    fun existsByCompanyIdAndActiveTrue(companyId: Long): Boolean

    @Modifying
    @Query("UPDATE CompanySettingsEntity cs SET cs.active = false, cs.updatedAt = :now WHERE cs.companyId = :companyId")
    fun softDeleteByCompanyId(@Param("companyId") companyId: Long, @Param("now") now: LocalDateTime): Int

    @Query("SELECT COUNT(cs) FROM CompanySettingsEntity cs WHERE cs.active = true")
    fun countActiveSettings(): Long

    @Query("SELECT cs FROM CompanySettingsEntity cs WHERE cs.taxId = :taxId AND cs.active = true")
    fun findByTaxIdAndActiveTrue(@Param("taxId") taxId: String): Optional<CompanySettingsEntity>

    @Query("SELECT cs FROM CompanySettingsEntity cs WHERE cs.logoFileId = :logoFileId AND cs.active = true")
    fun findByLogoFileIdAndActiveTrue(@Param("logoFileId") logoFileId: String): Optional<CompanySettingsEntity>
}