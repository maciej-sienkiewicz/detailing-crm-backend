package com.carslab.crm.production.modules.events.domain.activity

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.activities.application.dto.CreateActivityRequest
import com.carslab.crm.production.modules.activities.application.dto.RelatedEntityDto
import com.carslab.crm.production.modules.activities.application.service.ActivityCommandService
import com.carslab.crm.production.modules.activities.domain.model.ActivityCategory
import com.carslab.crm.production.modules.events.domain.models.aggregates.EventOccurrence
import com.carslab.crm.production.modules.events.domain.models.aggregates.RecurringEvent
import com.carslab.crm.production.modules.events.domain.models.enums.EventType
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.domain.models.enums.RecurrenceFrequency
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import org.springframework.stereotype.Service

@Service
class EventActivitySender(
    private val activityCommandService: ActivityCommandService,
    private val securityContext: SecurityContext
) {

    fun onRecurringEventCreated(event: RecurringEvent, occurrencesCount: Int) {
        activityCommandService.createActivity(
            CreateActivityRequest(
                category = ActivityCategory.PROTOCOL,
                message = "Utworzono nowe zdarzenie cykliczne: \"${event.title}\"",
                userId = getCurrentUserId(),
                userName = getCurrentUserName(),
                description = buildEventCreatedDescription(event, occurrencesCount),
                primaryEntity = RelatedEntityDto(
                    id = event.id?.value?.toString() ?: "",
                    type = "RECURRING_EVENT",
                    name = event.title
                ),
                relatedEntities = buildRelatedEntitiesForEvent(event),
                metadata = mapOf(
                    "eventType" to event.type.name,
                    "frequency" to event.recurrencePattern.frequency.name,
                    "interval" to event.recurrencePattern.interval.toString(),
                    "occurrencesGenerated" to occurrencesCount.toString()
                )
            )
        )
    }

    fun onRecurringEventUpdated(updatedEvent: RecurringEvent, originalEvent: RecurringEvent) {
        val changes = detectEventChanges(originalEvent, updatedEvent)

        activityCommandService.createActivity(
            CreateActivityRequest(
                category = ActivityCategory.PROTOCOL,
                message = "Zaktualizowano zdarzenie cykliczne: \"${updatedEvent.title}\"",
                userId = getCurrentUserId(),
                userName = getCurrentUserName(),
                description = "Zmiany: $changes",
                primaryEntity = RelatedEntityDto(
                    id = updatedEvent.id?.value?.toString() ?: "",
                    type = "RECURRING_EVENT",
                    name = updatedEvent.title
                ),
                relatedEntities = buildRelatedEntitiesForEvent(updatedEvent),
                metadata = mapOf(
                    "eventType" to updatedEvent.type.name,
                    "changesDetected" to (changes?.isNotEmpty()?.toString() ?: "false")
                )
            )
        )
    }

    fun onRecurringEventDeactivated(event: RecurringEvent) {
        activityCommandService.createActivity(
            CreateActivityRequest(
                category = ActivityCategory.PROTOCOL,
                message = "Dezaktywowano zdarzenie cykliczne: \"${event.title}\"",
                userId = getCurrentUserId(),
                userName = getCurrentUserName(),
                description = "Zdarzenie zostało dezaktywowane. Nie będą generowane nowe wystąpienia.",
                primaryEntity = RelatedEntityDto(
                    id = event.id?.value?.toString() ?: "",
                    type = "RECURRING_EVENT",
                    name = event.title
                ),
                relatedEntities = buildRelatedEntitiesForEvent(event),
                metadata = mapOf(
                    "eventType" to event.type.name,
                    "action" to "DEACTIVATED"
                )
            )
        )
    }

    fun onRecurringEventDeleted(event: RecurringEvent, deletedOccurrencesCount: Int) {
        activityCommandService.createActivity(
            CreateActivityRequest(
                category = ActivityCategory.PROTOCOL,
                message = "Usunięto zdarzenie cykliczne: \"${event.title}\"",
                userId = getCurrentUserId(),
                userName = getCurrentUserName(),
                description = "Usunięto zdarzenie wraz z $deletedOccurrencesCount wystąpieniami.",
                primaryEntity = RelatedEntityDto(
                    id = event.id?.value?.toString() ?: "",
                    type = "RECURRING_EVENT",
                    name = event.title
                ),
                relatedEntities = buildRelatedEntitiesForEvent(event),
                metadata = mapOf(
                    "eventType" to event.type.name,
                    "deletedOccurrences" to deletedOccurrencesCount.toString(),
                    "action" to "DELETED"
                )
            )
        )
    }

    fun onOccurrenceStatusChanged(
        occurrence: EventOccurrence,
        recurringEvent: RecurringEvent,
        previousStatus: OccurrenceStatus
    ) {
        activityCommandService.createActivity(
            CreateActivityRequest(
                category = ActivityCategory.PROTOCOL,
                message = "Zmieniono status wystąpienia zdarzenia: \"${recurringEvent.title}\"",
                userId = getCurrentUserId(),
                userName = getCurrentUserName(),
                description = "Status: '${humanFriendlyStatus(previousStatus)}' -> '${humanFriendlyStatus(occurrence.status)}'\n" +
                        "Data wystąpienia: ${occurrence.scheduledDate}",
                primaryEntity = RelatedEntityDto(
                    id = occurrence.id?.value?.toString() ?: "",
                    type = "EVENT_OCCURRENCE",
                    name = "${recurringEvent.title} - ${occurrence.scheduledDate.toLocalDate()}"
                ),
                relatedEntities = listOf(
                    RelatedEntityDto(
                        id = recurringEvent.id?.value?.toString() ?: "",
                        type = "RECURRING_EVENT",
                        name = recurringEvent.title
                    )
                ) + buildRelatedEntitiesForEvent(recurringEvent),
                metadata = mapOf(
                    "previousStatus" to previousStatus.name,
                    "newStatus" to occurrence.status.name,
                    "scheduledDate" to occurrence.scheduledDate.toString(),
                    "eventType" to recurringEvent.type.name
                )
            )
        )
    }

    fun onOccurrenceConvertedToVisit(
        occurrence: EventOccurrence,
        recurringEvent: RecurringEvent,
        visitResponse: VisitResponse
    ) {
        activityCommandService.createActivity(
            CreateActivityRequest(
                category = ActivityCategory.PROTOCOL,
                message = "Przekonwertowano wystąpienie na wizytę: \"${recurringEvent.title}\"",
                userId = getCurrentUserId(),
                userName = getCurrentUserName(),
                description = "Utworzono wizytę: \"${visitResponse.title}\"\n" +
                        "Data wystąpienia: ${occurrence.scheduledDate}\n" +
                        "ID wizyty: ${visitResponse.id}",
                primaryEntity = RelatedEntityDto(
                    id = occurrence.id?.value?.toString() ?: "",
                    type = "EVENT_OCCURRENCE",
                    name = "${recurringEvent.title} - ${occurrence.scheduledDate.toLocalDate()}"
                ),
                relatedEntities = listOf(
                    RelatedEntityDto(
                        id = recurringEvent.id?.value?.toString() ?: "",
                        type = "RECURRING_EVENT",
                        name = recurringEvent.title
                    ),
                    RelatedEntityDto(
                        id = visitResponse.id,
                        type = "VISIT",
                        name = visitResponse.title
                    ),
                ),
                metadata = mapOf(
                    "visitId" to visitResponse.id,
                    "clientId" to visitResponse.clientId,
                    "vehicleId" to visitResponse.vehicleId,
                    "eventType" to recurringEvent.type.name,
                    "action" to "CONVERTED_TO_VISIT"
                )
            )
        )
    }

    fun onOccurrencesGenerated(event: RecurringEvent, generatedCount: Int, fromDate: String, toDate: String) {
        activityCommandService.createActivity(
            CreateActivityRequest(
                category = ActivityCategory.PROTOCOL,
                message = "Wygenerowano wystąpienia dla zdarzenia: \"${event.title}\"",
                userId = getCurrentUserId(),
                userName = getCurrentUserName(),
                description = "Wygenerowano $generatedCount wystąpień w okresie: $fromDate - $toDate",
                primaryEntity = RelatedEntityDto(
                    id = event.id?.value?.toString() ?: "",
                    type = "RECURRING_EVENT",
                    name = event.title
                ),
                relatedEntities = buildRelatedEntitiesForEvent(event),
                metadata = mapOf(
                    "generatedCount" to generatedCount.toString(),
                    "fromDate" to fromDate,
                    "toDate" to toDate,
                    "eventType" to event.type.name,
                    "action" to "OCCURRENCES_GENERATED"
                )
            )
        )
    }

    fun onOccurrencesRegenerated(event: RecurringEvent, newCount: Int) {
        activityCommandService.createActivity(
            CreateActivityRequest(
                category = ActivityCategory.PROTOCOL,
                message = "Przegenerowano wystąpienia dla zdarzenia: \"${event.title}\"",
                userId = getCurrentUserId(),
                userName = getCurrentUserName(),
                description = "Usunięto poprzednie wystąpienia i wygenerowano $newCount nowych wystąpień na podstawie zaktualizowanego wzorca.",
                primaryEntity = RelatedEntityDto(
                    id = event.id?.value?.toString() ?: "",
                    type = "RECURRING_EVENT",
                    name = event.title
                ),
                relatedEntities = buildRelatedEntitiesForEvent(event),
                metadata = mapOf(
                    "regeneratedCount" to newCount.toString(),
                    "eventType" to event.type.name,
                    "action" to "OCCURRENCES_REGENERATED"
                )
            )
        )
    }

    private fun buildEventCreatedDescription(event: RecurringEvent, occurrencesCount: Int): String {
        return buildString {
            append("Typ zdarzenia: ${humanFriendlyEventType(event.type)}\n")
            append("Częstotliwość: ${humanFriendlyFrequency(event.recurrencePattern.frequency)}")
            if (event.recurrencePattern.interval > 1) {
                append(" co ${event.recurrencePattern.interval}")
            }
            append("\n")

            event.recurrencePattern.daysOfWeek?.let { days ->
                append("Dni tygodnia: ${days.joinToString(", ") { humanFriendlyDayOfWeek(it.name) }}\n")
            }

            event.recurrencePattern.dayOfMonth?.let { day ->
                append("Dzień miesiąca: $day\n")
            }

            event.recurrencePattern.endDate?.let { endDate ->
                append("Data zakończenia: $endDate\n")
            }

            event.recurrencePattern.maxOccurrences?.let { max ->
                append("Maksymalna liczba wystąpień: $max\n")
            }

            if (event.isRecurringVisit()) {
                event.visitTemplate?.let { template ->
                    append("Szablonowy czas trwania: ${template.estimatedDuration.toMinutes()} minut\n")
                    if (template.defaultServices.isNotEmpty()) {
                        append("Domyślne usługi: ${template.defaultServices.size}\n")
                    }
                }
            }

            append("Wygenerowano $occurrencesCount początkowych wystąpień")
        }
    }

    private fun buildRelatedEntitiesForEvent(event: RecurringEvent): List<RelatedEntityDto> {
        val entities = mutableListOf<RelatedEntityDto>()

        if (event.isRecurringVisit()) {
            event.visitTemplate?.let { template ->
                template.clientId?.let { clientId ->
                    entities.add(RelatedEntityDto(
                        id = clientId.toString(),
                        type = "CLIENT",
                        name = "Klient #$clientId"
                    ))
                }

                template.vehicleId?.let { vehicleId ->
                    entities.add(RelatedEntityDto(
                        id = vehicleId.toString(),
                        type = "VEHICLE",
                        name = "Pojazd #$vehicleId"
                    ))
                }
            }
        }

        return entities
    }

    private fun detectEventChanges(original: RecurringEvent, updated: RecurringEvent): String? {
        return buildString {
            if (original.title != updated.title) {
                append("Tytuł: '${original.title}' -> '${updated.title}'\n")
            }

            if (original.description != updated.description) {
                append("Opis: '${original.description ?: "brak"}' -> '${updated.description ?: "brak"}'\n")
            }

            if (original.isActive != updated.isActive) {
                append("Status: '${if (original.isActive) "Aktywne" else "Nieaktywne"}' -> '${if (updated.isActive) "Aktywne" else "Nieaktywne"}'\n")
            }

            // Zmiany w wzorcu powtarzania
            val originalPattern = original.recurrencePattern
            val updatedPattern = updated.recurrencePattern

            if (originalPattern.frequency != updatedPattern.frequency) {
                append("Częstotliwość: '${humanFriendlyFrequency(originalPattern.frequency)}' -> '${humanFriendlyFrequency(updatedPattern.frequency)}'\n")
            }

            if (originalPattern.interval != updatedPattern.interval) {
                append("Interwał: ${originalPattern.interval} -> ${updatedPattern.interval}\n")
            }

            if (originalPattern.daysOfWeek != updatedPattern.daysOfWeek) {
                val originalDays = originalPattern.daysOfWeek?.joinToString(", ") { humanFriendlyDayOfWeek(it.name) } ?: "brak"
                val updatedDays = updatedPattern.daysOfWeek?.joinToString(", ") { humanFriendlyDayOfWeek(it.name) } ?: "brak"
                append("Dni tygodnia: '$originalDays' -> '$updatedDays'\n")
            }

            if (originalPattern.dayOfMonth != updatedPattern.dayOfMonth) {
                append("Dzień miesiąca: ${originalPattern.dayOfMonth ?: "brak"} -> ${updatedPattern.dayOfMonth ?: "brak"}\n")
            }

            if (originalPattern.endDate != updatedPattern.endDate) {
                append("Data zakończenia: ${originalPattern.endDate ?: "brak"} -> ${updatedPattern.endDate ?: "brak"}\n")
            }

            if (originalPattern.maxOccurrences != updatedPattern.maxOccurrences) {
                append("Maksymalna liczba wystąpień: ${originalPattern.maxOccurrences ?: "brak"} -> ${updatedPattern.maxOccurrences ?: "brak"}\n")
            }

            // Zmiany w szablonie wizyty
            if (original.visitTemplate != updated.visitTemplate) {
                append("Szablon wizyty został zmieniony\n")
            }
        }.takeIf { it.isNotEmpty() }
    }

    private fun humanFriendlyEventType(type: EventType): String {
        return when (type) {
            EventType.SIMPLE_EVENT -> "Proste zdarzenie"
            EventType.RECURRING_VISIT -> "Cykliczna wizyta"
        }
    }

    private fun humanFriendlyFrequency(frequency: RecurrenceFrequency): String {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> "Codziennie"
            RecurrenceFrequency.WEEKLY -> "Tygodniowo"
            RecurrenceFrequency.MONTHLY -> "Miesięcznie"
            RecurrenceFrequency.YEARLY -> "Rocznie"
        }
    }

    private fun humanFriendlyStatus(status: OccurrenceStatus): String {
        return when (status) {
            OccurrenceStatus.PLANNED -> "Zaplanowane"
            OccurrenceStatus.COMPLETED -> "Zakończone"
            OccurrenceStatus.CONVERTED_TO_VISIT -> "Przekonwertowane na wizytę"
            OccurrenceStatus.SKIPPED -> "Pominięte"
            OccurrenceStatus.CANCELLED -> "Anulowane"
        }
    }

    private fun humanFriendlyDayOfWeek(dayName: String): String {
        return when (dayName.uppercase()) {
            "MONDAY" -> "Poniedziałek"
            "TUESDAY" -> "Wtorek"
            "WEDNESDAY" -> "Środa"
            "THURSDAY" -> "Czwartek"
            "FRIDAY" -> "Piątek"
            "SATURDAY" -> "Sobota"
            "SUNDAY" -> "Niedziela"
            else -> dayName
        }
    }

    private fun getCurrentUserId(): String {
        return securityContext.getCurrentUserId()
            ?: throw IllegalStateException("User not found in security context")
    }

    private fun getCurrentUserName(): String {
        return securityContext.getCurrentUserName()
            ?: throw IllegalStateException("User not found in security context")
    }
}