package com.carslab.crm.infrastructure.storage.repository

import com.carslab.crm.infrastructure.storage.entity.FileMetadataEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface FileMetadataRepository : JpaRepository<FileMetadataEntity, String> {

    fun findByCompanyIdAndEntityIdAndEntityType(
        companyId: Long,
        entityId: String,
        entityType: String
    ): List<FileMetadataEntity>

    fun findByCompanyIdAndCategory(companyId: Long, category: String): List<FileMetadataEntity>

    fun findByCompanyIdAndCategoryAndSubCategory(
        companyId: Long,
        category: String,
        subCategory: String
    ): List<FileMetadataEntity>

    @Query("SELECT f FROM FileMetadataEntity f WHERE f.createdAt < :cutoffDate AND f.entityId NOT IN " +
            "(SELECT d.id FROM UnifiedDocumentEntity d WHERE d.companyId = f.companyId)")
    fun findOrphanedFiles(@Param("cutoffDate") cutoffDate: LocalDateTime): List<FileMetadataEntity>

    @Query("SELECT f.category, f.subCategory, COUNT(f), SUM(f.fileSize) FROM FileMetadataEntity f " +
            "WHERE f.companyId = :companyId GROUP BY f.category, f.subCategory")
    fun getStorageStats(@Param("companyId") companyId: Long): List<Array<Any>>
}