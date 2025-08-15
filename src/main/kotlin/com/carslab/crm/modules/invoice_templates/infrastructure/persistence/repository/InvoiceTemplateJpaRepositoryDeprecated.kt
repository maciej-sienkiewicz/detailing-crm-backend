package com.carslab.crm.modules.invoice_templates.infrastructure.persistence.repository

import com.carslab.crm.modules.invoice_templates.domain.model.TemplateType
import com.carslab.crm.modules.invoice_templates.infrastructure.persistence.entity.InvoiceTemplateEntityDeprecated
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InvoiceTemplateJpaRepositoryDeprecated : JpaRepository<InvoiceTemplateEntityDeprecated, String> {

    fun findByCompanyIdAndIsActiveTrue(companyId: Long): List<InvoiceTemplateEntityDeprecated>

    fun findByCompanyIdOrderByIsActiveDescCreatedAtDesc(companyId: Long): List<InvoiceTemplateEntityDeprecated>

    fun findByTemplateTypeAndIsActiveTrue(type: TemplateType): List<InvoiceTemplateEntityDeprecated>

    fun findByTemplateType(type: TemplateType): List<InvoiceTemplateEntityDeprecated>

    fun findByCompanyIdAndNameAndIsActiveTrue(companyId: Long, name: String): InvoiceTemplateEntityDeprecated?

    @Modifying
    @Query("UPDATE InvoiceTemplateEntityDeprecated t SET t.isActive = false, t.updatedAt = CURRENT_TIMESTAMP WHERE t.companyId = :companyId")
    fun deactivateAllForCompany(@Param("companyId") companyId: Long)

    @Query("SELECT COUNT(t) FROM InvoiceTemplateEntityDeprecated t WHERE t.companyId = :companyId AND t.isActive = true")
    fun countActiveTemplatesForCompany(@Param("companyId") companyId: Long): Long

    fun existsByCompanyIdAndName(companyId: Long, name: String): Boolean
}