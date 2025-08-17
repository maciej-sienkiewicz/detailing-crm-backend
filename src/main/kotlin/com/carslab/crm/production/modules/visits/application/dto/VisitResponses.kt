package com.carslab.crm.production.modules.visits.application.dto

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.modules.visits.api.commands.OwnerBasicDto
import com.carslab.crm.modules.visits.api.commands.PeriodDto
import com.carslab.crm.modules.visits.api.commands.ServiceDto
import com.carslab.crm.modules.visits.api.commands.VehicleBasicDto
import com.carslab.crm.production.modules.visits.domain.model.*
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitComment
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitDocument
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitMedia
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.domain.models.enums.CommentType
import com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType
import com.carslab.crm.production.modules.visits.domain.models.enums.DocumentType
import com.carslab.crm.production.modules.visits.domain.models.enums.MediaType
import com.carslab.crm.production.modules.visits.domain.models.enums.ReferralSource
import com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class VisitResponse(
    val id: String,
    val title: String,
    @JsonProperty("client_id")
    val clientId: String,
    @JsonProperty("vehicle_id")
    val vehicleId: String,
    @JsonProperty("start_date")
    val startDate: LocalDateTime,
    @JsonProperty("end_date")
    val endDate: LocalDateTime,
    val status: VisitStatus,
    val services: List<VisitServiceResponse>,
    @JsonProperty("total_amount")
    val totalAmount: BigDecimal,
    @JsonProperty("service_count")
    val serviceCount: Int,
    val notes: String?,
    @JsonProperty("referral_source")
    val referralSource: ReferralSource?,
    @JsonProperty("appointment_id")
    val appointmentId: String?,
    @JsonProperty("calendar_color_id")
    val calendarColorId: String,
    @JsonProperty("keys_provided")
    val keysProvided: Boolean,
    @JsonProperty("documents_provided")
    val documentsProvided: Boolean,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(visit: Visit): VisitResponse {
            return VisitResponse(
                id = visit.id?.value?.toString() ?: "",
                title = visit.title,
                clientId = visit.clientId.value.toString(),
                vehicleId = visit.vehicleId.value.toString(),
                startDate = visit.period.startDate,
                endDate = visit.period.endDate,
                status = visit.status,
                services = visit.services.map { VisitServiceResponse.from(it) },
                totalAmount = visit.totalAmount(),
                serviceCount = visit.serviceCount(),
                notes = visit.notes,
                referralSource = visit.referralSource,
                appointmentId = visit.appointmentId,
                calendarColorId = visit.calendarColorId,
                keysProvided = visit.documents.keysProvided,
                documentsProvided = visit.documents.documentsProvided,
                createdAt = visit.createdAt,
                updatedAt = visit.updatedAt
            )
        }
    }
}

data class CarReceptionListDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("title")
    val title: String,

    @JsonProperty("vehicle")
    val vehicle: VehicleBasicDto,

    @JsonProperty("calendar_color_id")
    val calendarColorId: String,

    @JsonProperty("period")
    val period: PeriodDto,

    @JsonProperty("owner")
    val owner: OwnerBasicDto,

    @JsonProperty("status")
    val status: ApiProtocolStatus,

    @JsonProperty("total_service_count")
    val totalServiceCount: Int,

    @JsonProperty("total_amount")
    val totalAmount: Double,

    @JsonProperty("selected_services")
    val selectedServices: List<ServiceDto>,

    @JsonProperty("last_update")
    val lastUpdate: String
)

data class VisitServiceResponse(
    val id: String,
    val name: String,
    @JsonProperty("base_price")
    val basePrice: BigDecimal,
    val quantity: Long,
    val discount: ServiceDiscountResponse?,
    @JsonProperty("final_price")
    val finalPrice: BigDecimal,
    @JsonProperty("approval_status")
    val approvalStatus: ServiceApprovalStatus,
    val note: String?
) {
    companion object {
        fun from(service: VisitService): VisitServiceResponse {
            return VisitServiceResponse(
                id = service.id,
                name = service.name,
                basePrice = service.basePrice,
                quantity = service.quantity,
                discount = service.discount?.let { ServiceDiscountResponse.from(it) },
                finalPrice = service.finalPrice,
                approvalStatus = service.approvalStatus,
                note = service.note
            )
        }
    }
}

data class ServiceDiscountResponse(
    val type: DiscountType,
    val value: BigDecimal
) {
    companion object {
        fun from(discount: ServiceDiscount): ServiceDiscountResponse {
            return ServiceDiscountResponse(
                type = discount.type,
                value = discount.value
            )
        }
    }
}

data class VisitCommentResponse(
    val id: String,
    @JsonProperty("visit_id")
    val visitId: String,
    val author: String,
    val content: String,
    val type: CommentType,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(comment: VisitComment): VisitCommentResponse {
            return VisitCommentResponse(
                id = comment.id ?: "",
                visitId = comment.visitId.value.toString(),
                author = comment.author,
                content = comment.content,
                type = comment.type,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt
            )
        }
    }
}

data class VisitMediaResponse(
    val id: String,
    val name: String,
    val description: String?,
    val location: String?,
    val tags: List<String>,
    val type: MediaType,
    val size: Long,
    @JsonProperty("content_type")
    val contentType: String,
    @JsonProperty("download_url")
    val downloadUrl: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(media: VisitMedia): VisitMediaResponse {
            return VisitMediaResponse(
                id = media.id,
                name = media.name,
                description = media.description,
                location = media.location,
                tags = media.tags,
                type = media.type,
                size = media.size,
                contentType = media.contentType,
                downloadUrl = "/api/v1/visits/media/${media.id}/download",
                createdAt = media.createdAt,
                updatedAt = media.updatedAt
            )
        }
    }
}

data class VisitDocumentResponse(
    val id: String,
    @JsonProperty("visit_id")
    val visitId: String,
    val name: String,
    val type: DocumentType,
    val size: Long,
    @JsonProperty("content_type")
    val contentType: String,
    val description: String?,
    @JsonProperty("download_url")
    val downloadUrl: String,
    @JsonProperty("uploaded_by")
    val uploadedBy: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(document: VisitDocument): VisitDocumentResponse {
            return VisitDocumentResponse(
                id = document.id,
                visitId = document.visitId.value.toString(),
                name = document.name,
                type = document.type,
                size = document.size,
                contentType = document.contentType,
                description = document.description,
                downloadUrl = "/api/v1/visits/documents/${document.id}/download",
                uploadedBy = document.uploadedBy,
                createdAt = document.createdAt
            )
        }
    }
}

data class VisitCountersResponse(
    val scheduled: Long,
    @JsonProperty("in_progress")
    val inProgress: Long,
    @JsonProperty("ready_for_pickup")
    val readyForPickup: Long,
    val completed: Long,
    val cancelled: Long,
    val all: Long
)

data class VisitTableResponse(
    val id: Long,
    val title: String,
    @JsonProperty("client_name")
    val clientName: String,
    @JsonProperty("vehicle_info")
    val vehicleInfo: String,
    @JsonProperty("license_plate")
    val licensePlate: String,
    @JsonProperty("start_date")
    val startDate: String,
    @JsonProperty("end_date")
    val endDate: String,
    val status: VisitStatus,
    @JsonProperty("service_count")
    val serviceCount: Int,
    @JsonProperty("total_amount")
    val totalAmount: BigDecimal,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("updated_at")
    val updatedAt: String
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

        fun from(visit: Visit, clientName: String, vehicleInfo: String, licensePlate: String): VisitTableResponse {
            return VisitTableResponse(
                id = visit.id?.value ?: 0L,
                title = visit.title,
                clientName = clientName,
                vehicleInfo = vehicleInfo,
                licensePlate = licensePlate,
                startDate = visit.period.startDate.format(dateFormatter),
                endDate = visit.period.endDate.format(dateFormatter),
                status = visit.status,
                serviceCount = visit.serviceCount(),
                totalAmount = visit.totalAmount(),
                createdAt = visit.createdAt.format(dateFormatter),
                updatedAt = visit.updatedAt.format(dateFormatter)
            )
        }
    }
}