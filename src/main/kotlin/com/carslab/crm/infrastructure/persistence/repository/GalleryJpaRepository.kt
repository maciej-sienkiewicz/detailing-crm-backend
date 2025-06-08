// src/main/kotlin/com/carslab/crm/infrastructure/persistence/repository/GalleryJpaRepository.kt
package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.infrastructure.persistence.entity.VehicleImageEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GalleryJpaRepository : JpaRepository<VehicleImageEntity, String> {

    @Query(nativeQuery = true, value = """
        SELECT v.* FROM vehicle_images v
        WHERE v.company_id = :companyId
        AND (:protocolId IS NULL OR v.protocol_id = CAST(:protocolId AS BIGINT))
        AND (:name IS NULL OR v.name ILIKE CONCAT('%', :name, '%'))
        AND (:startDate IS NULL OR v.created_at >= CAST(:startDate AS TIMESTAMP))
        AND (:endDate IS NULL OR v.created_at <= CAST(:endDate AS TIMESTAMP))
        ORDER BY v.created_at DESC
        LIMIT :size OFFSET :offset
    """)
    fun findImagesWithFiltersNative(
        @Param("companyId") companyId: Long,
        @Param("protocolId") protocolId: Long?,
        @Param("name") name: String?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<VehicleImageEntity>

    @Query(nativeQuery = true, value = """
        SELECT COUNT(*) FROM vehicle_images v
        WHERE v.company_id = :companyId
        AND (:protocolId IS NULL OR v.protocol_id = CAST(:protocolId AS BIGINT))
        AND (:name IS NULL OR v.name ILIKE CONCAT('%', :name, '%'))
        AND (:startDate IS NULL OR v.created_at >= CAST(:startDate AS TIMESTAMP))
        AND (:endDate IS NULL OR v.created_at <= CAST(:endDate AS TIMESTAMP))
    """)
    fun countImagesWithFiltersNative(
        @Param("companyId") companyId: Long,
        @Param("protocolId") protocolId: Long?,
        @Param("name") name: String?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?
    ): Long

    @Query(nativeQuery = true, value = """
        SELECT v.* FROM vehicle_images v
        WHERE v.company_id = :companyId
        AND (:protocolId IS NULL OR v.protocol_id = CAST(:protocolId AS BIGINT))
        AND (:name IS NULL OR v.name ILIKE CONCAT('%', :name, '%'))
        AND (:startDate IS NULL OR v.created_at >= CAST(:startDate AS TIMESTAMP))
        AND (:endDate IS NULL OR v.created_at <= CAST(:endDate AS TIMESTAMP))
        AND (
            :tagsEmpty = true OR
            (
                :matchMode = 'ANY' AND v.id IN (
                    SELECT t.image_id FROM image_tags t 
                    WHERE t.tag = ANY(CAST(:tags AS text[])) AND t.company_id = :companyId
                )
            ) OR
            (
                :matchMode = 'ALL' AND v.id IN (
                    SELECT t.image_id FROM image_tags t 
                    WHERE t.tag = ANY(CAST(:tags AS text[])) AND t.company_id = :companyId
                    GROUP BY t.image_id 
                    HAVING COUNT(DISTINCT t.tag) = :tagCount
                )
            )
        )
        ORDER BY v.created_at DESC
        LIMIT :size OFFSET :offset
    """)
    fun findImagesWithTagFiltersNative(
        @Param("companyId") companyId: Long,
        @Param("protocolId") protocolId: Long?,
        @Param("name") name: String?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?,
        @Param("tags") tags: Array<String>,
        @Param("tagsEmpty") tagsEmpty: Boolean,
        @Param("matchMode") matchMode: String,
        @Param("tagCount") tagCount: Int,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<VehicleImageEntity>

    @Query(nativeQuery = true, value = """
        SELECT COUNT(*) FROM vehicle_images v
        WHERE v.company_id = :companyId
        AND (:protocolId IS NULL OR v.protocol_id = CAST(:protocolId AS BIGINT))
        AND (:name IS NULL OR v.name ILIKE CONCAT('%', :name, '%'))
        AND (:startDate IS NULL OR v.created_at >= CAST(:startDate AS TIMESTAMP))
        AND (:endDate IS NULL OR v.created_at <= CAST(:endDate AS TIMESTAMP))
        AND (
            :tagsEmpty = true OR
            (
                :matchMode = 'ANY' AND v.id IN (
                    SELECT t.image_id FROM image_tags t 
                    WHERE t.tag = ANY(CAST(:tags AS text[])) AND t.company_id = :companyId
                )
            ) OR
            (
                :matchMode = 'ALL' AND v.id IN (
                    SELECT t.image_id FROM image_tags t 
                    WHERE t.tag = ANY(CAST(:tags AS text[])) AND t.company_id = :companyId
                    GROUP BY t.image_id 
                    HAVING COUNT(DISTINCT t.tag) = :tagCount
                )
            )
        )
    """)
    fun countImagesWithTagFiltersNative(
        @Param("companyId") companyId: Long,
        @Param("protocolId") protocolId: Long?,
        @Param("name") name: String?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?,
        @Param("tags") tags: Array<String>,
        @Param("tagsEmpty") tagsEmpty: Boolean,
        @Param("matchMode") matchMode: String,
        @Param("tagCount") tagCount: Int
    ): Long

    @Query(nativeQuery = true, value = """
        SELECT t.tag, COUNT(DISTINCT t.image_id) as count
        FROM image_tags t 
        JOIN vehicle_images v ON t.image_id = v.id
        WHERE t.company_id = :companyId
        GROUP BY t.tag
        ORDER BY count DESC, t.tag ASC
    """)
    fun getTagStatisticsNative(@Param("companyId") companyId: Long): List<Array<Any>>

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM vehicle_images WHERE company_id = :companyId")
    fun countTotalImagesNative(@Param("companyId") companyId: Long): Long

    @Query(nativeQuery = true, value = "SELECT COALESCE(SUM(size), 0) FROM vehicle_images WHERE company_id = :companyId")
    fun getTotalSizeNative(@Param("companyId") companyId: Long): Long
}