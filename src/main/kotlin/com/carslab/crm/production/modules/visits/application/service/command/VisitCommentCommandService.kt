package com.carslab.crm.production.modules.visits.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.AddCommentRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitCommentResponse
import com.carslab.crm.production.modules.visits.domain.command.AddCommentCommand
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitCommentService
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

    fun addComment(visitId: String, request: AddCommentRequest): VisitCommentResponse {
        val companyId = securityContext.getCurrentCompanyId()
        val author = securityContext.getCurrentUserName() ?: "System"

        logger.info("Adding comment to visit: {} for company: {}", visitId, companyId)

        val command = AddCommentCommand(
            visitId = VisitId.of(visitId),
            content = request.content,
            type = request.type,
            author = author
        )

        val comment = commentService.addComment(command, companyId)
        logger.info("Comment added successfully to visit: {}", visitId)

        return VisitCommentResponse.from(comment)
    }
}