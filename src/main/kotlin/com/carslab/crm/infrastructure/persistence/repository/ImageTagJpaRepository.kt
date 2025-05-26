package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.infrastructure.persistence.entity.ImageTagEntity
import com.carslab.crm.infrastructure.persistence.entity.ImageTagId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface ImageTagJpaRepository : JpaRepository<ImageTagEntity, ImageTagId> {
    fun findByImageIdAndCompanyId(imageId: String, companyId: Long): List<ImageTagEntity>

    @Query("""
    SELECT DISTINCT t FROM ImageTagEntity t 
    WHERE t.companyId = :companyId
    AND (
        (:tagCount = 0) OR
        (:matchMode = 'ANY' AND t.tag IN :tags) OR
        (:matchMode = 'ALL' AND t.imageId IN (
            SELECT t2.imageId FROM ImageTagEntity t2 
            WHERE t2.tag IN :tags AND t2.companyId = :companyId
            GROUP BY t2.imageId 
            HAVING COUNT(DISTINCT t2.tag) = :tagCount
        ))
    )
""")
    fun findByTagsAndCompanyId(
        @Param("tags") tags: List<String>,
        @Param("companyId") companyId: Long,
        @Param("matchMode") matchMode: String,
        @Param("tagCount") tagCount: Long,
        pageable: Pageable
    ): Page<ImageTagEntity>


    @Modifying
    @Transactional
    @Query("DELETE FROM ImageTagEntity t WHERE t.imageId = :imageId AND t.companyId = :companyId")
    fun deleteAllByImageIdAndCompanyId(@Param("imageId") imageId: String, @Param("companyId") companyId: Long)
}