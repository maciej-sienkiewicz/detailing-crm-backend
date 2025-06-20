package com.carslab.crm.modules.email.infrastructure.persistence.read

import com.carslab.crm.modules.email.application.queries.models.EmailHistoryResponse
import com.carslab.crm.api.model.response.PaginatedResponse

interface EmailReadRepository {
    fun getEmailHistory(
        protocolId: String? = null,
        page: Int = 0,
        size: Int = 10
    ): PaginatedResponse<EmailHistoryResponse>
}
