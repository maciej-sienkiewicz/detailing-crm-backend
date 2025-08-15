package com.carslab.crm.production.modules.activities.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.domain.Page

data class ActivityPageResponse(
    val content: List<ActivityResponse>,
    val page: Int,
    val size: Int,
    @JsonProperty("total_elements")
    val totalElements: Long,
    @JsonProperty("total_pages")
    val totalPages: Int,
    @JsonProperty("is_last")
    val isLast: Boolean
) {
    companion object {
        fun from(page: Page<ActivityResponse>): ActivityPageResponse {
            return ActivityPageResponse(
                content = page.content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                isLast = page.isLast,
            )
        }
    }
}
