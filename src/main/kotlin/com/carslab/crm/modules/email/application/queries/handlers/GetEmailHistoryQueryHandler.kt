package com.carslab.crm.modules.email.application.queries.handlers

import com.carslab.crm.modules.email.application.queries.models.GetEmailHistoryQuery
import com.carslab.crm.modules.email.application.queries.models.EmailHistoryResponse
import com.carslab.crm.modules.email.infrastructure.persistence.read.EmailReadRepository
import com.carslab.crm.infrastructure.cqrs.QueryHandler
import com.carslab.crm.api.model.response.PaginatedResponse
import org.springframework.stereotype.Service

@Service
class GetEmailHistoryQueryHandler(
    private val emailReadRepository: EmailReadRepository
) : QueryHandler<GetEmailHistoryQuery, PaginatedResponse<EmailHistoryResponse>> {

    override fun handle(query: GetEmailHistoryQuery): PaginatedResponse<EmailHistoryResponse> {
        return emailReadRepository.getEmailHistory(
            protocolId = query.protocolId,
            page = query.page,
            size = query.size
        )
    }
}