package com.carslab.crm.production.modules.companysettings.domain.command

import org.springframework.web.multipart.MultipartFile

data class UploadLogoCommand(
    val companyId: Long,
    val logoFile: MultipartFile
)