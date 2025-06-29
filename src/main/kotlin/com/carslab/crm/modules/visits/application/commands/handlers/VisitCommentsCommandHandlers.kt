package com.carslab.crm.modules.visits.application.commands.handlers

import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.domain.ports.ProtocolCommentsRepository
import com.carslab.crm.modules.visits.domain.ports.ProtocolRepository
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import com.carslab.crm.infrastructure.events.EventPublisher
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolComment
import com.carslab.crm.modules.visits.domain.events.VisitCommentAddedEvent
import com.carslab.crm.modules.visits.domain.events.VisitCommentUpdatedEvent
import com.carslab.crm.modules.visits.domain.events.VisitCommentDeletedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AddVisitCommentCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val commentsRepository: ProtocolCommentsRepository,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<AddVisitCommentCommand, String> {

    private val logger = LoggerFactory.getLogger(AddVisitCommentCommandHandler::class.java)

    @Transactional
    override fun handle(command: AddVisitCommentCommand): String {
        logger.info("Adding comment to visit: ${command.visitId}")

        val protocol = protocolRepository.findById(ProtocolId(command.visitId))
            ?: throw ResourceNotFoundException("Visit", command.visitId)

        val author = securityContext.getCurrentUserName() ?: "System"
        val timestamp = Instant.now().toString()

        val comment = ProtocolComment(
            protocolId = ProtocolId(command.visitId),
            author = author,
            content = command.content,
            timestamp = timestamp,
            type = command.type
        )

        val savedComment = commentsRepository.save(comment)

        eventPublisher.publish(
            VisitCommentAddedEvent(
                visitId = command.visitId,
                commentId = savedComment.id.toString(),
                author = author,
                content = command.content,
                type = command.type,
                protocolTitle = protocol.title,
                companyId = securityContext.getCurrentCompanyId(),
                userId = securityContext.getCurrentUserId(),
                userName = author
            )
        )

        logger.info("Successfully added comment to visit: ${command.visitId}")
        return savedComment.id.toString()
    }
}

@Service
class UpdateVisitCommentCommandHandler(
    private val commentsRepository: ProtocolCommentsRepository,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<UpdateVisitCommentCommand, Unit> {

    private val logger = LoggerFactory.getLogger(UpdateVisitCommentCommandHandler::class.java)

    @Transactional
    override fun handle(command: UpdateVisitCommentCommand) {
        logger.info("Updating comment: ${command.commentId}")

        val existingComments = commentsRepository.findById(ProtocolId(command.commentId))
        val existingComment = existingComments.find { it.id.toString() == command.commentId }
            ?: throw ResourceNotFoundException("Comment", command.commentId)

        val updatedComment = existingComment.copy(
            content = command.content,
            timestamp = Instant.now().toString()
        )

        commentsRepository.save(updatedComment)

        eventPublisher.publish(
            VisitCommentUpdatedEvent(
                visitId = existingComment.protocolId.value,
                commentId = command.commentId,
                newContent = command.content,
                companyId = securityContext.getCurrentCompanyId(),
                userId = securityContext.getCurrentUserId(),
                userName = securityContext.getCurrentUserName()
            )
        )

        logger.info("Successfully updated comment: ${command.commentId}")
    }
}

@Service
class DeleteVisitCommentCommandHandler(
    private val commentsRepository: ProtocolCommentsRepository,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext
) : CommandHandler<DeleteVisitCommentCommand, Unit> {

    private val logger = LoggerFactory.getLogger(DeleteVisitCommentCommandHandler::class.java)

    @Transactional
    override fun handle(command: DeleteVisitCommentCommand) {
        logger.info("Deleting comment: ${command.commentId}")

        eventPublisher.publish(
            VisitCommentDeletedEvent(
                commentId = command.commentId,
                companyId = securityContext.getCurrentCompanyId(),
                userId = securityContext.getCurrentUserId(),
                userName = securityContext.getCurrentUserName()
            )
        )

        logger.info("Successfully deleted comment: ${command.commentId}")
    }
}