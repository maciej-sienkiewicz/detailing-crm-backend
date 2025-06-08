package com.carslab.crm.domain.model.create.protocol

import com.carslab.crm.domain.model.MediaType

class CreateMediaTypeModel(
    val type: MediaType,
    val name: String,
    val url: String? = null,
    val description: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList()
)

class UpdateMediaTypeMode(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList()
)