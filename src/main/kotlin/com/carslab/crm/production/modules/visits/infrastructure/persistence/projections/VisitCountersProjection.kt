package com.carslab.crm.production.modules.visits.infrastructure.persistence.projections

interface VisitCountersProjection {
    val status: String
    val count: Long
}