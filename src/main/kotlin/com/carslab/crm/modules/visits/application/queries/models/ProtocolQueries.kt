package com.carslab.crm.modules.visits.application.queries.models

import com.carslab.crm.infrastructure.cqrs.Query
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.api.model.response.PaginatedResponse
import java.time.LocalDateTime

data class GetProtocolByIdQuery(
    val protocolId: String
) : Query<ProtocolDetailReadModel?>

data class SearchProtocolsQuery(
    val clientName: String? = null,
    val clientId: Long? = null,
    val licensePlate: String? = null,
    val make: String? = null,
    val status: ProtocolStatus? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val page: Int = 0,
    val size: Int = 10
) : Query<PaginatedResponse<ProtocolListReadModel>>

data class GetProtocolCountersQuery(
    val userId: String? = null
) : Query<ProtocolCountersReadModel>

data class GetClientProtocolHistoryQuery(
    val clientId: Long,
    val status: ProtocolStatus? = null,
    val page: Int = 0,
    val size: Int = 10,
    val sortBy: String = "startDate",
    val sortDirection: String = "DESC"
) : Query<PaginatedResponse<ProtocolListReadModel>>