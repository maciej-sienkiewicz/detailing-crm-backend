package com.carslab.crm.production.modules.invoice_templates.application.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class UploadTemplateRequest(
    @field:NotNull(message = "File is required")
    val file: MultipartFile,

    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name cannot exceed 100 characters")
    val name: String,

    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String? = null
)