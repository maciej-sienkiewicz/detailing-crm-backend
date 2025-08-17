package com.carslab.crm.production.modules.visits.domain.repository

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface VisitRepository {
    fun save(visit: Visit): Visit
    fun findById(visitId: VisitId, companyId: Long): Visit?
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Visit>
    fun existsById(visitId: VisitId, companyId: Long): Boolean
    fun deleteById(visitId: VisitId, companyId: Long): Boolean
    fun searchVisits(companyId: Long, criteria: VisitSearchCriteria, pageable: Pageable): Page<Visit>
    fun countByStatus(companyId: Long, status: VisitStatus): Long
    fun findByClientId(clientId: ClientId, companyId: Long, pageable: Pageable): Page<Visit>
    fun findByVehicleId(vehicleId: VehicleId, companyId: Long, pageable: Pageable): Page<Visit>
}

interface VisitCommentRepository {
    fun save(comment: VisitComment): VisitComment
    fun findByVisitId(visitId: VisitId): List<VisitComment>
    fun deleteById(commentId: String): Boolean
}

interface VisitMediaRepository {
    fun save(media: VisitMedia): VisitMedia
    fun findByVisitId(visitId: VisitId): List<VisitMedia>
    fun findById(mediaId: String): VisitMedia?
    fun deleteById(mediaId: String): Boolean
    fun getFileData(mediaId: String): ByteArray?
}

interface VisitDocumentRepository {
    fun save(document: VisitDocument): VisitDocument
    fun findByVisitId(visitId: VisitId): List<VisitDocument>
    fun findById(documentId: String): VisitDocument?
    fun deleteById(documentId: String): Boolean
    fun getFileData(documentId: String): ByteArray?
}

data class VisitSearchCriteria(
    val clientName: String? = null,
    val licensePlate: String? = null,
    val status: VisitStatus? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val make: String? = null,
    val model: String? = null,
    val serviceName: String? = null,
    val title: String? = null,
    val minPrice: java.math.BigDecimal? = null,
    val maxPrice: java.math.BigDecimal? = null
)