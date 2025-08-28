package com.carslab.crm.production.modules.visits.domain.service.details.media

import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class MediaFileValidator {

    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
        )
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
    }

    fun validateFile(file: MultipartFile) {
        validateFileNotEmpty(file)
        validateContentType(file)
        validateFileSize(file)
    }

    private fun validateFileNotEmpty(file: MultipartFile) {
        if (file.isEmpty) {
            throw BusinessException("File cannot be empty")
        }
    }

    private fun validateContentType(file: MultipartFile) {
        val contentType = file.contentType
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw BusinessException("Only image files are allowed (JPEG, PNG, GIF, WebP)")
        }
    }

    private fun validateFileSize(file: MultipartFile) {
        if (file.size > MAX_FILE_SIZE) {
            throw BusinessException("File size cannot exceed 10MB")
        }
    }
}