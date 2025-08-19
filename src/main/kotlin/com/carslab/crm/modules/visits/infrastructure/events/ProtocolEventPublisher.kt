package com.carslab.crm.modules.visits.infrastructure.events

data class CurrentUser(
    val id: String,
    val name: String,
    val companyId: Long
)