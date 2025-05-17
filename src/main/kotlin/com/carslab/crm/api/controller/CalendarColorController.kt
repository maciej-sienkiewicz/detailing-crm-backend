package com.carslab.crm.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.commands.CreateCalendarColorCommand
import com.carslab.crm.api.model.commands.UpdateCalendarColorCommand
import com.carslab.crm.api.model.response.CalendarColorResponse
import com.carslab.crm.domain.settings.CalendarColorFacade
import com.carslab.crm.domain.model.view.calendar.CalendarColorId
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.util.ValidationUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/calendar/colors")
@Tag(name = "Calendar Colors", description = "Calendar color management endpoints")
class CalendarColorController(
    private val calendarColorFacade: CalendarColorFacade
) : BaseController() {

    @GetMapping
    @Operation(summary = "Get all calendar colors", description = "Retrieves all calendar colors")
    fun getAllCalendarColors(): ResponseEntity<List<CalendarColorResponse>> {
        logger.info("Getting all calendar colors")

        val colors = calendarColorFacade.getAllCalendarColors()
        return ok(colors)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get calendar color by ID", description = "Retrieves a specific calendar color by its ID")
    fun getCalendarColorById(
        @Parameter(description = "Calendar color ID", required = true) @PathVariable id: String
    ): ResponseEntity<CalendarColorResponse> {
        logger.info("Getting calendar color by ID: $id")

        val color = calendarColorFacade.getCalendarColorById(CalendarColorId(id))
            ?: throw ResourceNotFoundException("Calendar color", id)

        return ok(color)
    }

    @PostMapping
    @Operation(summary = "Create a new calendar color", description = "Creates a new calendar color with the provided information")
    fun createCalendarColor(@Valid @RequestBody command: CreateCalendarColorCommand): ResponseEntity<CalendarColorResponse> {
        logger.info("Creating new calendar color: ${command.name}")

        try {
            ValidationUtils.validateNotBlank(command.name, "Name")
            ValidationUtils.validateNotBlank(command.color, "Color")
            validateHexColor(command.color)

            // Check if color name is already taken
            if (calendarColorFacade.isColorNameTaken(command.name)) {
                return badRequest("Calendar color name '${command.name}' is already taken")
            }

            val createdColor = calendarColorFacade.createCalendarColor(command)
            logger.info("Successfully created calendar color with ID: ${createdColor.id}")
            return created(createdColor)
        } catch (e: Exception) {
            return logAndRethrow("Error creating calendar color", e)
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update calendar color", description = "Updates an existing calendar color with the provided information")
    fun updateCalendarColor(
        @Parameter(description = "Calendar color ID", required = true) @PathVariable id: String,
        @Valid @RequestBody command: UpdateCalendarColorCommand
    ): ResponseEntity<CalendarColorResponse> {
        logger.info("Updating calendar color with ID: $id")

        try {
            ValidationUtils.validateNotBlank(command.name, "Name")
            ValidationUtils.validateNotBlank(command.color, "Color")
            validateHexColor(command.color)

            // Check if calendar color exists
            val existingColor = calendarColorFacade.getCalendarColorById(CalendarColorId(id))
                ?: throw ResourceNotFoundException("Calendar color", id)

            // Check if color name is already taken by another color
            if (command.name != existingColor.name && calendarColorFacade.isColorNameTaken(command.name, id)) {
                return badRequest("Calendar color name '${command.name}' is already taken")
            }

            val updatedCommand = command.copy(id = id)
            val updatedColor = calendarColorFacade.updateCalendarColor(updatedCommand)
            logger.info("Successfully updated calendar color with ID: $id")
            return ok(updatedColor)
        } catch (e: Exception) {
            return logAndRethrow("Error updating calendar color with ID: $id", e)
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete calendar color", description = "Deletes a calendar color by its ID")
    fun deleteCalendarColor(
        @Parameter(description = "Calendar color ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting calendar color with ID: $id")

        val deleted = calendarColorFacade.deleteCalendarColor(CalendarColorId(id))

        return if (deleted) {
            logger.info("Successfully deleted calendar color with ID: $id")
            ok(createSuccessResponse("Calendar color successfully deleted", mapOf("calendarColorId" to id)))
        } else {
            logger.warn("Calendar color with ID: $id not found for deletion")
            throw ResourceNotFoundException("Calendar color", id)
        }
    }

    @GetMapping("/check-name")
    @Operation(summary = "Check if color name is taken", description = "Checks if a color name is already in use")
    fun isColorNameTaken(
        @Parameter(description = "Color name to check", required = true) @RequestParam name: String,
        @Parameter(description = "ID to exclude from check") @RequestParam(required = false) excludeId: String?
    ): ResponseEntity<Map<String, Boolean>> {
        logger.info("Checking if color name '$name' is taken")

        val isTaken = calendarColorFacade.isColorNameTaken(name, excludeId)
        return ok(mapOf("exists" to isTaken))
    }

    private fun validateHexColor(color: String) {
        val hexColorRegex = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
        if (!color.matches(hexColorRegex)) {
            throw IllegalArgumentException("Invalid color format. Color must be a valid HEX color (e.g., #3498db)")
        }
    }
}