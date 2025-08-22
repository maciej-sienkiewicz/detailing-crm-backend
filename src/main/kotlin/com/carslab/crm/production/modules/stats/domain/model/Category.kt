package com.carslab.crm.production.modules.stats.domain.model

data class CategoryId(val id: Long)

class Category(
    val id: CategoryId,
    val name: String,
    val servicesCount: Int,
)