package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitCommentEntity
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitDocumentEntity
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitEntity
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitMediaEntity
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitServiceEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VisitJpaRepository : JpaRepository<VisitEntity, Long>, JpaSpecificationExecutor<VisitEntity> {

    fun findByIdAndCompanyId(id: Long, companyId: Long): Optional<VisitEntity>

    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<VisitEntity>

    fun countByCompanyIdAndStatus(companyId: Long, status: VisitStatus): Long

    fun findByClientIdAndCompanyId(clientId: Long, companyId: Long, pageable: Pageable): Page<VisitEntity>

    fun findByVehicleIdAndCompanyId(vehicleId: Long, companyId: Long, pageable: Pageable): Page<VisitEntity>

    fun existsByIdAndCompanyId(id: Long, companyId: Long): Boolean

    fun deleteByIdAndCompanyId(id: Long, companyId: Long): Long
}

@Repository
interface VisitServiceJpaRepository : JpaRepository<VisitServiceEntity, String> {

    fun findByVisitId(visitId: Long): List<VisitServiceEntity>

    fun deleteByVisitId(visitId: Long)
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