package com.carslab.crm.production.modules.clients.domain.model

data class ClientWithStatistics(
    val client: Client,
    val statistics: ClientStatistics?,
    val vehicleIds: List<Long> = emptyList()
)