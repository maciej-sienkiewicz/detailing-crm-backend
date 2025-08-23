package com.carslab.crm.production.modules.stats.infrastructure.dto

import java.math.BigDecimal

interface UncategorizedServiceProjection {
    fun getServiceId(): String
    fun getServiceName(): String
    fun getServicesCount(): Long
    fun getTotalRevenue(): BigDecimal
}

interface CategoryStatsSummaryProjection {
    fun getCategoryId(): Long
    fun getCategoryName(): String
    fun getTotalOrders(): Long
    fun getTotalRevenue(): BigDecimal
    fun getServicesCount(): Long
}

interface TimeSeriesProjection {
    fun getPeriod(): String
    fun getOrders(): Long
    fun getRevenue(): BigDecimal
}

interface ServiceStatsProjection {
    fun getServiceName(): String
}