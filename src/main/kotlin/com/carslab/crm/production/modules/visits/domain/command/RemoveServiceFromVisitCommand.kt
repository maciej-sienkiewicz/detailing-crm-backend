package com.carslab.crm.production.modules.visits.domain.command

import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId

data class RemoveServiceFromVisitCommand(
    val visitId: VisitId,
    val companyId: Long,
    val serviceId: String,
    val reason: String? = null
)