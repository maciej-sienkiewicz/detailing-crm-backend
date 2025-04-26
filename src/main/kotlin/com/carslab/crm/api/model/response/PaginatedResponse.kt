package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO reprezentujące paginowaną odpowiedź.
 */
data class PaginatedResponse<T>(
    @JsonProperty("data")
    val data: List<T>,

    @JsonProperty("page")
    val page: Int,

    @JsonProperty("size")
    val size: Int,

    @JsonProperty("total_items")
    val totalItems: Long,

    @JsonProperty("total_pages")
    val totalPages: Long
)