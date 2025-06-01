package com.carslab.crm.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [SafeStringValidator::class])
annotation class SafeString(
    val message: String = "Invalid characters detected",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class SafeStringValidator : ConstraintValidator<SafeString, String> {

    private val dangerousPatterns = listOf(
        "<script", "</script>", "javascript:", "onload=", "onerror=",
        "eval(", "expression(", "vbscript:", "data:text/html"
    )

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true

        val lowerValue = value.lowercase()
        return dangerousPatterns.none { pattern ->
            lowerValue.contains(pattern.lowercase())
        }
    }
}

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [LicensePlateValidator::class])
annotation class ValidLicensePlate(
    val message: String = "Invalid license plate format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class LicensePlateValidator : ConstraintValidator<ValidLicensePlate, String> {

    private val patterns = listOf(
        "^[A-Z]{2,3}\\s?[0-9]{4,5}$".toRegex(),
        "^[A-Z]{1,2}\\s?[0-9]{1,5}\\s?[A-Z]{1,3}$".toRegex()
    )

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return false

        val cleanValue = value.replace("\\s+".toRegex(), " ").trim().uppercase()
        return patterns.any { it.matches(cleanValue) }
    }
}