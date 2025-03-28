package com.carslab.crm.domain.model

import java.time.Instant

data class VehicleImage(
    val id: String,
    val name: String,
    val size: Long,
    val type: String,
    val storageId: String,
    val createdAt: Instant = Instant.now(),
    var description: String? = null,
    var location: String? = null,
    var protocol: CarReceptionProtocol? = null
)