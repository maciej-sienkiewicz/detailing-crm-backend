package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.infrastructure.persistence.entity.ImageTagEntity
import com.carslab.crm.infrastructure.persistence.entity.ImageTagId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface ImageTagJpaRepository : JpaRepository<ImageTagEntity, ImageTagId> {
    fun findByImageIdAndCompanyId(imageId: String, companyId: Long): List<ImageTagEntity>

    @Modifying
    @Query("DELETE FROM ImageTagEntity t WHERE t.imageId = :imageId AND t.companyId = :companyId")
    fun deleteAllByImageIdAndCompanyId(@Param("imageId") imageId: String, @Param("companyId") companyId: Long)
}