package com.carslab.crm.production.modules.media.infrastructure.repository

import com.carslab.crm.production.modules.media.domain.model.MediaContext
import com.carslab.crm.production.modules.media.infrastructure.entity.MediaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MediaJpaRepository : JpaRepository<MediaEntity, String> {

    /**
     * Znajduje media po visit ID
     */
    @Query("SELECT m FROM MediaEntity m WHERE m.visitId = :visitId ORDER BY m.createdAt DESC")
    fun findByVisitId(@Param("visitId") visitId: Long): List<MediaEntity>

    /**
     * Znajduje media bezpośrednio przypisane do pojazdu (context = VEHICLE)
     */
    @Query("""
        SELECT m FROM MediaEntity m 
        WHERE m.vehicleId = :vehicleId 
        AND m.context = 'VEHICLE' 
        AND m.companyId = :companyId 
        ORDER BY m.createdAt DESC
    """)
    fun findByVehicleIdDirect(
        @Param("vehicleId") vehicleId: Long,
        @Param("companyId") companyId: Long
    ): List<MediaEntity>

    /**
     * Znajduje wszystkie media powiązane z pojazdem (bezpośrednio + z wizyt)
     */
    @Query("""
        SELECT m FROM MediaEntity m 
        WHERE m.vehicleId = :vehicleId 
        AND m.companyId = :companyId 
        ORDER BY m.createdAt DESC
    """)
    fun findAllByVehicleId(
        @Param("vehicleId") vehicleId: Long,
        @Param("companyId") companyId: Long
    ): List<MediaEntity>

    /**
     * Znajduje media po kontekście i entity ID
     */
    @Query("""
        SELECT m FROM MediaEntity m 
        WHERE m.context = :context 
        AND m.entityId = :entityId 
        AND m.companyId = :companyId 
        ORDER BY m.createdAt DESC
    """)
    fun findByContextAndEntityId(
        @Param("context") context: MediaContext,
        @Param("entityId") entityId: Long,
        @Param("companyId") companyId: Long
    ): List<MediaEntity>

    /**
     * Sprawdza czy media istnieje i należy do firmy
     */
    fun existsByIdAndCompanyId(id: String, companyId: Long): Boolean

    /**
     * Znajduje media po company ID z paginacją
     */
    @Query("""
        SELECT m FROM MediaEntity m 
        WHERE m.companyId = :companyId 
        ORDER BY m.createdAt DESC
        LIMIT :limit OFFSET :offset
    """)
    fun findByCompanyIdWithPagination(
        @Param("companyId") companyId: Long,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<MediaEntity>

    /**
     * Liczy wszystkie media dla firmy
     */
    fun countByCompanyId(companyId: Long): Long

    /**
     * Znajduje media dla galerii z filtrami (dla przyszłego użytku)
     */
    @Query(value = """
        SELECT m.* FROM media m
        WHERE m.company_id = :companyId
        AND (:context IS NULL OR m.context = :context)
        AND (:vehicleId IS NULL OR m.vehicle_id = :vehicleId)
        ORDER BY m.created_at DESC
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    fun findForGallery(
        @Param("companyId") companyId: Long,
        @Param("context") context: String?,
        @Param("vehicleId") vehicleId: Long?,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<MediaEntity>

    /**
     * Liczy media dla galerii z filtrami
     */
    @Query(value = """
        SELECT COUNT(*) FROM media m
        WHERE m.company_id = :companyId
        AND (:context IS NULL OR m.context = :context)
        AND (:vehicleId IS NULL OR m.vehicle_id = :vehicleId)
    """, nativeQuery = true)
    fun countForGallery(
        @Param("companyId") companyId: Long,
        @Param("context") context: String?,
        @Param("vehicleId") vehicleId: Long?
    ): Long
}