package com.carslab.crm.production.modules.activities.infrastructure.repository

import com.carslab.crm.production.modules.activities.infrastructure.entity.ActivityEntityRelationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ActivityEntityRelationJpaRepository : JpaRepository<ActivityEntityRelationEntity, String> {
    fun findByActivityId(activityId: String): List<ActivityEntityRelationEntity>

    @Query("SELECT r FROM ActivityEntityRelationEntity r WHERE r.activityId IN :activityIds")
    fun findByActivityIdIn(@Param("activityIds") activityIds: List<String>): List<ActivityEntityRelationEntity>

    fun findByEntityIdAndEntityType(entityId: String, entityType: String): List<ActivityEntityRelationEntity>
}