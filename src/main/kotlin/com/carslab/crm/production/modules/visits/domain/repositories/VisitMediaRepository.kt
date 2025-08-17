package com.carslab.crm.production.modules.visits.domain.repositories

import com.carslab.crm.production.modules.visits.domain.models.entities.VisitMedia
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId

interface VisitMediaRepository {
    fun save(media: VisitMedia): VisitMedia
    fun findByVisitId(visitId: VisitId): List<VisitMedia>
    fun findById(mediaId: String): VisitMedia?
    fun existsVisitByIdAndCompanyId(visitId: VisitId, companyId: Long): Boolean
    fun deleteById(mediaId: String): Boolean
    fun getFileData(mediaId: String): ByteArray?
}