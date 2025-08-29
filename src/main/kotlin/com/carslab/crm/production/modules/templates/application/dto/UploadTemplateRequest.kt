package com.carslab.crm.production.modules.templates.application.dto

import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class UploadTemplateRequest(
    @field:NotNull(message = "File is required")
    val file: MultipartFile,

    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name cannot exceed 255 characters")
    val name: String,

    @field:NotNull(message = "Template type is required")
    val type: TemplateType,

    @field:Size(max = 1000, message = "Description cannot exceed 1000 characters")
    val description: String? = null,

    @JsonProperty("is_active")
    val isActive: Boolean = true
)

data class UpdateTemplateRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name cannot exceed 255 characters")
    val name: String,

    @JsonProperty("is_active")
    val isActive: Boolean = true
)