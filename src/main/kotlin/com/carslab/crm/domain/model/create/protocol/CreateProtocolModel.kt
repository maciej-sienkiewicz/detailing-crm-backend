package com.carslab.crm.domain.model.create.protocol

import com.carslab.crm.domain.model.*

data class CreateProtocolRootModel(
    val id: ProtocolId,
    val title: String,
    val vehicle: CreateProtocolVehicleModel,
    val client: CreateProtocolClientModel,
    val period: ServicePeriod,
    val status: ProtocolStatus,
    val services: List<CreateServiceModel>,
    val notes: String? = null,
    val referralSource: ReferralSource? = null,
    val otherSourceDetails: String? = null,
    val documents: Documents = Documents(),
    val mediaItems: List<CreateMediaTypeModel> = emptyList(),
    val audit: AuditInfo
)

data class CreateServiceModel(
    val name: String,
    val basePrice: Money,
    val discount: Discount?,
    val finalPrice: Money,
    val approvalStatus: ApprovalStatus,
    val note: String?,
)

data class CreateProtocolVehicleModel(
    val id: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val licensePlate: String? = null,
    val productionYear: Int? = null,
    val vin: String? = null,
    val color: String? = null,
    val mileage: Int? = null
)

data class CreateProtocolClientModel(
    val id: String? = null,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val companyName: String? = null,
    val taxId: String? = null
)