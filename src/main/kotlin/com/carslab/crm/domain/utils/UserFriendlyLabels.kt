package com.carslab.crm.domain.utils

/**
 * Interfejs dla obiektów, które potrafią dostarczyć przyjazne nazwy pól
 */
interface UserFriendlyLabels {
    /**
     * Zwraca mapę z przyjazną dla użytkownika nazwą dla danego pola
     */
    fun getFriendlyLabels(): Map<String, String>

    /**
     * Zwraca przyjazną nazwę dla pola lub samą nazwę pola, jeśli nie znaleziono mapowania
     */
    fun getFriendlyLabel(fieldName: String): String {
        return getFriendlyLabels()[fieldName] ?: fieldName
    }
}

/**
 * Interfejs dla obiektów, które potrafią formatować swoje wartości w sposób przyjazny dla użytkownika
 */
interface UserFriendlyFormatter<T> {
    /**
     * Formatuje wartość typu T do przyjaznej dla użytkownika reprezentacji
     */
    fun formatValue(value: T?): String
}

/**
 * Rejestr formatowań dla poszczególnych typów
 */
object FormatterRegistry {
    private val formatters = mutableMapOf<Class<*>, UserFriendlyFormatter<*>>()
    private val entityLabels = mutableMapOf<Class<*>, Map<String, String>>()

    /**
     * Rejestruje formatter dla danego typu
     */
    fun <T : Any> registerFormatter(clazz: Class<T>, formatter: UserFriendlyFormatter<T>) {
        formatters[clazz] = formatter
    }

    /**
     * Rejestruje przyjazne nazwy pól dla danej klasy
     */
    fun registerLabels(clazz: Class<*>, labels: Map<String, String>) {
        entityLabels[clazz] = labels
    }

    /**
     * Pobiera przyjazną nazwę pola dla danej klasy i pola
     */
    fun getFriendlyLabel(clazz: Class<*>, fieldName: String): String {
        return entityLabels[clazz]?.get(fieldName) ?: fieldName
    }

    /**
     * Formatuje wartość używając zarejestrowanego formattera lub domyślnego formatowania
     */
    @Suppress("UNCHECKED_CAST")
    fun formatValue(value: Any?): String {
        if (value == null) return "brak wartości"

        val formatter = formatters[value::class.java] as? UserFriendlyFormatter<Any>
        return formatter?.formatValue(value) ?: DefaultFormatter.formatValue(value)
    }
}

/**
 * Domyślny formatter dla standardowych typów
 */
object DefaultFormatter : UserFriendlyFormatter<Any> {
    override fun formatValue(value: Any?): String {
        return when (value) {
            null -> "brak wartości"
            is String -> value
            is Number -> value.toString()
            is Boolean -> if (value) "Tak" else "Nie"
            is Collection<*> -> if (value.isEmpty()) "pusta lista" else "${value.size} elementów"
            is Map<*, *> -> if (value.isEmpty()) "pusty słownik" else "${value.size} par klucz-wartość"
            else -> value.toString()
        }
    }
}

/**
 * Rozszerzenie dla ChangeRecord o przyjazne formatowanie zmian
 */
fun ChangeRecord.formatUserFriendly(entityClass: Class<*>? = null): String {
    if (changes.isEmpty()) return "Brak zmian"

    return buildString {
        appendLine("Wprowadzono zmiany:")
        changes.forEach { change ->
            val friendlyLabel = entityClass?.let {
                FormatterRegistry.getFriendlyLabel(it, change.fieldName)
            } ?: change.fieldName

            appendLine("* $friendlyLabel: ${FormatterRegistry.formatValue(change.oldValue)} -> ${FormatterRegistry.formatValue(change.newValue)}")
        }
    }
}

/**
 * Rozszerzenie klasy ChangeTracker o przyjazne formatowanie
 */
inline fun <reified T : Any> ChangeTracker.trackChangesWithFormatting(
    oldObject: T,
    newObject: T,
    entityName: String = T::class.simpleName ?: "Unknown",
    noinline idProperty: (T) -> Any,
    ignoredFields: Set<String> = emptySet()
): ChangeRecord {
    val changes = trackChanges(oldObject, newObject, entityName, idProperty, ignoredFields)

    // Jeśli obiekt implementuje UserFriendlyLabels, zarejestruj jego etykiety
    if (oldObject is UserFriendlyLabels) {
        FormatterRegistry.registerLabels(T::class.java, oldObject.getFriendlyLabels())
    }

    return changes
}