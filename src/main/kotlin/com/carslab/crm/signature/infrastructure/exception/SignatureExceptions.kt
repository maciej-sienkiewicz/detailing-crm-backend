package com.carslab.crm.signature.infrastructure.exception

import java.util.UUID

class InvalidPairingCodeException(message: String = "Invalid or expired pairing code") :
    RuntimeException(message)

class TabletNotFoundException(deviceId: UUID) :
    RuntimeException("Tablet with ID $deviceId not found")

class WorkstationNotFoundException(workstationId: UUID) :
    RuntimeException("Workstation with ID $workstationId not found")

class SignatureSessionNotFoundException(sessionId: String) :
    RuntimeException("Signature session with ID $sessionId not found")

class TabletNotAvailableException(message: String = "No tablet available for signature request") :
    RuntimeException(message)

class UnauthorizedTabletException(message: String = "Unauthorized tablet access") :
    RuntimeException(message)