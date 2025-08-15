package com.carslab.crm.production.modules.activities.domain.repository

import com.carslab.crm.production.modules.activities.domain.model.Activity
import com.carslab.crm.production.modules.activities.domain.model.ActivityId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface ActivityRepository {
    fun save(activity: Activity): Activity
    fun findById(id: ActivityId): Activity?
    fun findByIdAndCompanyId(id: String, companyId: Long): Activity?
    fun findByCompanyIdPaginated(companyId: Long, pageable: Pageable): Page<Activity>
}