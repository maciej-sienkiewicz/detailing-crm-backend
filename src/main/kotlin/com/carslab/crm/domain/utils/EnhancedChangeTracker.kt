package com.carslab.crm.domain.utils

import com.carslab.crm.domain.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Rozbudowane rozwiązanie problemu śledzenia zmian z obsługą zagnieżdżonych obiektów
 */

/**
 * Rozszerzony ChangeTracker z obsługą zagnieżdżonych pól
 */
object EnhancedChangeTracker {

    /**
     * Porównuje dwa obiekty tego samego typu i wykrywa różnice w ich polach,
     * włącznie z polami zagnieżdżonymi.
     */
    inline fun <reified T : Any> trackChanges(
        oldObject: T,
        newObject: T,
        entityName: String = T::class.simpleName ?: "Unknown",
        noinline idProperty: (T) -> Any,
        ignoredFields: Set<String> = emptySet(),
        trackNestedChanges: Boolean = true
    ): ChangeRecord {
        val changes = mutableListOf<FieldChange>()
        val entityId = idProperty(newObject)

        // Porównaj wszystkie właściwości obiektów
        T::class.memberProperties.filterIsInstance<KProperty1<T, Any?>>().forEach { property ->
            val propertyName = property.name

            // Pomiń ignorowane pola
            if (propertyName in ignoredFields || ChangeTracker.isNestedIgnoredField(propertyName, ignoredFields)) {
                return@forEach
            }

            val oldValue = property.get(oldObject)
            val newValue = property.get(newObject)

            // Dodaj zmianę tylko jeśli wartości są różne
            if (!ChangeTracker.areEqual(oldValue, newValue)) {
                changes.add(FieldChange(propertyName, oldValue, newValue))

                // Jeśli trackNestedChanges=true, sprawdź czy to obiekt, który można rozbić na części
                if (trackNestedChanges && oldValue != null && newValue != null
                    && oldValue::class == newValue::class
                    && oldValue !is String && oldValue !is Number && oldValue !is Boolean
                    && oldValue !is Collection<*> && oldValue !is Map<*, *>
                ) {
                    val nestedChanges = trackNestedObjectChanges(propertyName, oldValue, newValue, ignoredFields)
                    changes.addAll(nestedChanges)
                }
            }
        }

        return ChangeRecord(entityName, entityId, changes)
    }

    /**
     * Śledzi zmiany w zagnieżdżonych obiektach
     */
    fun trackNestedObjectChanges(
        parentFieldName: String,
        oldValue: Any,
        newValue: Any,
        ignoredFields: Set<String> = emptySet()
    ): List<FieldChange> {
        val changes = mutableListOf<FieldChange>()

        oldValue::class.memberProperties.filterIsInstance<KProperty1<Any, Any?>>().forEach { property ->
            val propertyName = property.name
            val nestedFieldName = "$parentFieldName.$propertyName"

            // Pomiń ignorowane pola
            if (nestedFieldName in ignoredFields || ChangeTracker.isNestedIgnoredField(nestedFieldName, ignoredFields)) {
                return@forEach
            }

            try {
                val oldNestedValue = property.get(oldValue)
                val newNestedValue = property.get(newValue)

                if (!ChangeTracker.areEqual(oldNestedValue, newNestedValue)) {
                    changes.add(FieldChange(nestedFieldName, oldNestedValue, newNestedValue))
                }
            } catch (e: Exception) {
                // Ignoruj błędy refleksji
            }
        }

        return changes
    }
}

/**
 * Rozszerzenie dla ChangeRecord o przyjazne formatowanie zmian z obsługą zagnieżdżonych pól
 */
fun ChangeRecord.formatUserFriendlyEnhanced(entityClass: Class<*>? = null): String {
    if (changes.isEmpty()) return "Brak zmian"

    return buildString {
        appendLine("Wprowadzono zmiany:")

        // Grupujemy zmiany, żeby zagnieżdżone pola były prezentowane razem
        val processedFields = mutableSetOf<String>()

        // Najpierw przetwarzamy pola zagnieżdżone
        changes.filter { it.fieldName.contains(".") }
            .sortedBy { it.fieldName }
            .forEach { change ->
                val parentField = change.fieldName.substringBefore(".")
                if (parentField in processedFields) return@forEach

                val friendlyLabel = entityClass?.let {
                    FormatterRegistry.getFriendlyLabel(it, change.fieldName)
                } ?: change.fieldName

                appendLine("* $friendlyLabel: ${FormatterRegistry.formatValue(change.oldValue)} -> ${FormatterRegistry.formatValue(change.newValue)}")
                processedFields.add(change.fieldName)
            }

        // Potem przetwarzamy pola główne, które nie zostały jeszcze przetworzone
        changes.filter { !it.fieldName.contains(".") && it.fieldName !in processedFields }
            .forEach { change ->
                val friendlyLabel = entityClass?.let {
                    FormatterRegistry.getFriendlyLabel(it, change.fieldName)
                } ?: change.fieldName

                appendLine("* $friendlyLabel: ${FormatterRegistry.formatValue(change.oldValue)} -> ${FormatterRegistry.formatValue(change.newValue)}")
                processedFields.add(change.fieldName)
            }
    }
}

/**
 * Rozszerzenie dla formatowania obiektów typu ServicePeriod
 */
class ServicePeriodFormatter : UserFriendlyFormatter<ServicePeriod> {
    override fun formatValue(value: ServicePeriod?): String {
        if (value == null) return "brak okresu"

        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val startFormatted = value.startDate.format(dateFormatter)
        val endFormatted = value.endDate.format(dateFormatter)

        return "od $startFormatted do $endFormatted"
    }
}

/**
 * Rozszerzenie dla formatowania dat
 */
class LocalDateTimeFormatter : UserFriendlyFormatter<LocalDateTime> {
    override fun formatValue(value: LocalDateTime?): String {
        if (value == null) return "brak daty"
        return value.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    }
}

/**
 * Funkcja rozszerzająca do wygodniejszego użycia ulepszonego formatowania
 */
inline fun <reified T : Any> trackChangesAndFormat(
    oldObject: T,
    newObject: T,
    entityName: String = T::class.simpleName ?: "Unknown",
    noinline idProperty: (T) -> Any,
    ignoredFields: Set<String> = emptySet()
): String {
    // Zarejestruj formattery, jeśli jeszcze nie zostały zarejestrowane
    if (oldObject is UserFriendlyLabels) {
        FormatterRegistry.registerLabels(T::class.java, oldObject.getFriendlyLabels())
    }

    // Użyj ulepszonego śledzenia zmian z wykrywaniem zmian zagnieżdżonych
    val changes = EnhancedChangeTracker.trackChanges(
        oldObject = oldObject,
        newObject = newObject,
        entityName = entityName,
        idProperty = idProperty,
        ignoredFields = ignoredFields,
        trackNestedChanges = true
    )

    // Zwróć od razu sformatowany tekst
    return if (changes.hasChanges()) {
        changes.formatUserFriendlyEnhanced(T::class.java)
    } else {
        "Brak zmian"
    }
}