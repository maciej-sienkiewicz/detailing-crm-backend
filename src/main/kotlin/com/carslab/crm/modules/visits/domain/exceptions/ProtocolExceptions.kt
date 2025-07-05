// src/main/kotlin/com/carslab/crm/modules/visits/domain/exceptions/ProtocolExceptions.kt
package com.carslab.crm.modules.visits.domain.exceptions

import com.carslab.crm.domain.model.ProtocolId

class ProtocolNotFoundException(protocolId: ProtocolId) : RuntimeException("Protocol not found: ${protocolId.value}")

class ProtocolValidationException(message: String) : RuntimeException(message)

class ProtocolStatusTransitionException(from: String, to: String) :
    RuntimeException("Invalid status transition from $from to $to")

class ProtocolServicesUpdateException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)