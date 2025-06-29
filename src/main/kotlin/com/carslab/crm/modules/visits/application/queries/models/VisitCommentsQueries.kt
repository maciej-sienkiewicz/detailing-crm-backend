package com.carslab.crm.modules.visits.application.queries.models

import com.carslab.crm.infrastructure.cqrs.Query

data class GetVisitCommentsQuery(
    val visitId: String
) : Query<List<VisitCommentReadModel>>

data class GetCommentByIdQuery(
    val commentId: String,
    val visitId: String,
) : Query<VisitCommentReadModel?>

data class VisitCommentReadModel(
    val id: String,
    val visitId: String,
    val author: String,
    val content: String,
    val timestamp: String,
    val type: String,
    val createdAt: String
)