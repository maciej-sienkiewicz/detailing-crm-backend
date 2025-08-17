package com.carslab.crm.production.modules.visits.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.*
import com.carslab.crm.production.modules.visits.domain.service.*
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitQueryService(
    private val visitDomainService: VisitDomainService,
    private val commentService: VisitCommentService,
    private val mediaService: VisitMediaService,
    private val documentService: VisitDocumentService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitQueryService::class.java)

    fun getVisitsForCurrentCompany(pageable: Pageable): Page<VisitResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visits for company: {}", companyId)

        val visits = visitDomainService.getVisitsForCompany(companyId, pageable)
        logger.debug("Found {} visits for company: {}", visits.numberOfElements, companyId)

        return visits.map { VisitResponse.from(it) }
    }

    fun getVisit(visitId: String): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visit: {} for company: {}", visitId, companyId)

        val visit = visitDomainService.getVisitForCompany(VisitId.of(visitId), companyId)
        logger.debug("Visit found: {}", visit.title)

        return VisitResponse.from(visit)
    }
    
    fun getVisitCounters(): VisitCountersResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visit counters for company: {}", companyId)

        return VisitCountersResponse(
            scheduled = visitDomainService.getVisitCountByStatus(companyId, VisitStatus.SCHEDULED),
            inProgress = visitDomainService.getVisitCountByStatus(companyId, VisitStatus.IN_PROGRESS),
            readyForPickup = visitDomainService.getVisitCountByStatus(companyId, VisitStatus.READY_FOR_PICKUP),
            completed = visitDomainService.getVisitCountByStatus(companyId, VisitStatus.COMPLETED),
            cancelled = visitDomainService.getVisitCountByStatus(companyId, VisitStatus.CANCELLED),
            all = VisitStatus.values().sumOf { visitDomainService.getVisitCountByStatus(companyId, it) }
        )
    }

    fun getVisitComments(visitId: String): List<VisitCommentResponse> {
        logger.debug("Fetching comments for visit: {}", visitId)

        val comments = commentService.getCommentsForVisit(VisitId.of(visitId))
        return comments.map { VisitCommentResponse.from(it) }
    }

    fun getVisitMedia(visitId: String): List<VisitMediaResponse> {
        logger.debug("Fetching media for visit: {}", visitId)

        val media = mediaService.getMediaForVisit(VisitId.of(visitId))
        return media.map { VisitMediaResponse.from(it) }
    }

    fun getVisitDocuments(visitId: String): List<VisitDocumentResponse> {
        logger.debug("Fetching documents for visit: {}", visitId)

        val documents = documentService.getDocumentsForVisit(VisitId.of(visitId))
        return documents.map { VisitDocumentResponse.from(it) }
    }

    fun getMediaFile(mediaId: String): ByteArray? {
        logger.debug("Fetching media file: {}", mediaId)
        return mediaService.getMediaData(mediaId)
    }

    fun getDocumentFile(documentId: String): ByteArray? {
        logger.debug("Fetching document file: {}", documentId)
        return documentService.getDocumentData(documentId)
    }

    fun getVisitsForClient(clientId: String, pageable: Pageable): Page<VisitResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visits for client: {} and company: {}", clientId, companyId)

        val visits = visitDomainService.getVisitsForClient(ClientId.of(clientId.toLong()), companyId, pageable)
        return visits.map { VisitResponse.from(it) }
    }

    fun getVisitsForVehicle(vehicleId: String, pageable: Pageable): Page<VisitResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visits for vehicle: {} and company: {}", vehicleId, companyId)

        val visits = visitDomainService.getVisitsForVehicle(VehicleId.of(vehicleId.toLong()), companyId, pageable)
        return visits.map { VisitResponse.from(it) }
    }
}