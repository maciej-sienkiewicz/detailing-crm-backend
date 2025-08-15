package com.carslab.crm.production.modules.invoice_templates.domain.command

data class CreateTemplateCommand(
    val companyId: Long,
    val name: String,
    val description: String?,
    val htmlContent: String
)