package com.carslab.crm.production.modules.stats.infrastructure.dto

import java.math.BigDecimal

interface UncategorizedServiceProjection {
    fun getServiceId(): String
    fun getServiceName(): String
    fun getServicesCount(): Long
    fun getTotalRevenue(): BigDecimal
}