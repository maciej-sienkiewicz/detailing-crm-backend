package com.carslab.crm.infrastructure.exception

/**
 * Exception thrown when a requested resource is not found.
 */
class ResourceNotFoundException(message: String) : RuntimeException(message) {
    constructor(resourceType: String, resourceId: Any) : this("$resourceType with ID $resourceId not found")
}

/**
 * Exception thrown when business rules are violated.
 */
class BusinessException(message: String) : RuntimeException(message)

/**
 * Exception thrown when a validation error occurs outside of the automatic validation framework.
 */
class ValidationException(message: String) : RuntimeException(message)

/**
 * Exception thrown when there is a conflict with the existing state.
 */
class ConflictException(message: String) : RuntimeException(message) {
    constructor(resourceType: String, field: String, value: Any) : this("$resourceType with $field = $value already exists")
}

/**
 * Exception thrown when a user is not authorized to perform an action.
 */
class ForbiddenException(message: String = "Access denied") : RuntimeException(message)