package com.carslab.crm.production.modules.visits.api.details

import com.carslab.crm.production.modules.visits.application.dto.AddCommentRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitCommentResponse
import com.carslab.crm.production.modules.visits.application.service.command.VisitCommentCommandService
import com.carslab.crm.production.modules.visits.application.service.query.VisitCommentQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/visits/comments")
@Tag(name = "Visit Comments", description = "Comment operations for visits")
class VisitCommentController(
    private val visitCommentCommandService: VisitCommentCommandService,
    private val visitCommentQueryService: VisitCommentQueryService
) {

    @PostMapping
    @Operation(summary = "Add comment to visit")
    fun addComment(
        @Valid @RequestBody request: AddCommentRequest
    ): ResponseEntity<VisitCommentResponse> {
        val comment = visitCommentCommandService.addComment(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(comment)
    }

    @GetMapping("/{visitId}")
    @Operation(summary = "Get visit comments")
    fun getVisitComments(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<List<VisitCommentResponse>> {
        val comments = visitCommentQueryService.getVisitComments(visitId)
        return ResponseEntity.ok(comments)
    }
}