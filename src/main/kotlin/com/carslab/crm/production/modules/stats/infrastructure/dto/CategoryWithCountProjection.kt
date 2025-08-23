package com.carslab.crm.production.modules.stats.infrastructure.dto

interface CategoryWithCountProjection {
    fun getCategoryId(): Long
    fun getCategoryName(): String
    fun getServicesCount(): Long
}