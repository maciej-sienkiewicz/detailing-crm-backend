package com.carslab.crm.modules.company_settings.domain

import org.springframework.web.multipart.MultipartFile

interface LogoStorageService {
    fun storeLogo(companyId: Long, logoFile: MultipartFile): LogoMetadata
    fun deleteLogo(logoFileId: String): Boolean
    fun getLogoUrl(logoFileId: String): String?
    fun logoExists(logoFileId: String): Boolean
    fun getLogoPath(logoFileId: String): java.nio.file.Path?
}

data class LogoMetadata(
    val fileId: String,
    val fileName: String,
    val contentType: String,
    val size: Long,
    val url: String
)