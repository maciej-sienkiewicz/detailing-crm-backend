package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class VehicleImageResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("size")
    val size: Long,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    val createdAt: Instant,

    @JsonProperty("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    val updatedAt: Instant = Instant.now(),

    @JsonProperty("tags")
    val tags: List<String> = emptyList(),

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("location")
    val location: String? = null
)