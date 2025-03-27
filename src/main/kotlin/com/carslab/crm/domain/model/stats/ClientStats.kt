package com.carslab.crm.domain.model.stats

import java.math.BigDecimal

data class ClientStats(val clientId: Long, val visitNo: Long, val gmv: BigDecimal, val vehiclesNo: Long)