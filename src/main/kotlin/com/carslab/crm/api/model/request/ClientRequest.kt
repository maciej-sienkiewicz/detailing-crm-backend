package com.carslab.crm.api.model.request

import com.fasterxml.jackson.annotation.JsonProperty

data class ClientRequest(
    @JsonProperty("id")
    var id: String? = null,

    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("phone")
    val phone: String,

    @JsonProperty("address")
    val address: String? = null,

    @JsonProperty("company")
    val company: String? = null,

    @JsonProperty("tax_id")
    val taxId: String? = null,

    @JsonProperty("notes")
    val notes: String? = null
)