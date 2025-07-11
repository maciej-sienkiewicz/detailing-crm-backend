package com.carslab.crm.signature.exception

import java.util.UUID

// Business Logic Exceptions
abstract class BusinessException(
    message: String,
    val errorCode: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

abstract class SecurityException(
    message: String,
    val errorCode: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

abstract class ValidationException(
    message: String,
    val errorCode: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// Specific exceptions
class InvalidPairingCodeException(message: String = "Invalid or expired pairing code") :
    BusinessException(message, "INVALID_PAIRING_CODE")

class TabletNotFoundException(deviceId: UUID) :
    BusinessException("Tablet with ID $deviceId not found", "TABLET_NOT_FOUND")

class WorkstationNotFoundException(workstationId: UUID) :
    BusinessException("Workstation with ID $workstationId not found", "WORKSTATION_NOT_FOUND")

class SignatureSessionNotFoundException(sessionId: String) :
    BusinessException("Signature session with ID $sessionId not found", "SESSION_NOT_FOUND")

class TabletNotAvailableException(message: String = "No tablet available for signature request") :
    BusinessException(message, "TABLET_NOT_AVAILABLE")

class UnauthorizedTabletException(message: String = "Unauthorized tablet access") :
    SecurityException(message, "UNAUTHORIZED_TABLET")