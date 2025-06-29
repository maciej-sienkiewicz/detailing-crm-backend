package com.carslab.crm.modules.visits.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.infrastructure.cqrs.CommandBus
import com.carslab.crm.infrastructure.cqrs.QueryBus
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/visits/comments")
@Tag(name = "Visit Comments", description = "CQRS-based visit comment management endpoints")
class VisitCommentController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus
) : BaseController() {

    @GetMapping("/{visitId}")
    @Operation(summary = "Get visit comments", description = "Retrieves all comments for a specific visit")
    fun getVisitComments(
        @Parameter(description = "Visit ID", required = true) @PathVariable visitId: String
    ): ResponseEntity<List<CommentDto>> {
        val query = GetVisitCommentsQuery(visitId)
        val comments = queryBus.execute(query)

        val response = comments.map { comment ->
            CommentDto(
                id = comment.id,
                protocolId = comment.visitId,
                author = comment.author,
                content = comment.content,
                timestamp = comment.timestamp,
                type = comment.type
            )
        }

        return ok(response)
    }

    @PostMapping
    @Operation(summary = "Add visit comment", description = "Adds a new comment to a visit")
    fun addVisitComment(
        @RequestBody request: AddCommentRequest
    ): ResponseEntity<CommentDto> {
        validateAddCommentRequest(request)

        val command = AddVisitCommentCommand(
            visitId = request.protocolId,
            content = request.content,
            type = request.type
        )

        val commentId = commandBus.execute(command)

        val query = GetCommentByIdQuery(commentId, request.protocolId)
        val savedComment = queryBus.execute(query)
            ?: throw IllegalStateException("Failed to retrieve saved comment")

        val response = CommentDto(
            id = savedComment.id,
            protocolId = savedComment.visitId,
            author = savedComment.author,
            content = savedComment.content,
            timestamp = savedComment.timestamp,
            type = savedComment.type
        )

        return created(response)
    }
    
    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete visit comment", description = "Deletes a visit comment")
    fun deleteVisitComment(
        @Parameter(description = "Comment ID", required = true) @PathVariable commentId: String
    ): ResponseEntity<Map<String, Any>> {
        val command = DeleteVisitCommentCommand(commentId)
        commandBus.execute(command)

        return ok(createSuccessResponse("Comment deleted successfully", mapOf("commentId" to commentId)))
    }

    private fun validateAddCommentRequest(request: AddCommentRequest) {
        if (request.protocolId.isBlank()) {
            throw ValidationException("Protocol ID cannot be blank")
        }
        if (request.content.isBlank()) {
            throw ValidationException("Comment content cannot be blank")
        }
        if (request.content.length > 2000) {
            throw ValidationException("Comment content cannot exceed 2000 characters")
        }
        if (request.type !in listOf("internal", "customer", "system")) {
            throw ValidationException("Invalid comment type. Must be one of: internal, customer, system")
        }
    }

    private fun validateUpdateCommentRequest(request: UpdateCommentRequest) {
        if (request.content.isBlank()) {
            throw ValidationException("Comment content cannot be blank")
        }
        if (request.content.length > 2000) {
            throw ValidationException("Comment content cannot exceed 2000 characters")
        }
    }
}

data class CommentDto(
    val id: String? = null,
    val protocolId: String = "",
    val author: String = "",
    val content: String = "",
    val timestamp: String? = null,
    val type: String = ""
)

data class AddCommentRequest(
    val protocolId: String,
    val content: String,
    val type: String = "internal"
)

data class UpdateCommentRequest(
    val content: String
)