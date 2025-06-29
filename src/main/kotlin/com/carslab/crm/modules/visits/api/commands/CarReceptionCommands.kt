package com.carslab.crm.modules.visits.api.commands

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.modules.visits.api.request.ApiDiscountType
import com.carslab.crm.modules.visits.api.request.ApiReferralSource
import com.carslab.crm.modules.visits.api.request.ServiceApprovalStatus
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class CreateCarReceptionCommand(
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

    @JsonProperty("vehicle_images")
    val vehicleImages: List<CreateVehicleImageCommand>? = null
)

/**
 * Komenda dla aktualizacji istniejącego protokołu przyjęcia samochodu.
 * Zawiera identyfikator protokołu, który jest wymagany do aktualizacji.
 */
data class UpdateCarReceptionCommand(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("title")
    val title: String,

    @JsonProperty("calendar_color_id")
    val calendarColorId: String,

    @JsonProperty("start_date")
    val startDate: String,

    @JsonProperty("end_date")
    val endDate: String? = null,

    @JsonProperty("license_plate")
    val licensePlate: String,

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

    @JsonProperty("email")
    val email: String? = null,

    @JsonProperty("phone")
    val phone: String? = null,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("selected_services")
    val selectedServices: List<UpdateServiceCommand>? = null,

    @JsonProperty("status")
    val status: ApiProtocolStatus? = ApiProtocolStatus.SCHEDULED,

    @JsonProperty("referral_source")
    val referralSource: ApiReferralSource? = null,

    @JsonProperty("other_source_details")
    val otherSourceDetails: String? = null,

    @JsonProperty("appointment_id")
    val appointmentId: String? = null,

    @JsonProperty("vehicle_images")
    val vehicleImages: List<UpdateVehicleImageCommand>? = null
)

/**
 * Komenda do tworzenia nowej usługi.
 */
data class CreateServiceCommand(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("price")
    val price: Double,

    @JsonProperty("quantity")
    val quantity: Long = 1L, // DODANA DOMYŚLNA WARTOŚĆ!

    @JsonProperty("discount_type")
    val discountType: ApiDiscountType? = null,

    @JsonProperty("discount_value")
    val discountValue: Double? = null,

    @JsonProperty("final_price")
    val finalPrice: Double? = null,

    @JsonProperty("approval_status")
    val approvalStatus: ServiceApprovalStatus? = ServiceApprovalStatus.PENDING,

    @JsonProperty("note")
    val note: String? = null
)

class ServicesUpdateCommand {

    var services: List<CreateServiceCommand> = emptyList()

    constructor()
}

/**
 * Komenda do aktualizacji istniejącej usługi.
 */
data class UpdateServiceCommand(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("quantity")
    val quantity: Long,

    @JsonProperty("price")
    val price: Double,

    @JsonProperty("discount_type")
    val discountType: ApiDiscountType? = null,

    @JsonProperty("discount_value")
    val discountValue: Double? = null,

    @JsonProperty("final_price")
    val finalPrice: Double? = null,

    @JsonProperty("approval_status")
    val approvalStatus: ServiceApprovalStatus? = ServiceApprovalStatus.PENDING,

    @JsonProperty("note")
    val note: String?
)

/**
 * Komenda do tworzenia nowego obrazu pojazdu.
 */
data class CreateVehicleImageCommand(
    @JsonProperty("name")
    val name: String? = null,

    @JsonProperty("size")
    val size: Long? = null,

    @JsonProperty("type")
    val type: String? = null,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("location")
    val location: String? = null,

    @JsonProperty("has_file")
    val hasFile: Boolean = false,

    @JsonProperty("tags")
    val tags: List<String> = emptyList()
)

/**
 * Komenda do aktualizacji istniejącego obrazu pojazdu.
 */
data class UpdateVehicleImageCommand(
    @JsonProperty("name")
    val name: String? = null,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("location")
    val location: String? = null,

    @JsonProperty("tags")
    val tags: List<String> = emptyList()
)
/**
 * DTO dla szybkiego widoku listy protokołów.
 */
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

/**
 * DTO dla szczegółowego widoku protokołu.
 */
data class CarReceptionDetailDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("title")
    val title: String,

    @JsonProperty("calendar_color_id")
    val calendarColorId: String,

    @JsonProperty("start_date")
    val startDate: String,

    @JsonProperty("end_date")
    val endDate: String,

    @JsonProperty("license_plate")
    val licensePlate: String,

    @JsonProperty("make")
    val make: String,

    @JsonProperty("model")
    val model: String,

    @JsonProperty("production_year")
    val productionYear: Int,

    @JsonProperty("mileage")
    val mileage: Long? = null,

    @JsonProperty("vin")
    val vin: String? = null,

    @JsonProperty("color")
    val color: String? = null,

    @JsonProperty("keys_provided")
    val keysProvided: Boolean,

    @JsonProperty("documents_provided")
    val documentsProvided: Boolean,

    @JsonProperty("owner_id")
    val ownerId: Long? = null,

    @JsonProperty("owner_name")
    val ownerName: String,

    @JsonProperty("company_name")
    val companyName: String? = null,

    @JsonProperty("tax_id")
    val taxId: String? = null,

    @JsonProperty("email")
    val email: String? = null,

    @JsonProperty("phone")
    val phone: String? = null,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("selected_services")
    val selectedServices: List<ServiceDto>,

    @JsonProperty("status")
    val status: ApiProtocolStatus,

    @JsonProperty("referral_source")
    val referralSource: ApiReferralSource? = null,

    @JsonProperty("other_source_details")
    val otherSourceDetails: String? = null,

    @JsonProperty("vehicle_images")
    val vehicleImages: List<VehicleImageDto>? = null,

    @JsonProperty("created_at")
    val createdAt: String,

    @JsonProperty("updated_at")
    val updatedAt: String,

    @JsonProperty("status_updated_at")
    val statusUpdatedAt: String,

    @JsonProperty("appointment_id")
    val appointmentId: String? = null
)

/**
 * DTO dla podstawowych informacji o protokole.
 */
data class CarReceptionBasicDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("created_at")
    val createdAt: String,

    @JsonProperty("updated_at")
    val updatedAt: String,

    @JsonProperty("status_updated_at")
    val statusUpdatedAt: String,

    @JsonProperty("status")
    val status: ApiProtocolStatus
)
/**
 * DTO dla historii protokołów klienta.
 */
data class ClientProtocolHistoryDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("start_date")
    val startDate: String,

    @JsonProperty("end_date")
    val endDate: String,

    @JsonProperty("status")
    val status: ApiProtocolStatus,

    @JsonProperty("make")
    val carMake: String,

    @JsonProperty("model")
    val carModel: String,

    @JsonProperty("license_plate")
    val licensePlate: String,

    @JsonProperty("total_amount")
    val totalAmount: Double
)

/**
 * DTO dla usługi w protokole.
 */
data class ServiceDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("quantity")
    val quantity: Long,

    @JsonProperty("price")
    val price: Double,

    @JsonProperty("discount_type")
    val discountType: ApiDiscountType? = null,

    @JsonProperty("discount_value")
    val discountValue: Double = 0.0,

    @JsonProperty("final_price")
    val finalPrice: Double,

    @JsonProperty("approval_status")
    val approvalStatus: ServiceApprovalStatus? = null,

    @JsonProperty("note")
    val note: String? = ""
)

/**
 * DTO dla obrazu pojazdu.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VehicleImageDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("size")
    val size: Long,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("storage_id")
    val storageId: String,

    @JsonProperty("created_at")
    val createdAt: Instant,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("location")
    val location: String? = null,

    @JsonProperty("tags")
    val tags: List<String> = emptyList()
)

/**
 * DTO dla podstawowych informacji o pojeździe.
 */
data class VehicleBasicDto(
    @JsonProperty("make")
    val make: String,

    @JsonProperty("model")
    val model: String,

    @JsonProperty("license_plate")
    val licensePlate: String,

    @JsonProperty("production_year")
    val productionYear: Int,

    @JsonProperty("color")
    val color: String? = null
)

/**
 * DTO dla podstawowych informacji o właścicielu.
 */
data class OwnerBasicDto(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("company_name")
    val companyName: String? = null
)

/**
 * DTO dla informacji o okresie.
 */
data class PeriodDto(
    @JsonProperty("start_date")
    val startDate: String,

    @JsonProperty("end_date")
    val endDate: String
)

/**
 * Komenda do aktualizacji statusu protokołu.
 */
data class UpdateStatusCommand(
    @JsonProperty("status")
    val status: ApiProtocolStatus
)