package com.carslab.crm.production.modules.invoice_templates.infrastructure.repository

import com.carslab.crm.production.modules.invoice_templates.infrastructure.entity.InvoiceTemplateEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InvoiceTemplateJpaRepository : JpaRepository<InvoiceTemplateEntity, String> {

    fun findByCompanyIdOrderByIsActiveDescCreatedAtDesc(companyId: Long): List<InvoiceTemplateEntity>

    fun findByCompanyIdAndIsActiveTrue(companyId: Long): InvoiceTemplateEntity?

    fun existsByCompanyIdAndName(companyId: Long, name: String): Boolean

    @Modifying
    @Query("UPDATE InvoiceTemplateEntity t SET t.isActive = false, t.updatedAt = CURRENT_TIMESTAMP WHERE t.companyId = :companyId")
    fun deactivateAllForCompany(@Param("companyId") companyId: Long)
}