package com.carslab.crm.production.modules.activities.domain.service

import com.carslab.crm.production.modules.activities.domain.command.CreateActivityCommand
import com.carslab.crm.production.modules.activities.domain.model.Activity
import com.carslab.crm.production.modules.activities.domain.model.ActivityEntityRelation
import com.carslab.crm.production.modules.activities.domain.model.ActivityEntityRelationId
import com.carslab.crm.production.modules.activities.domain.model.ActivityId
import com.carslab.crm.production.modules.activities.domain.repository.ActivityEntityRelationRepository
import com.carslab.crm.production.modules.activities.domain.repository.ActivityRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ActivityDomainService(
    private val activityRepository: ActivityRepository,
    private val relationRepository: ActivityEntityRelationRepository
) {
    private val logger = LoggerFactory.getLogger(ActivityDomainService::class.java)

    fun createActivity(command: CreateActivityCommand): Activity {
        logger.debug("Creating activity for company: {}", command.companyId)

        val activityId = ActivityId.generate()
        val now = LocalDateTime.now()

        val activity = Activity(
            id = activityId,
            companyId = command.companyId,
            timestamp = now,
            category = command.category,
            message = command.message,
            userId = command.userId,
            userName = command.userName,
            status = command.status,
            statusText = command.statusText,
            primaryEntityId = command.primaryEntity?.id,
            primaryEntityType = command.primaryEntity?.type,
            primaryEntityName = command.primaryEntity?.name,
            metadata = command.metadata,
            createdAt = now
        )

        val savedActivity = activityRepository.save(activity)

        val relations = command.relatedEntities.map { entity ->
            ActivityEntityRelation(
                id = ActivityEntityRelationId.generate(),
                activityId = activityId,
                entityId = entity.id,
                entityType = entity.type,
                entityName = entity.name
            )
        }

        if (relations.isNotEmpty()) {
            relationRepository.saveAll(relations)
        }

        logger.info("Activity created: {} for company: {}", savedActivity.id.value, command.companyId)
        return savedActivity
    }

    fun getActivitiesForCompany(companyId: Long, pageable: Pageable): Page<Activity> {
        logger.debug("Fetching activities for company: {}", companyId)
        return activityRepository.findByCompanyIdPaginated(companyId, pageable)
    }

    fun getActivityForCompany(activityId: String, companyId: Long): Activity? {
        logger.debug("Fetching activity: {} for company: {}", activityId, companyId)

        val activity = activityRepository.findByIdAndCompanyId(activityId, companyId)
        return if (activity != null && activity.belongsToCompany(companyId)) {
            activity
        } else {
            null
        }
    }

    fun getRelatedEntitiesForActivities(activityIds: List<ActivityId>): List<ActivityEntityRelation> {
        return relationRepository.findByActivityIds(activityIds)
    }

    fun getRelatedEntitiesForActivity(activityId: ActivityId): List<ActivityEntityRelation> {
        return relationRepository.findByActivityId(activityId)
    }

    fun getActivitiesForEntity(entityId: String, entityType: String): List<ActivityEntityRelation> {
        return relationRepository.findByEntityIdAndType(entityId, entityType)
    }
}