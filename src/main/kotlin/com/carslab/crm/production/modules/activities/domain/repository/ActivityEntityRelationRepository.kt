package com.carslab.crm.production.modules.activities.domain.repository

import com.carslab.crm.production.modules.activities.domain.model.ActivityEntityRelation
import com.carslab.crm.production.modules.activities.domain.model.ActivityId

interface ActivityEntityRelationRepository {
    fun save(relation: ActivityEntityRelation): ActivityEntityRelation
    fun saveAll(relations: List<ActivityEntityRelation>): List<ActivityEntityRelation>
    fun findByActivityId(activityId: ActivityId): List<ActivityEntityRelation>
    fun findByActivityIds(activityIds: List<ActivityId>): List<ActivityEntityRelation>
    fun findByEntityIdAndType(entityId: String, entityType: String): List<ActivityEntityRelation>
}