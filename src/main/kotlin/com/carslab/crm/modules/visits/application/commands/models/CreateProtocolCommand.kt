package com.carslab.crm.modules.visits.application.commands.models

import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.*
import java.time.LocalDateTime

data class CreateProtocolCommand(
    val title: String,
    val calendarColorId: String,
    val vehicle: CreateVehicleCommand,
    val client: CreateClientCommand,
    val period: CreatePeriodCommand,
    val status: ProtocolStatus = ProtocolStatus.SCHEDULED,
    val services: List<CreateServiceCommand> = emptyList(),
    val notes: String? = null,
    val referralSource: String? = null,
    val otherSourceDetails: String? = null,
    val documents: CreateDocumentsCommand = CreateDocumentsCommand(),
    val mediaItems: List<CreateMediaCommand> = emptyList(),
    val appointmentId: String? = null
)

data class UpdateProtocolCommand(
    val protocolId: String,
    val title: String,
    val calendarColorId: String,
    val vehicle: UpdateVehicleCommand,
    val client: UpdateClientCommand,
    val period: UpdatePeriodCommand,
    val status: ProtocolStatus,
    val services: List<UpdateServiceCommand> = emptyList(),
    val notes: String? = null,
    val referralSource: String? = null,
    val otherSourceDetails: String? = null,
    val documents: UpdateDocumentsCommand,
    val appointmentId: String? = null
)

data class ChangeProtocolStatusCommand(
    val protocolId: String,
    val newStatus: ProtocolStatus,
    val reason: String? = null
)

data class UpdateProtocolServicesCommand(
    val protocolId: String,
    val services: List<CreateServiceCommand>
)

data class ReleaseVehicleCommand(
    val protocolId: String,
    val paymentMethod: String,
    val documentType: String,
    val additionalNotes: String? = null
)