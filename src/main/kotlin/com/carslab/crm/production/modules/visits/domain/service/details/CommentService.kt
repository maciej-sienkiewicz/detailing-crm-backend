package com.carslab.crm.production.modules.visits.domain.service.details

import com.carslab.crm.domain.model.UserId
import com.carslab.crm.production.modules.companysettings.domain.model.CompanyId
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.domain.activity.VisitActivitySender
import com.carslab.crm.production.modules.visits.domain.command.AddCommentCommand
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitComment
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitCommentRepository
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

data class AuthContext(
    val companyId: CompanyId,
    val userId: UserId,
    val userName: String,
)

@Service
class CommentService(
    private val commentRepository: VisitCommentRepository,
    private val visitDetailQueryService: VisitDetailQueryService,
    private val visitActivitySender: VisitActivitySender
) {
    fun addComment(command: AddCommentCommand, authContext: AuthContext): VisitComment {
        if (!commentRepository.existsVisitByIdAndCompanyId(command.visitId, authContext)) {
            throw EntityNotFoundException("Visit not found: ${command.visitId.value}")
        }
        
        val visit = visitDetailQueryService.getVisitDetail(command.visitId.toString(), authContext.companyId.value)

        val comment = VisitComment(
            id = null,
            visitId = command.visitId,
            author = command.author,
            content = command.content,
            type = command.type,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )

        return commentRepository.save(comment)
            .also { visitActivitySender.onCommentAdded(comment, command.visitId, visit.title, authContext) }
    }

    fun addWarning(command: AddCommentCommand, authContext: AuthContext): VisitComment {
        if (!commentRepository.existsVisitByIdAndCompanyId(command.visitId, authContext)) {
            throw EntityNotFoundException("Visit not found: ${command.visitId.value}")
        }

        val visit = visitDetailQueryService.getVisitDetail(command.visitId.toString(), authContext.companyId.value)

        val comment = VisitComment(
            id = null,
            visitId = command.visitId,
            author = command.author,
            content = command.content,
            type = command.type,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )

        return commentRepository.save(comment)
            .also { visitActivitySender.onWarningAdded(comment, command.visitId, visit.title, authContext) }
    }

    fun getCommentsForVisit(visitId: VisitId): List<VisitComment> {
        return commentRepository.findByVisitId(visitId)
            .sortedByDescending { it.createdAt }
    }

    fun deleteComment(commentId: String): Boolean {
        return commentRepository.deleteById(commentId)
    }
}