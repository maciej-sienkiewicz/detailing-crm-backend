package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.infrastructure.persistance.entity.Workstation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WorkstationRepository : JpaRepository<Workstation, UUID> {

    fun findByCompanyId(companyId: Long): List<Workstation>
}
