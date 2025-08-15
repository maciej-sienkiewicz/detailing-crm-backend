package com.carslab.crm.production.modules.activities.infrastructure.repository

import com.carslab.crm.production.modules.activities.infrastructure.entity.ActivityEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ActivityJpaRepository : JpaRepository<ActivityEntity, String> {
    fun findByIdAndCompanyId(id: String, companyId: Long): ActivityEntity?
    fun findByCompanyIdOrderByTimestampDesc(companyId: Long, pageable: Pageable): Page<ActivityEntity>
}