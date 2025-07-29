// src/main/kotlin/com/carslab/crm/modules/visits/application/commands/models/ProtocolCommands.kt
package com.carslab.crm.modules.visits.application.commands.models

import com.carslab.crm.infrastructure.cqrs.Command
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.*

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
) : Command<String>

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
) : Command<Unit>

data class ChangeProtocolStatusCommand(
    val protocolId: String,
    val newStatus: ProtocolStatus,
    val reason: String? = null
) : Command<Unit>

data class UpdateProtocolServicesCommand(
    val protocolId: String,
    val services: List<CreateServiceCommand>
) : Command<Unit>

data class DeleteProtocolCommand(
    val protocolId: String
) : Command<Unit>

data class ReleaseVehicleCommand(
    val protocolId: String,
    val paymentMethod: String,
    val documentType: String,
    val additionalNotes: String? = null,
    val overridenItems: List<OverridenInvoiceServiceItem> = emptyList(),
    val paymentDays: Long,
    ) : Command<Unit>