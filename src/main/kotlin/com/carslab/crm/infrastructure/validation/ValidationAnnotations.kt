package com.carslab.crm.infrastructure.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Validates that at least one of the specified fields is not empty or blank.
 * Applied at the class level.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AtLeastOneNotBlankValidator::class])
annotation class AtLeastOneNotBlank(
    val fieldNames: Array<String>,
    val message: String = "At least one of the specified fields must be provided",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Validates that a date is not in the future.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [NotFutureDateValidator::class])
annotation class NotFutureDate(
    val message: String = "Date cannot be in the future",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Validates that a production year is within a reasonable range.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidYearValidator::class])
annotation class ValidYear(
    val message: String = "Year must be between 1900 and current year + 1",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Validates that a phone number has a valid format.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PhoneNumberValidator::class])
annotation class ValidPhoneNumber(
    val message: String = "Invalid phone number format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Validates that a license plate has a valid format.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [LicensePlateValidator::class])
annotation class ValidLicensePlate(
    val message: String = "Invalid license plate format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Validates that a VIN (Vehicle Identification Number) has a valid format.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [VinValidator::class])
annotation class ValidVin(
    val message: String = "Invalid VIN format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Validates that the end date is not before the start date.
 * Applied at the class level.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidDateRangeValidator::class])
annotation class ValidDateRange(
    val startDateField: String,
    val endDateField: String,
    val message: String = "End date cannot be before start date",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)