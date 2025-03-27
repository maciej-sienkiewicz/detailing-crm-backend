package com.carslab.crm.infrastructure.util

import com.carslab.crm.infrastructure.exception.ValidationException
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Utility class for common validations used throughout the application.
 */
object ValidationUtils {

    /**
     * Validates that a string is not blank.
     * @throws ValidationException if the value is blank
     */
    fun validateNotBlank(value: String?, fieldName: String) {
        if (value.isNullOrBlank()) {
            throw ValidationException("$fieldName cannot be empty")
        }
    }

    /**
     * Validates that a number is positive.
     * @throws ValidationException if the value is not positive
     */
    fun validatePositive(value: Number?, fieldName: String) {
        when (value) {
            null -> throw ValidationException("$fieldName cannot be null")
            is Int -> if (value <= 0) throw ValidationException("$fieldName must be positive")
            is Long -> if (value <= 0) throw ValidationException("$fieldName must be positive")
            is Double -> if (value <= 0) throw ValidationException("$fieldName must be positive")
            else -> if (value.toDouble() <= 0) throw ValidationException("$fieldName must be positive")
        }
    }

    /**
     * Validates that a number is non-negative.
     * @throws ValidationException if the value is negative
     */
    fun validateNonNegative(value: Number?, fieldName: String) {
        when (value) {
            null -> throw ValidationException("$fieldName cannot be null")
            is Int -> if (value < 0) throw ValidationException("$fieldName cannot be negative")
            is Long -> if (value < 0) throw ValidationException("$fieldName cannot be negative")
            is Double -> if (value < 0) throw ValidationException("$fieldName cannot be negative")
            else -> if (value.toDouble() < 0) throw ValidationException("$fieldName cannot be negative")
        }
    }

    /**
     * Validates that a date is not in the future.
     * @throws ValidationException if the date is in the future
     */
    fun validateNotFutureDate(date: LocalDate?, fieldName: String) {
        if (date == null) {
            throw ValidationException("$fieldName cannot be null")
        }
        if (date.isAfter(LocalDate.now())) {
            throw ValidationException("$fieldName cannot be in the future")
        }
    }

    /**
     * Validates that a datetime is not in the future.
     * @throws ValidationException if the datetime is in the future
     */
    fun validateNotFutureDateTime(dateTime: LocalDateTime?, fieldName: String) {
        if (dateTime == null) {
            throw ValidationException("$fieldName cannot be null")
        }
        if (dateTime.isAfter(LocalDateTime.now())) {
            throw ValidationException("$fieldName cannot be in the future")
        }
    }

    /**
     * Validates that an email has a valid format.
     * @throws ValidationException if the email format is invalid
     */
    fun validateEmail(email: String?, fieldName: String = "Email") {
        if (email.isNullOrBlank()) {
            return // Email is optional
        }
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$")
        if (!email.matches(emailRegex)) {
            throw ValidationException("$fieldName has invalid format")
        }
    }

    /**
     * Validates that a phone number has a valid format.
     * @throws ValidationException if the phone format is invalid
     */
    fun validatePhone(phone: String?, fieldName: String = "Phone number") {
        if (phone.isNullOrBlank()) {
            return // Phone is optional
        }
        val phoneRegex = Regex("^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,4}[-\\s.]?[0-9]{1,9}$")
        if (!phone.matches(phoneRegex)) {
            throw ValidationException("$fieldName has invalid format")
        }
    }

    /**
     * Validates that at least one of the specified values is not null or blank.
     * @throws ValidationException if all values are null or blank
     */
    fun validateAtLeastOneNotBlank(fieldNameValuePairs: Map<String, String?>) {
        if (fieldNameValuePairs.values.all { it.isNullOrBlank() }) {
            val fieldNames = fieldNameValuePairs.keys.joinToString(" or ")
            throw ValidationException("At least one of $fieldNames must be provided")
        }
    }

    /**
     * Validates that a date range is valid (start date is not after end date).
     * @throws ValidationException if the date range is invalid
     */
    fun validateDateRange(startDate: LocalDate?, endDate: LocalDate?, startFieldName: String = "Start date", endFieldName: String = "End date") {
        if (startDate == null || endDate == null) {
            throw ValidationException("$startFieldName and $endFieldName cannot be null")
        }
        if (startDate.isAfter(endDate)) {
            throw ValidationException("$startFieldName cannot be after $endFieldName")
        }
    }

    /**
     * Validates that a value is within a given range.
     * @throws ValidationException if the value is outside the range
     */
    fun validateInRange(value: Int, min: Int, max: Int, fieldName: String) {
        if (value < min || value > max) {
            throw ValidationException("$fieldName must be between $min and $max")
        }
    }

    /**
     * Validates that a value is one of the valid values.
     * @throws ValidationException if the value is not in the list of valid values
     */
    fun <T> validateOneOf(value: T, validValues: Collection<T>, fieldName: String) {
        if (value !in validValues) {
            throw ValidationException("$fieldName must be one of: ${validValues.joinToString()}")
        }
    }
}