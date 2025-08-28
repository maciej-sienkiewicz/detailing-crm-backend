package com.carslab.crm.production.modules.visits.domain.service.aggregate

import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import org.springframework.stereotype.Service

@Service
class VisitGetService(
    private val visitRepository: VisitRepository
) {
}