package com.carslab.crm.production.modules.templates.domain.validator

import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class TemplateValidator {

    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf(
            "application/pdf",
            "text/html"
        )
        private const val MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
    }

    fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw BusinessException("File cannot be empty")
        }

        val contentType = file.contentType
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw BusinessException("Only PDF and HTML files are allowed")
        }

        if (file.size > MAX_FILE_SIZE) {
            throw BusinessException("File size cannot exceed 50MB")
        }
    }

    fun validateTemplateData(name: String, type: TemplateType) {
        if (name.isBlank()) {
            throw BusinessException("Template name cannot be blank")
        }

        if (name.length > 255) {
            throw BusinessException("Template name cannot exceed 255 characters")
        }
    }

    fun validateFileTypeForTemplateType(file: MultipartFile, templateType: TemplateType) {
        val fileContentType = file.contentType
        val requiredContentType = templateType.getRequiredContentType()

        if (requiredContentType != null && fileContentType != requiredContentType) {
            val expectedFileType = when (requiredContentType) {
                "application/pdf" -> "PDF"
                "text/html" -> "HTML"
                else -> "unknown"
            }
            throw BusinessException("Template type ${templateType.displayName} requires $expectedFileType file format")
        }
    }
}