package com.carslab.crm.production.shared.exception

class BusinessException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)