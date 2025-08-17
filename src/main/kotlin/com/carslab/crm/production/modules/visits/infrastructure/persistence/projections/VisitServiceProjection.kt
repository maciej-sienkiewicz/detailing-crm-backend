package com.carslab.crm.production.modules.visits.infrastructure.persistence.projections

import java.math.BigDecimal

interface VisitServiceProjection {
    val visitId: Long
    val serviceId: String
    val serviceName: String
    val finalPrice: BigDecimal
}