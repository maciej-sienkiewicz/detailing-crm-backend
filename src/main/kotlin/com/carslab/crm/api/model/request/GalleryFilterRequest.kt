// src/main/kotlin/com/carslab/crm/api/model/request/GalleryFilterRequest.kt
package com.carslab.crm.api.model.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request object for filtering gallery images")
data class GalleryFilterRequest(
    @JsonProperty("tags")
    @Schema(description = "List of tags to filter by")
    val tags: List<String>? = null, // Zmienione na nullable

    @JsonProperty("tag_match_mode")
    @Schema(description = "Tag matching mode: 'ALL' or 'ANY'", allowableValues = ["ALL", "ANY"])
    val tagMatchMode: TagMatchMode? = null, // Zmienione na nullable

    @JsonProperty("protocol_id")
    @Schema(description = "Filter by specific protocol ID")
    val protocolId: String? = null,

    @JsonProperty("name")
    @Schema(description = "Filter by image name")
    val name: String? = null,

    @JsonProperty("start_date")
    @Schema(description = "Filter images created from this date")
    val startDate: String? = null,

    @JsonProperty("end_date")
    @Schema(description = "Filter images created until this date")
    val endDate: String? = null,

    @JsonProperty("page")
    @Schema(description = "Page number (0-based)")
    val page: Int? = null,

    @JsonProperty("size")
    @Schema(description = "Page size")
    val size: Int? = null
) {
    // Dodajemy funkcje pomocnicze dla bezpiecznego dostępu do wartości
    fun getTagsOrEmpty(): List<String> = tags ?: emptyList()
    fun getTagMatchModeOrDefault(): TagMatchMode = tagMatchMode ?: TagMatchMode.ANY
    fun getPageOrDefault(): Int = page ?: 0
    fun getSizeOrDefault(): Int = size ?: 20
}

enum class TagMatchMode {
    ALL, // Zdjęcie musi zawierać wszystkie podane tagi
    ANY  // Zdjęcie musi zawierać przynajmniej jeden z podanych tagów
}