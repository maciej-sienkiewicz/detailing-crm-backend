package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.production.modules.visits.application.dto.VisitListFilterRequest
import com.carslab.crm.production.modules.visits.application.queries.models.VisitSearchCriteria
import org.springframework.stereotype.Component

@Component
class VisitListCriteriaBuilder {

    fun buildCriteria(companyId: Long, filter: VisitListFilterRequest): VisitSearchCriteria {
        return VisitSearchCriteria(
            companyId = companyId,
            clientName = filter.clientName,
            licensePlate = filter.licensePlate,
            status = filter.status,
            startDate = filter.startDate,
            endDate = filter.endDate,
            make = filter.make,
            model = filter.model,
            serviceName = filter.serviceName,
            serviceIds = filter.serviceIds,
            title = filter.title,
            minPrice = filter.minPrice,
            maxPrice = filter.maxPrice
        )
    }
}