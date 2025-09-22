package com.carslab.crm.production.modules.activities.infrastructure.repository

import com.carslab.crm.production.modules.activities.domain.model.Activity
import com.carslab.crm.production.modules.activities.domain.model.ActivityId
import com.carslab.crm.production.modules.activities.domain.repository.ActivityRepository
import com.carslab.crm.production.modules.activities.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.activities.infrastructure.mapper.toEntity
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class ActivityRepositoryImpl(
    private val jpaRepository: ActivityJpaRepository
) : ActivityRepository {

    private val logger = LoggerFactory.getLogger(ActivityRepositoryImpl::class.java)

    override fun save(activity: Activity): Activity {
        logger.debug("Saving activity: {} for company: {}", activity.id.value, activity.companyId)
        val entity = activity.toEntity()
        val savedEntity = jpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: ActivityId): Activity? {
        return jpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByIdAndCompanyId(id: String, companyId: Long): Activity? {
        return jpaRepository.findByIdAndCompanyId(id, companyId)?.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByCompanyIdPaginated(companyId: Long, pageable: Pageable): Page<Activity> {
        logger.debug("Finding paginated activities for company: {}", companyId)
        return jpaRepository.findByCompanyIdOrderByTimestampDesc(companyId, pageable)
            .map { it.toDomain() }
    }
}