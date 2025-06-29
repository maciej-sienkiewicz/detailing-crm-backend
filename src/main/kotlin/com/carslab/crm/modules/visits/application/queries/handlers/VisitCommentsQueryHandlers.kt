package com.carslab.crm.modules.visits.application.queries.handlers

import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.modules.visits.domain.ports.ProtocolCommentsRepository
import com.carslab.crm.infrastructure.cqrs.QueryHandler
import com.carslab.crm.domain.model.ProtocolId
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class GetVisitCommentsQueryHandler(
    private val commentsRepository: ProtocolCommentsRepository
) : QueryHandler<GetVisitCommentsQuery, List<VisitCommentReadModel>> {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

    override fun handle(query: GetVisitCommentsQuery): List<VisitCommentReadModel> {
        val comments = commentsRepository.findById(ProtocolId(query.visitId))

        return comments.map { comment ->
            VisitCommentReadModel(
                id = comment.id.toString(),
                visitId = comment.protocolId.value,
                author = comment.author,
                content = comment.content,
                timestamp = comment.timestamp,
                type = comment.type,
                createdAt = comment.timestamp
            )
        }.sortedByDescending { it.timestamp }
    }
}

@Service
class GetCommentByIdQueryHandler(
    private val commentsRepository: ProtocolCommentsRepository
) : QueryHandler<GetCommentByIdQuery, VisitCommentReadModel?> {

    override fun handle(query: GetCommentByIdQuery): VisitCommentReadModel? {
        val comments = commentsRepository.findById(ProtocolId(query.visitId))
        val comment = comments.find { it.id.toString() == query.commentId }
            ?: return null

        return VisitCommentReadModel(
            id = comment.id.toString(),
            visitId = comment.protocolId.value,
            author = comment.author,
            content = comment.content,
            timestamp = comment.timestamp,
            type = comment.type,
            createdAt = comment.timestamp
        )
    }
}