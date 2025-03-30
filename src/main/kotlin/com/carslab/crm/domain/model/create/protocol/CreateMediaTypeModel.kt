package com.carslab.crm.domain.model.create.protocol

import com.carslab.crm.domain.model.MediaType
import java.time.LocalDateTime

class CreateMediaTypeModel(
    val type: MediaType,
    val name: String,
    val url: String? = null,
    val description: String? = null,
    val location: String? = null
)