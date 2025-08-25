package com.carslab.crm.production.modules.visits.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.AddCommentRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitCommentResponse
import com.carslab.crm.production.modules.visits.domain.command.AddCommentCommand
import com.carslab.crm.production.modules.visits.domain.models.enums.CommentType
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.details.VisitCommentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VisitCommentCommandService(
    private val commentService: VisitCommentService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitCommentCommandService::class.java)

    fun addComment(request: AddCommentRequest): VisitCommentResponse {
        val companyId = securityContext.getCurrentCompanyId()
        val author = securityContext.getCurrentUserName() ?: "System"

        logger.info("Adding comment to visit: {} for company: {}", request.visitId, companyId)

        val command = AddCommentCommand(
            visitId = VisitId.of(request.visitId),
            content = request.content,
            type = request.type.uppercase().let { CommentType.valueOf(it) },
            author = author
        )

        val comment = commentService.addComment(command, companyId)
        logger.info("Comment added successfully to visit: {}", request.visitId)

        return VisitCommentResponse.from(comment)
    }
}