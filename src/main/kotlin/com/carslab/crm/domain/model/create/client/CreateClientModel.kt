package com.carslab.crm.domain.model.create.client

import com.carslab.crm.clients.domain.model.ClientAudit

data class CreateClientModel(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val company: String? = null,
    val taxId: String? = null,
    val notes: String? = null,
    val audit: ClientAudit = ClientAudit()
)