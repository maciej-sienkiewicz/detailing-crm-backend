package com.carslab.crm.modules.visits.application.commands.models

import com.carslab.crm.infrastructure.cqrs.Command

data class AddVisitCommentCommand(
    val visitId: String,
    val content: String,
    val type: String = "internal"
) : Command<String>

data class UpdateVisitCommentCommand(
    val commentId: String,
    val content: String
) : Command<Unit>

data class DeleteVisitCommentCommand(
    val commentId: String
) : Command<Unit>