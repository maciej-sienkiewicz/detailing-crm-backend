package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitCommentEntity
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitDocumentEntity
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitEntity
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitMediaEntity
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitServiceEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VisitJpaRepository : JpaRepository<VisitEntity, Long>, JpaSpecificationExecutor<VisitEntity> {

    fun findByIdAndCompanyId(id: Long, companyId: Long): Optional<VisitEntity>

    @EntityGraph(attributePaths = ["services"])
    @Query("SELECT v FROM VisitEntity v WHERE v.id = :id AND v.companyId = :companyId")
    fun findByIdAndCompanyIdWithServices(
        @Param("id") id: Long,
        @Param("companyId") companyId: Long
    ): VisitEntity?

    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<VisitEntity>

    @EntityGraph(attributePaths = ["services"])
    @Query("SELECT v FROM VisitEntity v WHERE v.companyId = :companyId")
    fun findByCompanyIdWithServices(
        @Param("companyId") companyId: Long,
        pageable: Pageable
    ): Page<VisitEntity>

    fun countByCompanyIdAndStatus(companyId: Long, status: VisitStatus): Long

    fun findByClientIdAndCompanyId(clientId: Long, companyId: Long, pageable: Pageable): Page<VisitEntity>

    @EntityGraph(attributePaths = ["services"])
    @Query("SELECT v FROM VisitEntity v WHERE v.clientId = :clientId AND v.companyId = :companyId")
    fun findByClientIdAndCompanyIdWithServices(
        @Param("clientId") clientId: Long,
        @Param("companyId") companyId: Long,
        pageable: Pageable
    ): Page<VisitEntity>

    fun findByVehicleIdAndCompanyId(vehicleId: Long, companyId: Long, pageable: Pageable): Page<VisitEntity>

    @EntityGraph(attributePaths = ["services"])
    @Query("SELECT v FROM VisitEntity v WHERE v.vehicleId = :vehicleId AND v.companyId = :companyId")
    fun findByVehicleIdAndCompanyIdWithServices(
        @Param("vehicleId") vehicleId: Long,
        @Param("companyId") companyId: Long,
        pageable: Pageable
    ): Page<VisitEntity>

    fun existsByIdAndCompanyId(id: Long, companyId: Long): Boolean

    fun deleteByIdAndCompanyId(id: Long, companyId: Long): Long

    @Query("""
        SELECT v FROM VisitEntity v 
        WHERE v.companyId = :companyId 
        AND v.status IN :statuses
        ORDER BY v.updatedAt DESC
    """)
    fun findByCompanyIdAndStatusIn(
        @Param("companyId") companyId: Long,
        @Param("statuses") statuses: List<VisitStatus>,
        pageable: Pageable
    ): Page<VisitEntity>
}

@Repository
interface VisitServiceJpaRepository : JpaRepository<VisitServiceEntity, String> {

    fun findByVisitId(visitId: Long): List<VisitServiceEntity>

    fun deleteByVisitId(visitId: Long)

    @Modifying
    @Query("""
        UPDATE VisitServiceEntity vs 
        SET vs.approvalStatus = :status 
        WHERE vs.visitId = :visitId AND vs.id IN :serviceIds
    """)
    fun bulkUpdateApprovalStatus(
        @Param("visitId") visitId: Long,
        @Param("serviceIds") serviceIds: List<String>,
        @Param("status") status: ServiceApprovalStatus
    )

    @Query("""
        SELECT vs FROM VisitServiceEntity vs 
        WHERE vs.visitId IN :visitIds 
        ORDER BY vs.visitId, vs.name
    """)
    fun findByVisitIds(@Param("visitIds") visitIds: List<Long>): List<VisitServiceEntity>

    @Query("""
        SELECT vs.visitId as visitId, COUNT(vs) as serviceCount, SUM(vs.finalPrice) as totalAmount
        FROM VisitServiceEntity vs 
        WHERE vs.visitId IN :visitIds 
        GROUP BY vs.visitId
    """)
    fun getVisitServiceSummary(@Param("visitIds") visitIds: List<Long>): List<VisitServiceSummaryProjection>
}

@Repository
interface VisitCommentJpaRepository : JpaRepository<VisitCommentEntity, String> {

    fun findByVisitIdOrderByCreatedAtDesc(visitId: Long): List<VisitCommentEntity>

    fun deleteByVisitId(visitId: Long)
}

@Repository
interface VisitMediaJpaRepository : JpaRepository<VisitMediaEntity, String> {

    fun findByVisitId(visitId: Long): List<VisitMediaEntity>

    fun deleteByVisitId(visitId: Long)
}

@Repository
interface VisitDocumentJpaRepository : JpaRepository<VisitDocumentEntity, String> {

    fun findByVisitId(visitId: Long): List<VisitDocumentEntity>

    fun deleteByVisitId(visitId: Long)
}

interface VisitServiceSummaryProjection {
    val visitId: Long
    val serviceCount: Long
    val totalAmount: java.math.BigDecimal
}