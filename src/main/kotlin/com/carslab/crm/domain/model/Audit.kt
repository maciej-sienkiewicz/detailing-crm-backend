package com.carslab.crm.domain.model

import java.time.LocalDateTime

data class Audit(
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val createdBy: String? = null,
    val updatedBy: String? = null
)