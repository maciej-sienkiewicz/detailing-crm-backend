package com.carslab.crm.modules.visits.infrastructure.processor.exceptions

import com.carslab.crm.domain.model.ProtocolId

sealed class DomainException(message: String) : Exception(message)

class ProtocolNotFoundException(visitId: ProtocolId) :
    DomainException("Visit ${visitId.value} not found")

class InvalidRequestException(message: String) : DomainException(message)

class UnsupportedRequestFormatException(message: String) : DomainException(message)