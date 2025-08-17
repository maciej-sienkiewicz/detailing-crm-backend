package com.carslab.crm.production.modules.visits.application.service

import com.carslab.crm.production.modules.visits.application.dto.VisitCommentResponse
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitCommentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitCommentQueryService(
    private val commentService: VisitCommentService
) {
    private val logger = LoggerFactory.getLogger(VisitCommentQueryService::class.java)

    fun getVisitComments(visitId: String): List<VisitCommentResponse> {
        logger.debug("Fetching comments for visit: {}", visitId)

        val comments = commentService.getCommentsForVisit(VisitId.of(visitId))
        return comments.map { VisitCommentResponse.from(it) }
    }
}