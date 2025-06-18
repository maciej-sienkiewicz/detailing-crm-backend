// src/main/kotlin/com/carslab/crm/modules/visits/domain/exceptions/ProtocolExceptions.kt
package com.carslab.crm.modules.visits.domain.exceptions

class ProtocolNotFoundException(protocolId: String) : RuntimeException("Protocol not found: $protocolId")

class ProtocolValidationException(message: String) : RuntimeException(message)

class ProtocolStatusTransitionException(from: String, to: String) :
    RuntimeException("Invalid status transition from $from to $to")