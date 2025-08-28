package com.carslab.crm.production.modules.visits.domain.service.details.media

import com.carslab.crm.production.modules.visits.domain.command.UploadMediaCommand
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitMedia
import com.carslab.crm.production.modules.visits.domain.models.enums.MediaType
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class VisitMediaFactory {

    fun createMedia(command: UploadMediaCommand, storageId: String): VisitMedia {
        return VisitMedia(
            id = storageId,
            visitId = command.visitId.value,
            name = command.metadata.name,
            description = command.metadata.description,
            location = command.metadata.location,
            tags = command.metadata.tags,
            type = MediaType.PHOTO,
            size = command.file.size,
            contentType = command.file.contentType ?: "application/octet-stream",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}