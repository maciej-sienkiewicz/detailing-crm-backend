// src/main/kotlin/com/carslab/crm/api/model/response/GalleryImageResponse.kt
package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class GalleryImageResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("protocol_id")
    val protocolId: String,

    @JsonProperty("protocol_title")
    val protocolTitle: String? = null,

    @JsonProperty("client_name")
    val clientName: String? = null,

    @JsonProperty("vehicle_info")
    val vehicleInfo: String? = null,

    @JsonProperty("size")
    val size: Long,

    @JsonProperty("content_type")
    val contentType: String,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("location")
    val location: String? = null,

    @JsonProperty("tags")
    val tags: List<String> = emptyList(),

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("thumbnail_url")
    val thumbnailUrl: String? = null,

    @JsonProperty("download_url")
    val downloadUrl: String
)

data class GalleryStatsResponse(
    @JsonProperty("total_images")
    val totalImages: Long,

    @JsonProperty("total_size")
    val totalSize: Long,

    @JsonProperty("available_tags")
    val availableTags: List<TagStatResponse>
)

data class TagStatResponse(
    @JsonProperty("tag")
    val tag: String,

    @JsonProperty("count")
    val count: Long
)