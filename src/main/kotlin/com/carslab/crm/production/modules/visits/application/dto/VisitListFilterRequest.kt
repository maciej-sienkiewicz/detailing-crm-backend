package com.carslab.crm.production.modules.visits.application.dto

import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class VisitListFilterRequest(
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
) {
    fun hasAnyFilter(): Boolean {
        return  clientId != null || 
                clientName != null ||
                licensePlate != null ||
                status != null ||
                startDate != null ||
                endDate != null ||
                make != null ||
                model != null ||
                serviceName != null ||
                !serviceIds.isNullOrEmpty() ||
                title != null ||
                minPrice != null ||
                maxPrice != null
    }
}