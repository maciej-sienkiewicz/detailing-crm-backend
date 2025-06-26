import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DisplayName(val value: String)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class IgnoreComparison

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomFormatter(val formatterClass: KClass<out ValueFormatter<*>>)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomMessage(val template: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MessageBinding(val bindings: Array<MessageBindingRule>)

@Target()
@Retention(AnnotationRetention.RUNTIME)
annotation class MessageBindingRule(
    val fieldName: String,
    val template: String
)

// Interfejs do formatowania wartości
interface ValueFormatter<T> {
    fun format(value: T?): String
}

// Formattery dla różnych typów danych
class DateTimeFormatter : ValueFormatter<LocalDateTime> {
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    override fun format(value: LocalDateTime?): String {
        return value?.format(formatter) ?: "brak"
    }
}

class BigDecimalFormatter : ValueFormatter<BigDecimal> {
    override fun format(value: BigDecimal?): String {
        return value?.let { "${it.setScale(2)} zł" } ?: "0,00 zł"
    }
}

class DefaultFormatter : ValueFormatter<Any> {
    override fun format(value: Any?): String {
        return value?.toString() ?: "brak"
    }
}

// Klasa reprezentująca zmianę w polu
data class FieldChange(
    val fieldPath: String,
    val displayName: String,
    val oldValue: String,
    val newValue: String,
    val changeMessage: String,
    val oldObject: Any? = null,
    val newObject: Any? = null
)

// Główna klasa do porównywania obiektów
class ObjectComparator {

    fun <T : Any> compare(oldObject: T, newObject: T): List<FieldChange> {
        if (oldObject::class != newObject::class) {
            throw IllegalArgumentException("Obiekty muszą być tego samego typu")
        }

        val changes = mutableListOf<FieldChange>()
        compareObjects(oldObject, newObject, "", changes)
        return changes
    }

    private fun compareObjects(
        oldObj: Any?,
        newObj: Any?,
        parentPath: String,
        changes: MutableList<FieldChange>
    ) {
        if (oldObj == null && newObj == null) return
        if (oldObj == null || newObj == null) {
            // Obsługa przypadku gdy jeden z obiektów jest null
            return
        }

        if (oldObj::class != newObj::class) return

        val kClass = oldObj::class
        val properties = kClass.declaredMemberProperties

        for (property in properties) {
            // Sprawdź czy pole ma być ignorowane
            if (property.findAnnotation<IgnoreComparison>() != null) {
                continue
            }

            val oldValue = property.getter.call(oldObj)
            val newValue = property.getter.call(newObj)

            val currentPath = if (parentPath.isEmpty()) property.name else "$parentPath.${property.name}"

            when {
                // Porównanie prostych wartości
                isSimpleType(oldValue) || isSimpleType(newValue) -> {
                    if (oldValue != newValue) {
                        val fieldChange = createFieldChange(property, currentPath, oldValue, newValue, oldObj, newObj)
                        changes.add(fieldChange)
                    }
                }
                // Porównanie list
                oldValue is List<*> && newValue is List<*> -> {
                    compareLists(oldValue, newValue, currentPath, changes, property)
                }
                // Rekurencyjne porównanie zagnieżdżonych obiektów
                else -> {
                    compareObjects(oldValue, newValue, currentPath, changes)
                }
            }
        }
    }

    private fun compareLists(
        oldList: List<*>,
        newList: List<*>,
        path: String,
        changes: MutableList<FieldChange>,
        property: KProperty1<out Any, *>
    ) {
        // Porównanie rozmiaru listy
        if (oldList.size != newList.size) {
            val displayName = getDisplayName(property)
            val changeMessage = "Zmieniono liczbę elementów w: $displayName z wartości: ${oldList.size} na wartość: ${newList.size}"

            changes.add(
                FieldChange(
                    fieldPath = "$path.size",
                    displayName = "$displayName (liczba elementów)",
                    oldValue = oldList.size.toString(),
                    newValue = newList.size.toString(),
                    changeMessage = changeMessage
                )
            )
        }

        // Porównanie elementów listy (tylko dla prostych typów)
        val maxSize = maxOf(oldList.size, newList.size)
        for (i in 0 until maxSize) {
            val oldItem = oldList.getOrNull(i)
            val newItem = newList.getOrNull(i)

            if (oldItem != newItem && (isSimpleType(oldItem) || isSimpleType(newItem))) {
                val displayName = getDisplayName(property)
                val changeMessage = when {
                    oldItem == null -> "Dodano element do: $displayName, wartość: ${formatValue(newItem)}"
                    newItem == null -> "Usunięto element z: $displayName, wartość: ${formatValue(oldItem)}"
                    else -> "Zmieniono element w: $displayName z wartości: ${formatValue(oldItem)} na wartość: ${formatValue(newItem)}"
                }

                changes.add(
                    FieldChange(
                        fieldPath = "$path[$i]",
                        displayName = "$displayName [element $i]",
                        oldValue = formatValue(oldItem),
                        newValue = formatValue(newItem),
                        changeMessage = changeMessage,
                        oldObject = oldItem,
                        newObject = newItem
                    )
                )
            }
        }
    }

    private fun createFieldChange(
        property: KProperty1<out Any, *>,
        path: String,
        oldValue: Any?,
        newValue: Any?,
        oldObject: Any? = null,
        newObject: Any? = null
    ): FieldChange {
        val displayName = getDisplayName(property)
        val formattedOldValue = formatValue(oldValue, property)
        val formattedNewValue = formatValue(newValue, property)

        // Sprawdź czy jest zdefiniowana custom message dla tego pola
        val customMessage = property.findAnnotation<CustomMessage>()?.template
        val messageBindingRules = oldObject?.let { getMessageBindingRules(it::class, property.name) }

        val changeMessage = when {
            customMessage != null -> {
                processMessageTemplate(customMessage, oldObject, newObject, oldValue, newValue, property)
            }
            messageBindingRules != null -> {
                processMessageTemplate(messageBindingRules, oldObject, newObject, oldValue, newValue, property)
            }
            else -> {
                "Zmieniono wartość: $displayName z wartości: $formattedOldValue na wartość: $formattedNewValue"
            }
        }

        return FieldChange(
            fieldPath = path,
            displayName = displayName,
            oldValue = formattedOldValue,
            newValue = formattedNewValue,
            changeMessage = changeMessage,
            oldObject = oldObject,
            newObject = newObject
        )
    }

    private fun getMessageBindingRules(kClass: KClass<*>, fieldName: String): String? {
        return kClass.findAnnotation<MessageBinding>()?.bindings
            ?.find { it.fieldName == fieldName }?.template
    }

    private fun processMessageTemplate(
        template: String,
        oldObject: Any?,
        newObject: Any?,
        oldValue: Any?,
        newValue: Any?,
        property: KProperty1<out Any, *>
    ): String {
        var message = template

        // Standardowe zmienne
        message = message.replace("{{oldValue}}", formatValue(oldValue, property))
        message = message.replace("{{newValue}}", formatValue(newValue, property))
        message = message.replace("{{fieldName}}", getDisplayName(property))

        // Zmienne z obiektów
        if (oldObject != null) {
            message = replaceObjectBindings(message, oldObject, "old")
        }
        if (newObject != null) {
            message = replaceObjectBindings(message, newObject, "new")
        }

        return message
    }

    private fun replaceObjectBindings(message: String, obj: Any, prefix: String): String {
        var result = message
        val kClass = obj::class
        val properties = kClass.declaredMemberProperties

        for (property in properties) {
            val propertyName = property.name
            val value = try {
                property.getter.call(obj)
            } catch (e: Exception) {
                continue
            }

            // Obsługa zmiennych z prefiksem (np. {{oldName}}, {{newFinalPrice}})
            val variablePattern = "\\{\\{${prefix}${propertyName.replaceFirstChar { it.uppercase() }}\\}\\}"
            val regex = Regex(variablePattern, RegexOption.IGNORE_CASE)
            result = regex.replace(result, formatValue(value, property))

            // Obsługa zmiennych bez prefiksu ale z nazwą właściwości (np. {{name}})
            val simpleVariablePattern = "\\{\\{${propertyName}\\}\\}"
            val simpleRegex = Regex(simpleVariablePattern, RegexOption.IGNORE_CASE)
            result = simpleRegex.replace(result, formatValue(value, property))
        }

        return result
    }

    private fun getDisplayName(property: KProperty1<out Any, *>): String {
        return property.findAnnotation<DisplayName>()?.value ?: property.name.camelCaseToDisplayName()
    }
    private fun formatValue(value: Any?, property: KProperty1<out Any, *>? = null): String {
        property?.findAnnotation<CustomFormatter>()?.let { annotation ->
            try {
                val formatter = annotation.formatterClass.objectInstance
                    ?: annotation.formatterClass.java.getDeclaredConstructor().newInstance()
                @Suppress("UNCHECKED_CAST")
                return (formatter as ValueFormatter<Any>).format(value)
            } catch (e: Exception) {
                // Jeśli nie udało się użyć custom formattera, użyj domyślnego
            }
        }

        // Domyślne formatowanie
        return when (value) {
            null -> "brak"
            is LocalDateTime -> DateTimeFormatter().format(value)
            is BigDecimal -> BigDecimalFormatter().format(value)
            else -> value.toString()
        }
    }

    private fun isSimpleType(value: Any?): Boolean {
        return when (value) {
            null -> true
            is String, is Number, is Boolean, is Char -> true
            is LocalDateTime, is BigDecimal -> true
            is Enum<*> -> true
            else -> false
        }
    }
}

// Extension do konwersji camelCase na czytelny tekst
private fun String.camelCaseToDisplayName(): String {
    return this.replace(Regex("([a-z])([A-Z])")) { matchResult ->
        "${matchResult.groupValues[1]} ${matchResult.groupValues[2].lowercase()}"
    }.replaceFirstChar { it.uppercase() }
}
