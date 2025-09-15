package com.carslab.crm.production.modules.media.domain.repository

import com.carslab.crm.production.modules.media.domain.model.Media
import com.carslab.crm.production.modules.media.domain.model.MediaContext
import com.carslab.crm.production.modules.media.domain.model.MediaId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId

interface MediaRepository {

    fun save(media: Media): Media

    fun findById(mediaId: MediaId): Media?

    fun findByVisitId(visitId: VisitId): List<Media>

    fun findByVehicleId(vehicleId: VehicleId, companyId: Long): List<Media>

    fun findAllVehicleMedia(vehicleId: VehicleId, companyId: Long): List<Media>

    fun findByContextAndEntityId(context: MediaContext, entityId: Long, companyId: Long): List<Media>

    fun findByContextAndCompanyId(context: MediaContext, companyId: Long): List<Media>

    fun deleteById(mediaId: MediaId): Boolean

    fun existsByIdAndCompanyId(mediaId: MediaId, companyId: Long): Boolean

    fun findByCompanyId(companyId: Long): List<Media>

    fun findByCompanyId(companyId: Long, limit: Int, offset: Int): List<Media>

    fun countByCompanyId(companyId: Long): Long
}