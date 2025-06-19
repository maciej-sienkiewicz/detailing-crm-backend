package com.carslab.crm.modules.visits.infrastructure.processor

import com.carslab.crm.modules.visits.infrastructure.processor.dto.MediaUploadData
import org.springframework.web.multipart.MultipartHttpServletRequest

interface MultipartRequestProcessor {
    fun canProcess(request: MultipartHttpServletRequest): Boolean
    fun extractMediaData(request: MultipartHttpServletRequest): MediaUploadData
}