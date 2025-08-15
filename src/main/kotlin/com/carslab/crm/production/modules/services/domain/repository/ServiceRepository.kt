package com.carslab.crm.production.modules.services.domain.repository

import com.carslab.crm.production.modules.services.domain.model.Service
import com.carslab.crm.production.modules.services.domain.model.ServiceId

interface ServiceRepository {
    fun save(service: Service): Service
    fun findById(id: ServiceId): Service?
    fun findActiveById(id: ServiceId): Service?
    fun findActiveByCompanyId(companyId: Long): List<Service>
    fun existsByCompanyIdAndName(companyId: Long, name: String): Boolean
    fun deleteById(id: ServiceId): Boolean
    fun deactivateById(id: ServiceId)
}