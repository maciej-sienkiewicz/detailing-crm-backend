package com.carslab.crm.production.modules.clients.domain.command

data class UpdateClientCommand(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String?,
    val company: String?,
    val taxId: String?,
    val notes: String?
)