package com.carslab.crm.modules.visits.application.commands.models

import com.carslab.crm.domain.model.view.protocol.ProtocolDocumentType
import com.carslab.crm.infrastructure.cqrs.Command
import org.springframework.web.multipart.MultipartFile

data class UploadProtocolDocumentCommand(
    val protocolId: String,
    val file: MultipartFile,
    val documentType: ProtocolDocumentType,
    val description: String? = null
) : Command<String>

data class DeleteProtocolDocumentCommand(
    val protocolId: String,
    val documentId: String
) : Command<Unit>