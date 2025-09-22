package com.carslab.crm.production.modules.activities.infrastructure.repository

import com.carslab.crm.production.modules.activities.domain.model.ActivityEntityRelation
import com.carslab.crm.production.modules.activities.domain.model.ActivityId
import com.carslab.crm.production.modules.activities.domain.repository.ActivityEntityRelationRepository
import com.carslab.crm.production.modules.activities.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.activities.infrastructure.mapper.toEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class ActivityEntityRelationRepositoryImpl(
    private val jpaRepository: ActivityEntityRelationJpaRepository
) : ActivityEntityRelationRepository {

    private val logger = LoggerFactory.getLogger(ActivityEntityRelationRepositoryImpl::class.java)

    override fun save(relation: ActivityEntityRelation): ActivityEntityRelation {
        logger.debug("Saving activity relation: {}", relation.id.value)
        val entity = relation.toEntity()
        val savedEntity = jpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun saveAll(relations: List<ActivityEntityRelation>): List<ActivityEntityRelation> {
        logger.debug("Saving {} activity relations", relations.size)
        val entities = relations.map { it.toEntity() }
        val savedEntities = jpaRepository.saveAll(entities)
        return savedEntities.map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByActivityId(activityId: ActivityId): List<ActivityEntityRelation> {
        logger.debug("Finding relations for activity: {}", activityId.value)
        val entities = jpaRepository.findByActivityId(activityId.value)
        return entities.map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByActivityIds(activityIds: List<ActivityId>): List<ActivityEntityRelation> {
        logger.debug("Finding relations for {} activities", activityIds.size)
        val activityIdStrings = activityIds.map { it.value }
        val entities = jpaRepository.findByActivityIdIn(activityIdStrings)
        return entities.map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByEntityIdAndType(entityId: String, entityType: String): List<ActivityEntityRelation> {
        logger.debug("Finding relations for entity: {} of type: {}", entityId, entityType)
        val entities = jpaRepository.findByEntityIdAndEntityType(entityId, entityType)
        return entities.map { it.toDomain() }
    }
}
