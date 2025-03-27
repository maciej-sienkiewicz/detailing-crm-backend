package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class ClientVehicleResponse(
    @JsonProperty("totalVisits")
    val totalVisits: Long,

    @JsonProperty("totalRevenue")
    val totalRevenue: BigDecimal,

    @JsonProperty("vehicleNo")
    val vehicleNo: Long
)