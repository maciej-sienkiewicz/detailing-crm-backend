package com.carslab.crm.production.modules.visits.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.*
import com.carslab.crm.production.modules.visits.domain.command.*
import com.carslab.crm.production.modules.visits.domain.model.*
import com.carslab.crm.production.modules.visits.domain.service.*
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
import com.carslab.crm.production.modules.vehicles.application.service.VehicleQueryService
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class VisitCommandService(
    private val visitDomainService: VisitDomainService,
    private val commentService: VisitCommentService,
    private val mediaService: VisitMediaService,
    private val documentService: VisitDocumentService,
    private val clientQueryService: ClientQueryService,
    private val vehicleQueryService: VehicleQueryService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitCommandService::class.java)

    fun createVisit(request: CreateVisitRequest): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating visit '{}' for company: {}", request.title, companyId)

        validateCreateRequest(request)

        val command = CreateVisitCommand(
           companyId = companyId,
            title = request.title,
            clientId = ClientId.of(1),
            vehicleId = VehicleId.of(1),
            startDate = LocalDateTime.parse(request.startDate),
            endDate = request.endDate?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
            status = VisitStatus.SCHEDULED,
            services = request.selectedServices?.map { mapToCreateServiceCommand(it) } ?: emptyList(),
            notes = request.notes,
            referralSource = request.referralSource?.toString()?.uppercase()?.let { ReferralSource.valueOf(it) },
            appointmentId = request.appointmentId,
            calendarColorId = request.calendarColorId,
            keysProvided = request.keysProvided ?: false,
            documentsProvided = request.documentsProvided ?: false
        )

        val visit = visitDomainService.createVisit(command)
        logger.info("Visit created successfully: {}", visit.id?.value)

        return VisitResponse.from(visit)
    }

    fun updateVisit(visitId: String, request: UpdateVisitRequest): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating visit: {} for company: {}", visitId, companyId)

        validateUpdateRequest(request)

        val command = UpdateVisitCommand(
            title = request.title,
            startDate = request.startDate,
            endDate = request.endDate,
            services = request.services.map { mapToUpdateServiceCommand(it) },
            notes = request.notes,
            referralSource = request.referralSource,
            appointmentId = request.appointmentId,
            calendarColorId = request.calendarColorId,
            keysProvided = request.keysProvided,
            documentsProvided = request.documentsProvided
        )

        val visit = visitDomainService.updateVisit(VisitId.of(visitId), command, companyId)
        logger.info("Visit updated successfully: {}", visitId)

        return VisitResponse.from(visit)
    }

    fun changeVisitStatus(visitId: String, request: ChangeStatusRequest): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Changing status for visit: {} to {} for company: {}", visitId, request.status, companyId)

        val command = ChangeVisitStatusCommand(
            visitId = VisitId.of(visitId),
            newStatus = request.status,
            reason = request.reason,
            companyId = companyId
        )

        val visit = visitDomainService.changeVisitStatus(command)
        logger.info("Visit status changed successfully: {}", visitId)

        return VisitResponse.from(visit)
    }

    fun deleteVisit(visitId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting visit: {} for company: {}", visitId, companyId)

        val deleted = visitDomainService.deleteVisit(VisitId.of(visitId), companyId)
        if (deleted) {
            logger.info("Visit deleted successfully: {}", visitId)
        } else {
            logger.warn("Visit not found for deletion: {}", visitId)
        }
    }

    fun addComment(visitId: String, request: AddCommentRequest): VisitCommentResponse {
        val companyId = securityContext.getCurrentCompanyId()
        val author = securityContext.getCurrentUserName() ?: "System"

        logger.info("Adding comment to visit: {} for company: {}", visitId, companyId)

        val command = AddCommentCommand(
            visitId = VisitId.of(visitId),
            content = request.content,
            type = request.type,
            author = author
        )

        val comment = commentService.addComment(command, companyId)
        logger.info("Comment added successfully to visit: {}", visitId)

        return VisitCommentResponse.from(comment)
    }

    fun uploadMedia(visitId: String, request: UploadMediaRequest): VisitMediaResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Uploading media to visit: {} for company: {}", visitId, companyId)

        val command = UploadMediaCommand(
            visitId = VisitId.of(visitId),
            file = request.file,
            metadata = MediaMetadata(
                name = request.name,
                description = request.description,
                location = request.location,
                tags = request.tags
            )
        )

        val media = mediaService.uploadMedia(command, companyId)
        logger.info("Media uploaded successfully to visit: {}", visitId)

        return VisitMediaResponse.from(media)
    }

    fun uploadDocument(visitId: String, request: UploadDocumentRequest): VisitDocumentResponse {
        val companyId = securityContext.getCurrentCompanyId()
        val uploadedBy = securityContext.getCurrentUserName() ?: "System"

        logger.info("Uploading document to visit: {} for company: {}", visitId, companyId)

        val command = UploadDocumentCommand(
            visitId = VisitId.of(visitId),
            file = request.file,
            documentType = request.documentType,
            description = request.description
        )

        val document = documentService.uploadDocument(command, companyId, uploadedBy)
        logger.info("Document uploaded successfully to visit: {}", visitId)

        return VisitDocumentResponse.from(document)
    }

    private fun validateCreateRequest(request: CreateVisitRequest) {
        if (request.title.isBlank()) {
            throw BusinessException("Visit title cannot be blank")
        }
    }

    private fun validateUpdateRequest(request: UpdateVisitRequest) {
        if (request.title.isBlank()) {
            throw BusinessException("Visit title cannot be blank")
        }
        if (request.startDate.isAfter(request.endDate)) {
            throw BusinessException("Start date cannot be after end date")
        }
    }

    private fun validateClientAndVehicleExist(clientId: Long, vehicleId: Long) {
        if (!clientQueryService.findByIds(listOf(ClientId.of(clientId))).any()) {
            throw BusinessException("Client not found: $clientId")
        }
        if (!vehicleQueryService.exists(VehicleId.of(vehicleId))) {
            throw BusinessException("Vehicle not found: $vehicleId")
        }
    }

    private fun mapToCreateServiceCommand(request: CreateServiceRequest): CreateServiceCommand {
        return CreateServiceCommand(
            name = request.name,
            basePrice = request.basePrice,
            quantity = request.quantity,
            discountType = request.discountType,
            discountValue = request.discountValue,
            finalPrice = request.finalPrice,
            approvalStatus = request.approvalStatus,
            note = request.note
        )
    }
    
    private fun mapToCreateServiceCommand(request: com.carslab.crm.modules.visits.api.commands.CreateServiceCommand): CreateServiceCommand {
        return CreateServiceCommand(
            name = request.name,
            basePrice = request.price.toBigDecimal(),
            quantity = request.quantity,
            discountType = request.discountType.toString().uppercase().let { DiscountType.valueOf(it) },
            discountValue = request.discountValue?.toBigDecimal(),
            finalPrice = request.finalPrice?.toBigDecimal(),
            approvalStatus = request.approvalStatus.toString().uppercase().let { ServiceApprovalStatus.valueOf(it) },
            note = request.note
        )
    }

    private fun mapToUpdateServiceCommand(request: UpdateServiceRequest): UpdateServiceCommand {
        return UpdateServiceCommand(
            id = request.id,
            name = request.name,
            basePrice = request.basePrice,
            quantity = request.quantity,
            discountType = request.discountType,
            discountValue = request.discountValue,
            finalPrice = request.finalPrice,
            approvalStatus = request.approvalStatus,
            note = request.note
        )
    }
}