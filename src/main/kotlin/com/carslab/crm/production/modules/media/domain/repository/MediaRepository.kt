package com.carslab.crm.production.modules.media.domain.repository

import com.carslab.crm.production.modules.media.domain.model.Media
import com.carslab.crm.production.modules.media.domain.model.MediaContext
import com.carslab.crm.production.modules.media.domain.model.MediaId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId

interface MediaRepository {

    /**
     * Zapisuje media do bazy danych
     */
    fun save(media: Media): Media

    /**
     * Znajduje media po ID
     */
    fun findById(mediaId: MediaId): Media?

    /**
     * Znajduje wszystkie media dla wizyty
     */
    fun findByVisitId(visitId: VisitId): List<Media>

    /**
     * Znajduje media bezpośrednio przypisane do pojazdu (context = VEHICLE)
     */
    fun findByVehicleId(vehicleId: VehicleId, companyId: Long): List<Media>

    /**
     * Znajduje wszystkie media powiązane z pojazdem:
     * - Bezpośrednio przypisane (context = VEHICLE, entity_id = vehicleId)
     * - Z wizyt (context = VISIT, vehicle_id = vehicleId)
     */
    fun findAllVehicleMedia(vehicleId: VehicleId, companyId: Long): List<Media>

    /**
     * Znajduje media po kontekście i entity ID
     */
    fun findByContextAndEntityId(context: MediaContext, entityId: Long, companyId: Long): List<Media>

    /**
     * Usuwa media po ID
     */
    fun deleteById(mediaId: MediaId): Boolean

    /**
     * Sprawdza czy media istnieje i należy do firmy
     */
    fun existsByIdAndCompanyId(mediaId: MediaId, companyId: Long): Boolean

    /**
     * Znajduje media po company ID z paginacją
     */
    fun findByCompanyId(companyId: Long, limit: Int, offset: Int): List<Media>

    /**
     * Liczy wszystkie media dla firmy
     */
    fun countByCompanyId(companyId: Long): Long
}