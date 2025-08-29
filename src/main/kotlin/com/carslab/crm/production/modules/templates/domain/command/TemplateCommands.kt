package com.carslab.crm.production.modules.templates.domain.command

import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import org.springframework.web.multipart.MultipartFile

data class CreateTemplateCommand(
    val companyId: Long,
    val file: MultipartFile,
    val name: String,
    val type: TemplateType,
    val isActive: Boolean
)

data class UpdateTemplateCommand(
    val templateId: String,
    val name: String,
    val isActive: Boolean,
    val companyId: Long
)