package com.carslab.crm.production.modules.visits.domain.model

import java.time.LocalDateTime

data class VisitComment(
    val id: String?,
    val visitId: VisitId,
    val author: String,
    val content: String,
    val type: CommentType,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
) {
    init {
        require(content.isNotBlank()) { "Comment content cannot be blank" }
        require(content.length <= 2000) { "Comment content cannot exceed 2000 characters" }
        require(author.isNotBlank()) { "Comment author cannot be blank" }
    }

    fun isInternal(): Boolean = type == CommentType.INTERNAL
    fun isCustomer(): Boolean = type == CommentType.CUSTOMER
    fun isSystem(): Boolean = type == CommentType.SYSTEM
}

enum class CommentType {
    INTERNAL,
    CUSTOMER,
    SYSTEM
}