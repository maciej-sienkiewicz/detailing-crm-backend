package com.carslab.crm.modules.email.application.queries.models

import com.carslab.crm.infrastructure.cqrs.Query
import com.carslab.crm.api.model.response.PaginatedResponse

data class GetEmailHistoryQuery(
    val protocolId: String? = null,
    val page: Int = 0,
    val size: Int = 10
) : Query<PaginatedResponse<EmailHistoryResponse>>