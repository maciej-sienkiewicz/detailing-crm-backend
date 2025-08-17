package com.carslab.crm.production.modules.visits.application.queries.models

data class GetMediaQuery(
    val data: ByteArray,
    val contentType: String,
    val originalName: String,
    val size: Long
)