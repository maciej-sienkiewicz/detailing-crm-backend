package com.carslab.crm.modules.visits.infrastructure.processor.dto

import com.carslab.crm.modules.visits.domain.valueobjects.MediaMetadata
import org.springframework.web.multipart.MultipartFile

data class MediaUploadData(
    val fileData: MultipartFile,
    val metadata: MediaMetadata
)