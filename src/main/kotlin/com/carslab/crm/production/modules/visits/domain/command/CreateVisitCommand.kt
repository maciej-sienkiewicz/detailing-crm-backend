package com.carslab.crm.production.modules.visits.domain.command

import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleResponse
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.enums.CommentType
import com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType
import com.carslab.crm.production.modules.visits.domain.models.enums.DocumentType
import com.carslab.crm.production.modules.visits.domain.models.enums.ReferralSource
import com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.MediaMetadata
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import java.time.LocalDateTime

data class CreateVisitCommand(
    val companyId: Long,
    val title: String,
    val client: ClientResponse,
    val vehicle: VehicleResponse,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val status: VisitStatus = VisitStatus.SCHEDULED,
    val services: List<CreateServiceCommand> = emptyList(),
    val notes: String? = null,
    val referralSource: ReferralSource? = null,
    val appointmentId: String? = null,
    val calendarColorId: String,
    val keysProvided: Boolean = false,
    val documentsProvided: Boolean = false,
    val deliveryPerson: DeliveryPerson? = null,
)

data class DeliveryPerson(
    val id: String? = null,
    val name: String,
    val phone: String
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
    val documentsProvided: Boolean = false,
    val status: VisitStatus,
    val deliveryPerson: DeliveryPerson? = null,
    val sendWithEmail: Boolean? = null,
)

data class CreateServiceCommand(
    val id: String,
    val name: String,
    val basePrice: PriceValueObject,
    val quantity: Long = 1,
    val discountType: DiscountType? = null,
    val discountValue: java.math.BigDecimal? = null,
    val approvalStatus: ServiceApprovalStatus = ServiceApprovalStatus.PENDING,
    val note: String? = null
)

data class UpdateServiceCommand(
    val id: String,
    val name: String,
    val basePrice: PriceValueObject,
    val quantity: Long,
    val discountType: DiscountType? = null,
    val discountValue: java.math.BigDecimal? = null,
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
    val vehicleId: VehicleId? = null,
    val file: org.springframework.web.multipart.MultipartFile,
    val metadata: MediaMetadata,
)

data class UploadDocumentCommand(
    val visitId: VisitId,
    val file: org.springframework.web.multipart.MultipartFile,
    val documentType: DocumentType,
    val description: String? = null
)