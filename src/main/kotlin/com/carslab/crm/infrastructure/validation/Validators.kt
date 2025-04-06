package com.carslab.crm.infrastructure.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.lang.reflect.Field
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Validator for the AtLeastOneNotBlank annotation.
 */
class AtLeastOneNotBlankValidator : ConstraintValidator<AtLeastOneNotBlank, Any> {
    private lateinit var fieldNames: Array<String>

    override fun initialize(constraintAnnotation: AtLeastOneNotBlank) {
        fieldNames = constraintAnnotation.fieldNames
    }

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return false
        }

        // Get all fields from the object, including superclass fields
        val fields = getAllFields(value.javaClass).associateBy { it.name }

        // Check if at least one of the specified fields is not blank
        return fieldNames.any { fieldName ->
            val field = fields[fieldName] ?: return@any false
            field.isAccessible = true
            val fieldValue = field.get(value) as? String
            return !fieldValue.isNullOrBlank()
        }
    }

    private fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        var currentClass: Class<*>? = clazz

        while (currentClass != null && currentClass != Any::class.java) {
            fields.addAll(currentClass.declaredFields)
            currentClass = currentClass.superclass
        }

        return fields
    }
}

/**
 * Validator for the NotFutureDate annotation.
 */
class NotFutureDateValidator : ConstraintValidator<NotFutureDate, Any> {
    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true // Null values are handled by @NotNull
        }

        val now = LocalDate.now()

        return when (value) {
            is LocalDate -> !value.isAfter(now)
            is LocalDateTime -> !value.toLocalDate().isAfter(now)
            is Date -> !value.toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalDate().isAfter(now)
            else -> false // Unsupported type
        }
    }
}

/**
 * Validator for the ValidYear annotation.
 */
class ValidYearValidator : ConstraintValidator<ValidYear, Int> {
    override fun isValid(value: Int?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true // Null values are handled by @NotNull
        }

        val currentYear = LocalDate.now().year
        return value in 1900..(currentYear + 1) // Allow for next year's models
    }
}

/**
 * Validator for the ValidPhoneNumber annotation.
 */
class PhoneNumberValidator : ConstraintValidator<ValidPhoneNumber, String> {
    private val phoneRegex = Regex("^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,4}[-\\s.]?[0-9]{1,9}$")

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value.isNullOrBlank()) {
            return true // Blank values are handled by @NotBlank
        }

        return phoneRegex.matches(value)
    }
}

/**
 * Validator for the ValidLicensePlate annotation.
 */
class LicensePlateValidator : ConstraintValidator<ValidLicensePlate, String> {
    // This is a simplified regex that should match most license plate formats
    // In a real-world application, this might be country-specific
    private val licensePlateRegex = Regex("^[A-Z0-9 -]{2,15}$")

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value.isNullOrBlank()) {
            return true // Blank values are handled by @NotBlank
        }

        return licensePlateRegex.matches(value)
    }
}

/**
 * Validator for the ValidVin annotation.
 */
class VinValidator : ConstraintValidator<ValidVin, String> {
    // VIN is typically 17 characters for most vehicles since 1981
    private val vinRegex = Regex("^[A-HJ-NPR-Z0-9]{17}$")

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value.isNullOrBlank()) {
            return true // Blank values are handled by @NotBlank
        }

        return vinRegex.matches(value)
    }
}

/**
 * Validator for the ValidDateRange annotation.
 */
class ValidDateRangeValidator : ConstraintValidator<ValidDateRange, Any> {
    private lateinit var startDateField: String
    private lateinit var endDateField: String

    override fun initialize(constraintAnnotation: ValidDateRange) {
        startDateField = constraintAnnotation.startDateField
        endDateField = constraintAnnotation.endDateField
    }

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true
        }

        // Get all fields from the object, including superclass fields
        val fields = getAllFields(value.javaClass).associateBy { it.name }

        val startDateFieldObj = fields[startDateField] ?: return false
        val endDateFieldObj = fields[endDateField] ?: return false

        startDateFieldObj.isAccessible = true
        endDateFieldObj.isAccessible = true

        val startDate = startDateFieldObj.get(value) ?: return true // Null dates are handled by @NotNull
        val endDate = endDateFieldObj.get(value) ?: return true

        return when {
            startDate is LocalDate && endDate is LocalDate -> !startDate.isAfter(endDate)
            startDate is LocalDateTime && endDate is LocalDateTime -> !startDate.isAfter(endDate)
            startDate is Date && endDate is Date -> !startDate.after(endDate)
            else -> false // Unsupported types
        }
    }

    private fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        var currentClass: Class<*>? = clazz

        while (currentClass != null && currentClass != Any::class.java) {
            fields.addAll(currentClass.declaredFields)
            currentClass = currentClass.superclass
        }

        return fields
    }
}