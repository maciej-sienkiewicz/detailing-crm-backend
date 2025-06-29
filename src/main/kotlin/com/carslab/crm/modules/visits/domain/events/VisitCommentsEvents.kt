package com.carslab.crm.modules.visits.domain.events

import com.carslab.crm.infrastructure.events.BaseDomainEvent

data class VisitCommentAddedEvent(
    val visitId: String,
    val commentId: String,
    val author: String,
    val content: String,
    val type: String,
    val protocolTitle: String? = null,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = visitId,
    aggregateType = "VISIT",
    eventType = "VISIT_COMMENT_ADDED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "commentId" to commentId,
        "author" to author,
        "content" to content,
        "type" to type,
        "protocolTitle" to protocolTitle
    ) + additionalMetadata
)

data class VisitCommentUpdatedEvent(
    val visitId: String,
    val commentId: String,
    val newContent: String,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = visitId,
    aggregateType = "VISIT",
    eventType = "VISIT_COMMENT_UPDATED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "commentId" to commentId,
        "newContent" to newContent
    ) + additionalMetadata
)

data class VisitCommentDeletedEvent(
    val commentId: String,
    override val companyId: Long,
    override val userId: String? = null,
    override val userName: String? = null,
    private val additionalMetadata: Map<String, Any> = emptyMap()
) : BaseDomainEvent(
    aggregateId = commentId,
    aggregateType = "COMMENT",
    eventType = "VISIT_COMMENT_DELETED",
    companyId = companyId,
    userId = userId,
    userName = userName,
    metadata = mapOf(
        "commentId" to commentId
    ) + additionalMetadata
)