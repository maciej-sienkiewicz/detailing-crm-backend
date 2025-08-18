package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitMediaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VisitGalleryJpaRepository : JpaRepository<VisitMediaEntity, String> {

    @Query(
        value = """
        SELECT vm.* FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
        ORDER BY vm.created_at DESC
        LIMIT :size OFFSET :offset
    """, nativeQuery = true
    )
    fun findImagesForCompany(
        @Param("companyId") companyId: Long,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<VisitMediaEntity>

    @Query(
        value = """
        SELECT COUNT(*) FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
    """, nativeQuery = true
    )
    fun countImagesForCompany(
        @Param("companyId") companyId: Long
    ): Long

    @Query(
        value = """
        SELECT vm.* FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
        AND EXISTS (
            SELECT 1 FROM unnest(string_to_array(COALESCE(vm.tags, ''), ',')) AS tag_elem
            WHERE TRIM(tag_elem) != '' AND TRIM(tag_elem) IN (:tags)
        )
        ORDER BY vm.created_at DESC
        LIMIT :size OFFSET :offset
    """, nativeQuery = true
    )
    fun findImagesWithAnyTags(
        @Param("companyId") companyId: Long,
        @Param("tags") tags: List<String>,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<VisitMediaEntity>

    @Query(
        value = """
        SELECT COUNT(*) FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
        AND EXISTS (
            SELECT 1 FROM unnest(string_to_array(COALESCE(vm.tags, ''), ',')) AS tag_elem
            WHERE TRIM(tag_elem) != '' AND TRIM(tag_elem) IN (:tags)
        )
    """, nativeQuery = true
    )
    fun countImagesWithAnyTags(
        @Param("companyId") companyId: Long,
        @Param("tags") tags: List<String>
    ): Long

    @Query(
        value = """
        SELECT vm.* FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
        AND (
            SELECT COUNT(DISTINCT tag_elem) 
            FROM unnest(string_to_array(COALESCE(vm.tags, ''), ',')) AS tag_elem
            WHERE TRIM(tag_elem) != '' AND TRIM(tag_elem) IN (:tags)
        ) = :tagCount
        ORDER BY vm.created_at DESC
        LIMIT :size OFFSET :offset
    """, nativeQuery = true
    )
    fun findImagesWithAllTags(
        @Param("companyId") companyId: Long,
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
        AND (
            SELECT COUNT(DISTINCT tag_elem) 
            FROM unnest(string_to_array(COALESCE(vm.tags, ''), ',')) AS tag_elem
            WHERE TRIM(tag_elem) != '' AND TRIM(tag_elem) IN (:tags)
        ) = :tagCount
    """, nativeQuery = true
    )
    fun countImagesWithAllTags(
        @Param("companyId") companyId: Long,
        @Param("tags") tags: List<String>,
        @Param("tagCount") tagCount: Int
    ): Long

    @Query(
        value = """
        SELECT DISTINCT TRIM(tag_elem) as tag, COUNT(*) as count
        FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id,
        LATERAL unnest(string_to_array(COALESCE(vm.tags, ''), ',')) AS tag_elem
        WHERE v.company_id = :companyId
        AND TRIM(tag_elem) != ''
        GROUP BY TRIM(tag_elem)
        ORDER BY count DESC, TRIM(tag_elem) ASC
    """, nativeQuery = true
    )
    fun getTagStatistics(@Param("companyId") companyId: Long): List<Array<Any>>

    @Query(
        value = """
        SELECT COUNT(*) FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
    """, nativeQuery = true
    )
    fun countTotalImages(@Param("companyId") companyId: Long): Long

    @Query(
        value = """
        SELECT COALESCE(SUM(vm.size), 0) FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE v.company_id = :companyId
    """, nativeQuery = true
    )
    fun getTotalSize(@Param("companyId") companyId: Long): Long

    @Query(
        value = """
        SELECT DISTINCT TRIM(tag_elem)
        FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id,
        LATERAL unnest(string_to_array(COALESCE(vm.tags, ''), ',')) AS tag_elem
        WHERE v.company_id = :companyId
        AND TRIM(tag_elem) != ''
        ORDER BY TRIM(tag_elem) ASC
    """, nativeQuery = true
    )
    fun getAllTagsNative(@Param("companyId") companyId: Long): List<String>

    @Query(
        value = """
        SELECT vm.* FROM visit_media vm
        JOIN visits v ON v.id = vm.visit_id
        WHERE vm.id = :imageId AND v.company_id = :companyId
    """, nativeQuery = true
    )
    fun findByIdAndCompanyExists(
        @Param("imageId") imageId: String,
        @Param("companyId") companyId: Long
    ): VisitMediaEntity?
}