package com.carslab.crm.modules.visits.api.response

data class ProtocolCountersResponse(
    val SCHEDULED: Int,
    val IN_PROGRESS: Int,
    val READY_FOR_PICKUP: Int,
    val COMPLETED: Int,
    val CANCELLED: Int,
    val ALL: Int
)