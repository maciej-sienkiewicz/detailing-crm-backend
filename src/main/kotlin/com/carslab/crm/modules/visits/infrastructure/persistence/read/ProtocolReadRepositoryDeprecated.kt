package com.carslab.crm.modules.visits.infrastructure.persistence.read

import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.api.model.response.PaginatedResponse
import java.time.LocalDateTime

interface ProtocolReadRepositoryDeprecated {
    fun findDetailById(protocolId: String): ProtocolDetailReadModel?

    fun searchProtocols(
        clientName: String? = null,
        clientId: Long? = null,
        licensePlate: String? = null,
        make: String? = null,
        status: ProtocolStatus? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        page: Int = 0,
        size: Int = 10
    ): PaginatedResponse<ProtocolListReadModel>

    fun getCounters(): ProtocolCountersReadModel

    fun findByClientIdWithPagination(
        clientId: Long,
        status: ProtocolStatus? = null,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "startDate",
        sortDirection: String = "DESC"
    ): PaginatedResponse<ProtocolListReadModel>
}