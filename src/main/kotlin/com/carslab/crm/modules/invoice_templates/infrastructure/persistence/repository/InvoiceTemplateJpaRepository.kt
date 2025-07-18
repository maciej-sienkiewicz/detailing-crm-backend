package com.carslab.crm.modules.invoice_templates.infrastructure.persistence.repository

import com.carslab.crm.modules.invoice_templates.domain.model.TemplateType
import com.carslab.crm.modules.invoice_templates.infrastructure.persistence.entity.InvoiceTemplateEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InvoiceTemplateJpaRepository : JpaRepository<InvoiceTemplateEntity, String> {

    fun findByCompanyIdAndIsActiveTrue(companyId: Long): List<InvoiceTemplateEntity>

    fun findByCompanyIdOrderByIsActiveDescCreatedAtDesc(companyId: Long): List<InvoiceTemplateEntity>

    fun findByTemplateTypeAndIsActiveTrue(type: TemplateType): List<InvoiceTemplateEntity>

    fun findByTemplateType(type: TemplateType): List<InvoiceTemplateEntity>

    fun findByCompanyIdAndNameAndIsActiveTrue(companyId: Long, name: String): InvoiceTemplateEntity?

    @Modifying
    @Query("UPDATE InvoiceTemplateEntity t SET t.isActive = false, t.updatedAt = CURRENT_TIMESTAMP WHERE t.companyId = :companyId")
    fun deactivateAllForCompany(@Param("companyId") companyId: Long)

    @Query("SELECT COUNT(t) FROM InvoiceTemplateEntity t WHERE t.companyId = :companyId AND t.isActive = true")
    fun countActiveTemplatesForCompany(@Param("companyId") companyId: Long): Long

    fun existsByCompanyIdAndName(companyId: Long, name: String): Boolean
}