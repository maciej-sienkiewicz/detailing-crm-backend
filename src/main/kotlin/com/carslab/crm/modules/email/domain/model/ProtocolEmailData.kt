package com.carslab.crm.modules.email.domain.model

data class ProtocolEmailData(
    val protocolId: String,
    val clientName: String,
    val clientEmail: String,
    val companyName: String?,
    val vehicleMake: String,
    val vehicleModel: String,
    val licensePlate: String,
    val servicePeriod: String,
    val status: String,
    val services: List<ProtocolServiceData>,
    val totalAmount: Double,
    val notes: String?
)

data class ProtocolServiceData(
    val name: String,
    val quantity: Long,
    val price: Double,
    val finalPrice: Double
)