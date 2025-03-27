package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class VehicleStatisticsResponse(
    @JsonProperty("servicesNo")
    val servicesNo: Long,

    @JsonProperty("totalRevenue")
    val totalRevenue: BigDecimal,
)