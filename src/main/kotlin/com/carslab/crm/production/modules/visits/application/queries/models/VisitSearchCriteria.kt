package com.carslab.crm.production.modules.visits.application.queries.models

import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class VisitSearchCriteria(
    val companyId: Long,
    val clientId: String? = null,
    val clientName: String? = null,
    val licensePlate: String? = null,
    val status: VisitStatus? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val make: String? = null,
    val model: String? = null,
    val serviceName: String? = null,
    val serviceIds: List<String>? = null,
    val title: String? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null
)

data class VisitCountersReadModel(
    val scheduled: Long,
    val inProgress: Long,
    val readyForPickup: Long,
    val completed: Long,
    val cancelled: Long,
    val all: Long
)