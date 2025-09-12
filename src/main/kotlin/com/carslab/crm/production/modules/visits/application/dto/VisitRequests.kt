package com.carslab.crm.production.modules.visits.application.dto

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.modules.visits.api.commands.CreateServiceCommand
import com.carslab.crm.modules.visits.api.request.ApiReferralSource
import com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType
import com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateVisitRequest(
    @JsonProperty("title")
    val title: String,

    @JsonProperty("calendar_color_id")
    val calendarColorId: String,

    @JsonProperty("start_date")
    val startDate: String,

    @JsonProperty("end_date")
    val endDate: String? = null,

    @JsonProperty("license_plate")
    val licensePlate: String? = null,

    @JsonProperty("make")
    val make: String,

    @JsonProperty("model")
    val model: String,

    @JsonProperty("production_year")
    val productionYear: Int? = null,

    @JsonProperty("mileage")
    val mileage: Long? = null,

    @JsonProperty("vin")
    val vin: String? = null,

    @JsonProperty("color")
    val color: String? = null,

    @JsonProperty("keys_provided")
    val keysProvided: Boolean? = false,

    @JsonProperty("documents_provided")
    val documentsProvided: Boolean? = false,

    @JsonProperty("owner_id")
    val ownerId: Long? = null,

    @JsonProperty("owner_name")
    val ownerName: String,

    @JsonProperty("company_name")
    val companyName: String? = null,

    @JsonProperty("tax_id")
    val taxId: String? = null,

    val address: String? = null,

    @JsonProperty("email")
    val email: String? = null,

    @JsonProperty("phone")
    val phone: String? = null,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("selected_services")
    val selectedServices: List<CreateServiceCommand>? = null,

    @JsonProperty("status")
    val status: ApiProtocolStatus? = ApiProtocolStatus.SCHEDULED,

    @JsonProperty("referral_source")
    val referralSource: ApiReferralSource? = null,

    @JsonProperty("other_source_details")
    val otherSourceDetails: String? = null,

    @JsonProperty("appointment_id")
    val appointmentId: String? = null,
    
    @JsonProperty("delivery_person")
    val deliveryPerson: DeliveryPerson? = null,

    @JsonProperty("is_recurring")
    val isRecurring: Boolean = false,

    @JsonProperty("recurrence_pattern")
    val recurrencePattern: com.carslab.crm.production.modules.visits.application.dto.RecurrencePatternRequest? = null
)

data class DeliveryPerson(
    val id: String? = null,
    val name: String,
    val phone: String
)

data class UpdateVisitRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200, message = "Title cannot exceed 200 characters")
    val title: String,

    @field:NotNull(message = "Start date is required")
    @JsonProperty("start_date")
    val startDate: LocalDateTime,

    @field:NotNull(message = "End date is required")
    @JsonProperty("end_date")
    val endDate: LocalDateTime,

    val services: List<UpdateServiceRequest> = emptyList(),

    @field:Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    val notes: String? = null,

    @JsonProperty("referral_source")
    val referralSource: String? = null,

    @JsonProperty("appointment_id")
    val appointmentId: String? = null,

    @field:NotBlank(message = "Calendar color ID is required")
    @JsonProperty("calendar_color_id")
    val calendarColorId: String,

    @JsonProperty("keys_provided")
    val keysProvided: Boolean = false,

    @JsonProperty("documents_provided")
    val documentsProvided: Boolean = false,
    
    val status: String
)

data class UpdateServiceRequest(
    @field:NotBlank(message = "Service ID is required")
    val id: String,

    @field:NotBlank(message = "Service name is required")
    @field:Size(max = 100, message = "Service name cannot exceed 100 characters")
    val name: String,

    @field:NotNull(message = "Base price is required")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Base price must be positive")
    @JsonProperty("base_price")
    val basePrice: BigDecimal,

    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Long,

    @JsonProperty("discount_type")
    val discountType: DiscountType? = null,

    @JsonProperty("discount_value")
    val discountValue: BigDecimal? = null,

    @JsonProperty("final_price")
    val finalPrice: BigDecimal? = null,

    @JsonProperty("approval_status")
    val approvalStatus: ServiceApprovalStatus = ServiceApprovalStatus.PENDING,

    @field:Size(max = 500, message = "Note cannot exceed 500 characters")
    val note: String? = null
)

data class ChangeStatusRequest(
    @field:NotNull(message = "Status is required")
    val status: VisitStatus,

    @field:Size(max = 500, message = "Reason cannot exceed 500 characters")
    val reason: String? = null
)

data class AddCommentRequest(
    @field:NotBlank(message = "Content is required")
    @field:Size(max = 2000, message = "Content cannot exceed 2000 characters")
    val content: String,

    val type: String,

    @JsonProperty("protocolId")
    val visitId: String
)

data class UploadMediaRequest(
    @field:NotNull(message = "File is required")
    val file: MultipartFile,

    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name cannot exceed 255 characters")
    val name: String,

    @field:Size(max = 1000, message = "Description cannot exceed 1000 characters")
    val description: String? = null,

    @field:Size(max = 100, message = "Location cannot exceed 100 characters")
    val location: String? = null,

    val tags: List<String> = emptyList()
)

data class UploadDocumentRequest(
    @field:NotNull(message = "File is required")
    val file: MultipartFile,

    @field:NotNull(message = "Document type is required")
    @JsonProperty("document_type")
    val documentType: String,

    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String? = null
)