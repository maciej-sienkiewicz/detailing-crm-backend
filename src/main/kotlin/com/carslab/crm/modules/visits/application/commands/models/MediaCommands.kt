package com.carslab.crm.modules.visits.application.commands.models

import com.carslab.crm.infrastructure.cqrs.Command
import com.carslab.crm.modules.visits.domain.valueobjects.MediaMetadata
import org.springframework.web.multipart.MultipartFile

data class UploadVisitMediaCommand(
    val visitId: String,
    val file: MultipartFile,
    val mediaDetails: MediaDetailsCommand
) : Command<String>

data class UpdateVisitMediaCommand(
    val visitId: String,
    val mediaId: String,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList()
) : Command<Unit>

data class DeleteVisitMediaCommand(
    val visitId: String,
    val mediaId: String
) : Command<Unit>

data class MediaDetailsCommand(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val type: String = "PHOTO"
)