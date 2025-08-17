package com.carslab.crm.production.shared.exception

class BusinessException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class CompanyNotFoundException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class InvoiceNotFoundException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class ServiceNotFoundException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class UserNotFoundException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class EntityNotFoundException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
