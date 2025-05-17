package com.carslab.crm.domain.utils
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import java.time.LocalDateTime

data class FieldChange(
    val fieldName: String,
    val oldValue: Any?,
    val newValue: Any?
) {
    override fun toString(): String {
        return "$fieldName: $oldValue -> $newValue"
    }
}

/**
 * Klasa reprezentująca zestaw zmian w obiekcie biznesowym
 *
 * @param entityName Nazwa encji/obiektu biznesowego, który został zmieniony
 * @param entityId Identyfikator zmienionego obiektu
 * @param changes Lista zmian w poszczególnych polach
 * @param timestamp Czas kiedy zmiana została wykryta
 */
data class ChangeRecord(
    val entityName: String,
    val entityId: Any,
    val changes: List<FieldChange>,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Sprawdza czy obiekt faktycznie zawierał zmiany
     */
    fun hasChanges(): Boolean = changes.isNotEmpty()

    /**
     * Formatuje zmiany do czytelnej reprezentacji tekstowej
     */
    fun formatChanges(): String {
        if (changes.isEmpty()) return "Brak zmian"

        return buildString {
            appendLine("Zmiany w $entityName (ID: $entityId) - $timestamp:")
            changes.forEach { change ->
                appendLine("  - ${change.fieldName}: ${formatValue(change.oldValue)} -> ${formatValue(change.newValue)}")
            }
        }
    }

    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> "\"$value\""
            is Collection<*> -> if (value.isEmpty()) "[]" else "[${value.size} elementów]"
            is Map<*, *> -> if (value.isEmpty()) "{}" else "{${value.size} par klucz-wartość}"
            else -> value.toString()
        }
    }
}

/**
 * Klasa narzędziowa do wykrywania zmian w obiektach biznesowych
 */
object ChangeTracker {

    /**
     * Porównuje dwa obiekty tego samego typu i wykrywa różnice w ich polach
     *
     * @param oldObject Obiekt przed zmianą
     * @param newObject Obiekt po zmianie
     * @param entityName Opcjonalna nazwa encji (domyślnie używana jest nazwa klasy)
     * @param idProperty Funkcja wyciągająca identyfikator z obiektu
     * @param ignoredFields Lista nazw pól, które mają być ignorowane podczas porównania (np. audit.updatedAt)
     * @return ChangeRecord zawierający informacje o zmianach
     */
    inline fun <reified T : Any> trackChanges(
        oldObject: T,
        newObject: T,
        entityName: String = T::class.simpleName ?: "Unknown",
        idProperty: (T) -> Any,
        ignoredFields: Set<String> = emptySet()
    ): ChangeRecord {
        val changes = mutableListOf<FieldChange>()
        val entityId = idProperty(newObject)

        // Porównaj wszystkie właściwości obiektów
        T::class.memberProperties.filterIsInstance<KProperty1<T, Any?>>().forEach { property ->
            val propertyName = property.name

            // Pomiń ignorowane pola
            if (propertyName in ignoredFields || isNestedIgnoredField(propertyName, ignoredFields)) {
                return@forEach
            }

            val oldValue = property.get(oldObject)
            val newValue = property.get(newObject)

            // Dodaj zmianę tylko jeśli wartości są różne
            if (!areEqual(oldValue, newValue)) {
                changes.add(FieldChange(propertyName, oldValue, newValue))
            }
        }

        return ChangeRecord(entityName, entityId, changes)
    }

    /**
     * Sprawdza, czy pole zagnieżdżone powinno być ignorowane
     */
    fun isNestedIgnoredField(fieldName: String, ignoredFields: Set<String>): Boolean {
        // Sprawdź, czy jakaś reguła ignorowania pasuje do pola zagnieżdżonego
        // np. "audit.updatedAt" powinno pasować do pola "audit"
        return ignoredFields.any { ignoredField ->
            fieldName.startsWith("$ignoredField.") || ignoredField.startsWith("$fieldName.")
        }
    }

    /**
     * Porównuje dwie wartości z uwzględnieniem nullowalności i specjalnych typów
     */
    fun areEqual(a: Any?, b: Any?): Boolean {
        if (a === b) return true
        if (a == null || b == null) return a == b

        // Obsługa kolekcji
        if (a is Collection<*> && b is Collection<*>) {
            if (a.size != b.size) return false
            return a.zip(b).all { (elemA, elemB) -> areEqual(elemA, elemB) }
        }

        // Obsługa map
        if (a is Map<*, *> && b is Map<*, *>) {
            if (a.size != b.size) return false
            return a.all { (key, valueA) ->
                b.containsKey(key) && areEqual(valueA, b[key])
            }
        }

        return a == b
    }
}

/**
 * Rozszerzenie dla interfejsu ChangeLog do rejestrowania zmian
 */
interface ChangeLogger {
    /**
     * Rejestruje zmianę w systemie (np. zapis do bazy danych lub loga)
     */
    fun logChange(change: ChangeRecord)
}

/**
 * Przykładowa implementacja loggera zmian
 */
class ConsoleChangeLogger : ChangeLogger {
    override fun logChange(change: ChangeRecord) {
        if (change.hasChanges()) {
            println(change.formatChanges())
        } else {
            println("Brak zmian do zapisania dla ${change.entityName} (ID: ${change.entityId})")
        }
    }
}

/**
 * Rozszerzenie dla uproszczenia kodu klienta
 */
inline fun <reified T : Any> T.compareWith(
    newObject: T,
    entityName: String = T::class.simpleName ?: "Unknown",
    noinline idProperty: (T) -> Any,
    ignoredFields: Set<String> = emptySet()
): ChangeRecord {
    return ChangeTracker.trackChanges(
        this,
        newObject,
        entityName,
        idProperty,
        ignoredFields
    )
}

/**
 * Rozszerzenie dla automatycznej aktualizacji i rejestracji zmian w jednym kroku
 */
inline fun <reified T : Any> T.updateAndTrack(
    updater: (T) -> T,
    changeLogger: ChangeLogger,
    entityName: String = T::class.simpleName ?: "Unknown",
    noinline idProperty: (T) -> Any,
    ignoredFields: Set<String> = setOf("audit", "audit.updatedAt")
): T {
    val updated = updater(this)
    val changes = compareWith(updated, entityName, idProperty, ignoredFields)
    changeLogger.logChange(changes)
    return updated
}