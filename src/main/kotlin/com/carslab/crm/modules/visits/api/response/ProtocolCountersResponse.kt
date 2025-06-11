package com.carslab.crm.modules.visits.api.response

import java.time.LocalDate
import java.time.LocalDateTime

data class ProtocolCountersResponse(
    val SCHEDULED: Int,
    val IN_PROGRESS: Int,
    val READY_FOR_PICKUP: Int,
    val COMPLETED: Int,
    val CANCELLED: Int,
    val ALL: Int
)

data class AbandonedVisitsCleanupResult(
    val updatedProtocolsCount: Int,
    val commentsAddedCount: Int,
    val processedDate: LocalDate,
    val executionTimestamp: LocalDateTime
)
