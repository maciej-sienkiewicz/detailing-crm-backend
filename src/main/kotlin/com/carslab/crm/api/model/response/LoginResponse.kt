package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class LoginResponse(
    @JsonProperty("token")
    val token: String,

    @JsonProperty("user_id")
    val userId: Long,

    @JsonProperty("username")
    val username: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String,

    @JsonProperty("company_id")
    val companyId: Long,

    @JsonProperty("roles")
    val roles: List<String>
)