// src/main/kotlin/com/carslab/crm/production/modules/events/presentation/RecurringEventController.kt
package com.carslab.crm.production.modules.events.presentation

import com.carslab.crm.production.modules.events.application.dto.*
import com.carslab.crm.production.modules.events.application.service.command.EventOccurrenceCommandService
import com.carslab.crm.production.modules.events.application.service.command.RecurringEventCommandService
import com.carslab.crm.production.modules.events.application.service.query.EventCalendarQueryService
import com.carslab.crm.production.modules.events.application.service.query.EventOccurrenceQueryService
import com.carslab.crm.production.modules.events.application.service.query.RecurringEventQueryService
import com.carslab.crm.production.modules.events.domain.models.value_objects.EventOccurrenceId
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.events.domain.services.EventVisitIntegrationService
import com.carslab.crm.production.shared.presentation.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/recurring-events")
@Tag(name = "Recurring Events", description = "Recurring events management endpoints")
class RecurringEventController(
    private val recurringEventCommandService: RecurringEventCommandService,
    private val recurringEventQueryService: RecurringEventQueryService,
    private val eventOccurrenceCommandService: EventOccurrenceCommandService,
    private val eventOccurrenceQueryService: EventOccurrenceQueryService,
    private val eventCalendarQueryService: EventCalendarQueryService,
    private val eventVisitIntegrationService: EventVisitIntegrationService
) : BaseController() {

    @PostMapping
    @Operation(summary = "Create a recurring event", description = "Creates a new recurring event")
    fun createRecurringEvent(
        @Valid @RequestBody request: CreateRecurringEventRequest
    ): ResponseEntity<RecurringEventResponse> {
        logger.info("Creating recurring event: {}", request.title)

        val response = recurringEventCommandService.createRecurringEvent(request)
        logger.info("Recurring event created successfully: {}", response.id)

        return created(response)
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get recurring event", description = "Retrieves a specific recurring event")
    fun getRecurringEvent(
        @Parameter(description = "Event ID", required = true) @PathVariable eventId: Long
    ): ResponseEntity<RecurringEventResponse> {
        logger.info("Getting recurring event: {}", eventId)

        val response = recurringEventQueryService.getRecurringEvent(eventId)
        return ok(response)
    }

    @PutMapping("/{eventId}")
    @Operation(summary = "Update recurring event", description = "Updates an existing recurring event")
    fun updateRecurringEvent(
        @Parameter(description = "Event ID", required = true) @PathVariable eventId: Long,
        @Valid @RequestBody request: UpdateRecurringEventRequest
    ): ResponseEntity<RecurringEventResponse> {
        logger.info("Updating recurring event: {}", eventId)

        val response = recurringEventCommandService.updateRecurringEvent(eventId, request)
        logger.info("Recurring event updated successfully: {}", eventId)

        return ok(response)
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "Delete recurring event", description = "Deletes a recurring event and all its occurrences")
    fun deleteRecurringEvent(
        @Parameter(description = "Event ID", required = true) @PathVariable eventId: Long
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting recurring event: {}", eventId)

        val deleted = recurringEventCommandService.deleteRecurringEvent(RecurringEventId.of(eventId))
        val message = if (deleted) {
            "Recurring event deleted successfully"
        } else {
            "Recurring event not found"
        }

        return ok(mapOf("message" to message, "deleted" to deleted))
    }

    @PatchMapping("/{eventId}/deactivate")
    @Operation(summary = "Deactivate recurring event", description = "Deactivates a recurring event")
    fun deactivateRecurringEvent(
        @Parameter(description = "Event ID", required = true) @PathVariable eventId: Long
    ): ResponseEntity<RecurringEventResponse> {
        logger.info("Deactivating recurring event: {}", eventId)

        val response = recurringEventCommandService.deactivateRecurringEvent(eventId)
        return ok(response)
    }

    @GetMapping
    @Operation(summary = "Get recurring events", description = "Retrieves all recurring events with pagination")
    fun getRecurringEvents(
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "updatedAt") sortBy: String,
        @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") sortDirection: String,
        @Parameter(description = "Event type filter") @RequestParam(required = false) type: String?,
        @Parameter(description = "Active events only") @RequestParam(defaultValue = "false") activeOnly: Boolean
    ): ResponseEntity<Page<RecurringEventResponse>> {
        logger.info("Getting recurring events - page: {}, size: {}", page, size)

        val sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val events = when {
            activeOnly -> recurringEventQueryService.getActiveRecurringEvents(pageable)
            type != null -> recurringEventQueryService.getRecurringEventsByType(type, pageable)
            else -> recurringEventQueryService.getRecurringEvents(pageable)
        }

        return ok(events)
    }

    @GetMapping("/{eventId}/occurrences")
    @Operation(summary = "Get event occurrences", description = "Retrieves occurrences for a specific recurring event")
    fun getEventOccurrences(
        @Parameter(description = "Event ID", required = true) @PathVariable eventId: Long,
        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<EventOccurrenceResponse>> {
        logger.info("Getting occurrences for event: {} from {} to {}", eventId, startDate, endDate)

        val occurrences = eventOccurrenceQueryService.getOccurrences(eventId, startDate, endDate)
        return ok(occurrences)
    }

    @GetMapping("/{eventId}/occurrences/all")
    @Operation(summary = "Get all event occurrences", description = "Retrieves all occurrences for a specific recurring event with pagination")
    fun getAllEventOccurrences(
        @Parameter(description = "Event ID", required = true) @PathVariable eventId: Long,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<Page<EventOccurrenceResponse>> {
        logger.info("Getting all occurrences for event: {}", eventId)

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "scheduledDate"))
        val occurrences = eventOccurrenceQueryService.getOccurrencesByEvent(eventId, pageable)
        return ok(occurrences)
    }

    @PostMapping("/{eventId}/occurrences/{occurrenceId}/convert-to-visit")
    @Operation(summary = "Convert occurrence to visit", description = "Converts an event occurrence to a full visit")
    fun convertToVisit(
        @Parameter(description = "Event ID", required = true) @PathVariable eventId: Long,
        @Parameter(description = "Occurrence ID", required = true) @PathVariable occurrenceId: Long,
        @Valid @RequestBody request: ConvertToVisitRequest
    ): ResponseEntity<com.carslab.crm.production.modules.visits.application.dto.VisitResponse> {
        logger.info("Converting occurrence {} to visit for event: {}", occurrenceId, eventId)

        val visit = eventVisitIntegrationService.convertOccurrenceToVisit(
            EventOccurrenceId.of(occurrenceId),
            request
        )
        return created(visit)
    }

    @PatchMapping("/{eventId}/occurrences/{occurrenceId}/status")
    @Operation(summary = "Update occurrence status", description = "Updates the status of an event occurrence")
    fun updateOccurrenceStatus(
        @Parameter(description = "Event ID", required = true) @PathVariable eventId: Long,
        @Parameter(description = "Occurrence ID", required = true) @PathVariable occurrenceId: Long,
        @Valid @RequestBody request: UpdateOccurrenceStatusRequest
    ): ResponseEntity<EventOccurrenceResponse> {
        logger.info("Updating occurrence {} status to {} for event: {}", occurrenceId, request.status, eventId)

        val occurrence = eventOccurrenceCommandService.updateOccurrenceStatus(occurrenceId, request)
        return ok(occurrence)
    }

    @GetMapping("/calendar")
    @Operation(summary = "Get event calendar", description = "Retrieves all event occurrences for calendar view")
    fun getEventCalendar(
        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start_date: LocalDate,
        @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end_date: LocalDate
    ): ResponseEntity<List<EventOccurrenceResponse>> {
        logger.info("Getting event calendar from {} to {}", start_date, end_date)

        val calendar = eventCalendarQueryService.getEventCalendar(start_date, end_date)
        return ok(calendar)
    }

    // NEW ENDPOINT: Calendar with detailed event information (solves N+1 query problem)
    @GetMapping("/calendar/detailed")
    @Operation(
        summary = "Get event calendar with full event details",
        description = "Retrieves all event occurrences with embedded recurring event details for calendar view. " +
                "This endpoint is optimized to avoid N+1 queries by fetching all required data in a single request."
    )
    fun getEventCalendarWithDetails(
        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start_date: LocalDate,
        @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end_date: LocalDate,
        @Parameter(description = "Include event details in response") @RequestParam(defaultValue = "true") includeEventDetails: Boolean = true
    ): ResponseEntity<List<EventOccurrenceWithDetailsResponse>> {
        logger.info("Getting detailed event calendar from {} to {} (includeDetails: {})", start_date, end_date, includeEventDetails)

        val calendarWithDetails = eventCalendarQueryService.getEventCalendarWithDetails(start_date, end_date, includeEventDetails)

        logger.info("Retrieved {} occurrences with details for calendar", calendarWithDetails.size)
        return ok(calendarWithDetails)
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming events", description = "Retrieves upcoming event occurrences")
    fun getUpcomingEvents(
        @Parameter(description = "Number of days ahead") @RequestParam(defaultValue = "7") days: Int
    ): ResponseEntity<List<EventOccurrenceResponse>> {
        logger.info("Getting upcoming events for next {} days", days)

        val upcoming = eventCalendarQueryService.getUpcomingEvents(days)
        return ok(upcoming)
    }

    @GetMapping("/{eventId}/statistics")
    @Operation(summary = "Get occurrence statistics", description = "Retrieves statistics for event occurrences")
    fun getOccurrenceStatistics(
        @Parameter(description = "Event ID", required = true) @PathVariable eventId: Long
    ): ResponseEntity<Map<String, Long>> {
        logger.info("Getting statistics for event: {}", eventId)

        val statistics = eventOccurrenceQueryService.getOccurrenceStatistics(eventId)
        return ok(statistics)
    }

    @GetMapping("/count")
    @Operation(summary = "Get events count", description = "Retrieves count of recurring events")
    fun getRecurringEventsCount(
        @Parameter(description = "Event type filter") @RequestParam(required = false) type: String?
    ): ResponseEntity<Map<String, Long>> {
        val count = if (type != null) {
            mapOf("count" to recurringEventQueryService.getRecurringEventsCountByType(type))
        } else {
            mapOf("count" to recurringEventQueryService.getRecurringEventsCount())
        }
        return ok(count)
    }
}