package com.carslab.crm.production.modules.visits.domain.repositories

import com.carslab.crm.production.modules.visits.domain.models.entities.VisitComment
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId

interface VisitCommentRepository {
    fun save(comment: VisitComment): VisitComment
    fun findByVisitId(visitId: VisitId): List<VisitComment>
    fun deleteById(commentId: String): Boolean
}