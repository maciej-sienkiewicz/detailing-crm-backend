package com.carslab.crm.production.modules.events.domain.models.value_objects

import java.time.Duration

data class VisitTemplate(
    val clientId: Long? = null,
    val vehicleId: Long? = null,
    val estimatedDuration: Duration,
    val defaultServices: List<ServiceTemplate> = emptyList(),
    val notes: String? = null
) {
    init {
        require(!estimatedDuration.isNegative) { "Estimated duration cannot be negative" }
    }
}

data class ServiceTemplate(
    val name: String,
    val basePrice: java.math.BigDecimal
) {
    init {
        require(name.isNotBlank()) { "Service name cannot be blank" }
        require(basePrice >= java.math.BigDecimal.ZERO) { "Base price cannot be negative" }
    }
}