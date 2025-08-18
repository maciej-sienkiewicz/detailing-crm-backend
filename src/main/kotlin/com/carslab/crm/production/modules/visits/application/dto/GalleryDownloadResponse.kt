package com.carslab.crm.production.modules.visits.application.dto

import org.springframework.core.io.Resource

data class GalleryDownloadResponse(
    val resource: Resource,
    val contentType: String,
    val originalName: String
)