package com.carslab.crm.production.modules.visits.domain.command

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.model.*
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateVisitCommand(
    val companyId: Long,
    val title: String,
    val clientId: ClientId,
    val vehicleId: VehicleId,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val status: VisitStatus = VisitStatus.SCHEDULED,
    val services: List<CreateServiceCommand> = emptyList(),
    val notes: String? = null,
    val referralSource: ReferralSource? = null,
    val appointmentId: String? = null,
    val calendarColorId: String,
    val keysProvided: Boolean = false,
    val documentsProvided: Boolean = false
)

data class UpdateVisitCommand(
    val title: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val services: List<UpdateServiceCommand> = emptyList(),
    val notes: String? = null,
    val referralSource: ReferralSource? = null,
    val appointmentId: String? = null,
    val calendarColorId: String,
    val keysProvided: Boolean = false,
    val documentsProvided: Boolean = false
)

data class CreateServiceCommand(
    val name: String,
    val basePrice: BigDecimal,
    val quantity: Long = 1,
    val discountType: DiscountType? = null,
    val discountValue: BigDecimal? = null,
    val finalPrice: BigDecimal? = null,
    val approvalStatus: ServiceApprovalStatus = ServiceApprovalStatus.PENDING,
    val note: String? = null
)

data class UpdateServiceCommand(
    val id: String,
    val name: String,
    val basePrice: BigDecimal,
    val quantity: Long,
    val discountType: DiscountType? = null,
    val discountValue: BigDecimal? = null,
    val finalPrice: BigDecimal? = null,
    val approvalStatus: ServiceApprovalStatus = ServiceApprovalStatus.PENDING,
    val note: String? = null
)

data class ChangeVisitStatusCommand(
    val visitId: VisitId,
    val newStatus: VisitStatus,
    val reason: String? = null,
    val companyId: Long
)

data class AddCommentCommand(
    val visitId: VisitId,
    val content: String,
    val type: CommentType = CommentType.INTERNAL,
    val author: String
)

data class UploadMediaCommand(
    val visitId: VisitId,
    val file: org.springframework.web.multipart.MultipartFile,
    val metadata: MediaMetadata
)

data class UploadDocumentCommand(
    val visitId: VisitId,
    val file: org.springframework.web.multipart.MultipartFile,
    val documentType: DocumentType,
    val description: String? = null
)