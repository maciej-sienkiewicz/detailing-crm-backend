// src/main/kotlin/com/carslab/crm/modules/visits/infrastructure/persistence/repository/ProtocolDocumentJpaRepository.kt
package com.carslab.crm.modules.visits.infrastructure.persistence.repository

import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolDocumentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProtocolDocumentJpaRepository : JpaRepository<ProtocolDocumentEntity, String> {

    /**
     * Znajdź wszystkie dokumenty dla protokołu w ramach firmy
     */
    fun findByProtocolIdAndCompanyId(protocolId: Long, companyId: Long): List<ProtocolDocumentEntity>

    /**
     * Znajdź dokument po storageId w ramach firmy (bezpieczeństwo)
     */
    fun findByStorageIdAndCompanyId(storageId: String, companyId: Long): ProtocolDocumentEntity?

    /**
     * Znajdź dokumenty po typie w ramach firmy
     */
    fun findByDocumentTypeAndCompanyId(documentType: String, companyId: Long): List<ProtocolDocumentEntity>

    /**
     * Usuń wszystkie dokumenty protokołu
     */
    fun deleteAllByProtocolIdAndCompanyId(protocolId: Long, companyId: Long): Int

    /**
     * Sprawdź czy dokument istnieje i należy do firmy
     */
    fun existsByStorageIdAndCompanyId(storageId: String, companyId: Long): Boolean

    /**
     * Statystyki dokumentów dla firmy
     */
    @Query("""
        SELECT d.documentType, COUNT(d), SUM(d.fileSize) 
        FROM ProtocolDocumentEntity d 
        WHERE d.companyId = :companyId 
        GROUP BY d.documentType
    """)
    fun getDocumentStats(@Param("companyId") companyId: Long): List<Array<Any>>

    /**
     * Znajdź dokumenty starsze niż podana data (do czyszczenia)
     */
    @Query("""
        SELECT d FROM ProtocolDocumentEntity d 
        WHERE d.companyId = :companyId 
        AND d.createdAt < :cutoffDate
        AND NOT EXISTS (
            SELECT 1 FROM ProtocolEntity p 
            WHERE p.id = d.protocolId AND p.companyId = d.companyId
        )
    """)
    fun findOrphanedDocuments(
        @Param("companyId") companyId: Long,
        @Param("cutoffDate") cutoffDate: java.time.LocalDateTime
    ): List<ProtocolDocumentEntity>
}