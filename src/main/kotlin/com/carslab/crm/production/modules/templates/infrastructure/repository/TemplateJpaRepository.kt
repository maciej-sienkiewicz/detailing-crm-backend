package com.carslab.crm.production.modules.templates.infrastructure.repository

import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import com.carslab.crm.production.modules.templates.infrastructure.entity.TemplateEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TemplateJpaRepository : JpaRepository<TemplateEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: Long): Optional<TemplateEntity>

    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<TemplateEntity>

    fun findByCompanyIdAndType(companyId: Long, type: TemplateType, pageable: Pageable): Page<TemplateEntity>

    fun findByCompanyIdAndIsActive(companyId: Long, isActive: Boolean, pageable: Pageable): Page<TemplateEntity>

    fun findByCompanyIdAndTypeAndIsActive(
        companyId: Long,
        type: TemplateType,
        isActive: Boolean,
        pageable: Pageable
    ): Page<TemplateEntity>

    @Query("SELECT t FROM TemplateEntity t WHERE t.companyId = :companyId AND t.type = :type AND t.isActive = true")
    fun findActiveByTypeAndCompany(@Param("companyId") companyId: Long, @Param("type") type: TemplateType): Optional<TemplateEntity>

    @Query("SELECT t FROM TemplateEntity t WHERE t.companyId = :companyId AND t.type = :type AND t.isActive = true")
    fun findAllActiveByTypeAndCompany(@Param("companyId") companyId: Long, @Param("type") type: TemplateType): List<TemplateEntity>

    fun existsByIdAndCompanyId(id: String, companyId: Long): Boolean

    @Modifying
    @Query("DELETE FROM TemplateEntity t WHERE t.id = :id AND t.companyId = :companyId")
    fun deleteByIdAndCompanyId(@Param("id") id: String, @Param("companyId") companyId: Long): Int
}