package com.carslab.crm.production.modules.visits.domain.repositories

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface VisitRepository {
    fun save(visit: Visit): Visit
    fun findById(visitId: VisitId, companyId: Long): Visit?
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Visit>
    fun existsById(visitId: VisitId, companyId: Long): Boolean
    fun deleteById(visitId: VisitId, companyId: Long): Boolean
    fun countByStatus(companyId: Long, status: VisitStatus): Long
    fun findByClientId(clientId: ClientId, companyId: Long, pageable: Pageable): Page<Visit>
    fun findByVehicleId(vehicleId: VehicleId, companyId: Long, pageable: Pageable): Page<Visit>
}