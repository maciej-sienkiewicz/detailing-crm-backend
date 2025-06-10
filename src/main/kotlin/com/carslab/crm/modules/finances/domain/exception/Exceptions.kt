package com.carslab.crm.modules.finances.domain.exception


class InsufficientFundsException(message: String) : RuntimeException(message)
class PermissionDeniedException(message: String) : RuntimeException(message)
class BalanceUpdateException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
