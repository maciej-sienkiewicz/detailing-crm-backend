package com.carslab.crm.production.modules.clients.domain.command

data class CreateClientCommand(
    val companyId: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String?,
    val company: String?,
    val taxId: String?,
    val notes: String?
)