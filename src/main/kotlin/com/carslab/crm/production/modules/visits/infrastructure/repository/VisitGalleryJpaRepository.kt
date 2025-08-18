package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.visits.domain.model.TagStat
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitMediaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface VisitGalleryJpaRepository : JpaRepository<VisitMediaEntity, String> {

    @Query(
        value = """
        SELECT vm.* FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
        AND (:protocolId IS NULL OR vm.visit_id = :protocolId)
        AND (:name IS NULL OR UPPER(vm.name::text) ILIKE UPPER(CONCAT('%', :name, '%')))
        AND (CAST(:startDate AS text) IS NULL OR vm.created_at >= CAST(:startDate AS timestamp))
        AND (CAST(:endDate AS text) IS NULL OR vm.created_at <= CAST(:endDate AS timestamp))
        ORDER BY vm.created_at DESC
        LIMIT :size OFFSET :offset
    """, nativeQuery = true
    )
    fun findImagesWithFiltersNative(
        @Param("companyId") companyId: Long,
        @Param("protocolId") protocolId: Long?,
        @Param("name") name: String?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<VisitMediaEntity>

    @Query(
        """
        SELECT COUNT(vm) FROM VisitMediaEntity vm
        JOIN VisitEntity v ON v.id = vm.visitId
        WHERE v.companyId = :companyId
        AND (:protocolId IS NULL OR vm.visitId = :protocolId)
        AND (:name IS NULL OR UPPER(CAST(vm.name AS string)) LIKE UPPER(CONCAT('%', :name, '%')))
        AND (:startDate IS NULL OR vm.createdAt >= :startDate)
        AND (:endDate IS NULL OR vm.createdAt <= :endDate)
    """
    )
    fun countImagesWithFilters(
        @Param("companyId") companyId: Long,
        @Param("protocolId") protocolId: Long?,
        @Param("name") name: String?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): Long

    @Query(
        value = """
        SELECT vm.* FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
        AND (:protocolId IS NULL OR vm.visit_id = :protocolId)
        AND (:name IS NULL OR UPPER(vm.name::text) ILIKE UPPER(CONCAT('%', :name, '%')))
        AND (CAST(:startDate AS text) IS NULL OR vm.created_at >= CAST(:startDate AS timestamp))
        AND (CAST(:endDate AS text) IS NULL OR vm.created_at <= CAST(:endDate AS timestamp))
        AND EXISTS (
            SELECT 1 FROM unnest(string_to_array(vm.tags, ',')) AS tag_elem
            WHERE tag_elem = ANY(CAST(:tags AS text[]))
        )
        ORDER BY vm.created_at DESC
        LIMIT :size OFFSET :offset
    """, nativeQuery = true
    )
    fun findImagesWithAnyTags(
        @Param("companyId") companyId: Long,
        @Param("protocolId") protocolId: Long?,
        @Param("name") name: String?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?,
        @Param("tags") tags: List<String>,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<VisitMediaEntity>

    @Query(
        value = """
        SELECT COUNT(*) FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
        AND (:protocolId IS NULL OR vm.visit_id = :protocolId)
        AND (:name IS NULL OR UPPER(vm.name::text) ILIKE UPPER(CONCAT('%', :name, '%')))
        AND (CAST(:startDate AS text) IS NULL OR vm.created_at >= CAST(:startDate AS timestamp))
        AND (CAST(:endDate AS text) IS NULL OR vm.created_at <= CAST(:endDate AS timestamp))
        AND EXISTS (
            SELECT 1 FROM unnest(string_to_array(vm.tags, ',')) AS tag_elem
            WHERE tag_elem = ANY(CAST(:tags AS text[]))
        )
    """, nativeQuery = true
    )
    fun countImagesWithAnyTags(
        @Param("companyId") companyId: Long,
        @Param("protocolId") protocolId: Long?,
        @Param("name") name: String?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?,
        @Param("tags") tags: List<String>
    ): Long

    @Query(
        value = """
        SELECT vm.* FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
        AND (:protocolId IS NULL OR vm.visit_id = :protocolId)
        AND (:name IS NULL OR UPPER(vm.name::text) ILIKE UPPER(CONCAT('%', :name, '%')))
        AND (CAST(:startDate AS text) IS NULL OR vm.created_at >= CAST(:startDate AS timestamp))
        AND (CAST(:endDate AS text) IS NULL OR vm.created_at <= CAST(:endDate AS timestamp))
        AND (
            SELECT COUNT(DISTINCT tag_elem) 
            FROM unnest(string_to_array(vm.tags, ',')) AS tag_elem
            WHERE tag_elem = ANY(CAST(:tags AS text[]))
        ) = :tagCount
        ORDER BY vm.created_at DESC
        LIMIT :size OFFSET :offset
    """, nativeQuery = true
    )
    fun findImagesWithAllTags(
        @Param("companyId") companyId: Long,
        @Param("protocolId") protocolId: Long?,
        @Param("name") name: String?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?,
        @Param("tags") tags: List<String>,
        @Param("tagCount") tagCount: Int,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<VisitMediaEntity>

    @Query(
        value = """
        SELECT COUNT(*) FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
        AND (:protocolId IS NULL OR vm.visit_id = :protocolId)
        AND (:name IS NULL OR UPPER(vm.name::text) ILIKE UPPER(CONCAT('%', :name, '%')))
        AND (CAST(:startDate AS text) IS NULL OR vm.created_at >= CAST(:startDate AS timestamp))
        AND (CAST(:endDate AS text) IS NULL OR vm.created_at <= CAST(:endDate AS timestamp))
        AND (
            SELECT COUNT(DISTINCT tag_elem) 
            FROM unnest(string_to_array(vm.tags, ',')) AS tag_elem
            WHERE tag_elem = ANY(CAST(:tags AS text[]))
        ) = :tagCount
    """, nativeQuery = true
    )
    fun countImagesWithAllTags(
        @Param("companyId") companyId: Long,
        @Param("protocolId") protocolId: Long?,
        @Param("name") name: String?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?,
        @Param("tags") tags: List<String>,
        @Param("tagCount") tagCount: Int
    ): Long

    @Query(
        value = """
        SELECT DISTINCT TRIM(tag_elem) as tag, COUNT(*) as count
        FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id,
        LATERAL unnest(string_to_array(vm.tags, ',')) AS tag_elem
        WHERE v.company_id = :companyId
        AND TRIM(tag_elem) != ''
        GROUP BY TRIM(tag_elem)
        ORDER BY count DESC, TRIM(tag_elem) ASC
    """, nativeQuery = true
    )
    fun getTagStatisticsNative(@Param("companyId") companyId: Long): List<Array<Any>>

    @Query(
        value = """
        SELECT DISTINCT TRIM(tag_elem) as tag, COUNT(*) as count
        FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id,
        LATERAL unnest(string_to_array(vm.tags, ',')) AS tag_elem
        WHERE v.company_id = :companyId
        AND TRIM(tag_elem) != ''
        GROUP BY TRIM(tag_elem)
        ORDER BY count DESC, TRIM(tag_elem) ASC
    """, nativeQuery = true
    )
    fun getTagStatistics(@Param("companyId") companyId: Long): List<Array<Any>>

    @Query(
        """
        SELECT COUNT(vm) FROM VisitMediaEntity vm
        JOIN VisitEntity v ON v.id = vm.visitId
        WHERE v.companyId = :companyId
    """
    )
    fun countTotalImages(@Param("companyId") companyId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(vm.size), 0) FROM VisitMediaEntity vm
        JOIN VisitEntity v ON v.id = vm.visitId
        WHERE v.companyId = :companyId
    """
    )
    fun getTotalSize(@Param("companyId") companyId: Long): Long

    @Query(
        value = """
        SELECT DISTINCT TRIM(tag_elem)
        FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id,
        LATERAL unnest(string_to_array(vm.tags, ',')) AS tag_elem
        WHERE v.company_id = :companyId
        AND TRIM(tag_elem) != ''
        ORDER BY TRIM(tag_elem) ASC
    """, nativeQuery = true
    )
    fun getAllTagsNative(@Param("companyId") companyId: Long): List<String>

    @Query(
        """
        SELECT vm FROM VisitMediaEntity vm
        JOIN VisitEntity v ON v.id = vm.visitId
        WHERE vm.id = :imageId AND v.companyId = :companyId
    """
    )
    fun findByIdAndCompanyExists(
        @Param("imageId") imageId: String,
        @Param("companyId") companyId: Long
    ): VisitMediaEntity?
}