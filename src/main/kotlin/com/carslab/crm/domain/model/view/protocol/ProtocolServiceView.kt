package com.carslab.crm.domain.model.view.protocol

import com.carslab.crm.domain.model.ApprovalStatus
import com.carslab.crm.domain.model.Discount
import com.carslab.crm.domain.model.Money
import com.carslab.crm.infrastructure.repository.ServiceId

data class ProtocolServiceView(
    val id: ServiceId,
    val name: String,
    val basePrice: Money,
    val discount: Discount? = null,
    val finalPrice: Money,
    val approvalStatus: ApprovalStatus,
    val note: String?
)