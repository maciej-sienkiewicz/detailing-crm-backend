package com.carslab.crm.api.model.commands

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Command for creating a new calendar color
 */
@Schema(description = "Command for creating a new calendar color")
data class CreateCalendarColorCommand(
    @field:NotBlank(message = "Name cannot be empty")
    @field:Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @JsonProperty("name")
    @Schema(description = "Name identifying the color", example = "Meeting", required = true)
    val name: String,

    @field:NotBlank(message = "Color cannot be empty")
    @field:Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Invalid HEX color format")
    @JsonProperty("color")
    @Schema(description = "HEX color value", example = "#3498db", required = true)
    val color: String
)

/**
 * Command for updating an existing calendar color
 */
@Schema(description = "Command for updating an existing calendar color")
data class UpdateCalendarColorCommand(
    @JsonProperty("id")
    @Schema(description = "Calendar color ID", required = false)
    val id: String? = null,

    @field:NotBlank(message = "Name cannot be empty")
    @field:Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @JsonProperty("name")
    @Schema(description = "Name identifying the color", example = "Meeting", required = true)
    val name: String,

    @field:NotBlank(message = "Color cannot be empty")
    @field:Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Invalid HEX color format")
    @JsonProperty("color")
    @Schema(description = "HEX color value", example = "#3498db", required = true)
    val color: String
)