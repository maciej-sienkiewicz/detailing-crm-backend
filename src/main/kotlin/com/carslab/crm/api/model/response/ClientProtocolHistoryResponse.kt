package com.carslab.crm.api.model.response

import com.carslab.crm.api.model.ApiProtocolStatus
import com.fasterxml.jackson.annotation.JsonProperty

class ClientProtocolHistoryResponse(
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