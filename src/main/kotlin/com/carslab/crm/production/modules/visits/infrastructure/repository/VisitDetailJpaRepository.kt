package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.visits.domain.model.VisitId
import com.carslab.crm.production.modules.visits.domain.repository.VisitDetailRepository
import com.carslab.crm.production.modules.visits.domain.repository.VisitDetailProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface VisitDetailJpaRepository : JpaRepository<com.carslab.crm.production.modules.visits.infrastructure.entity.VisitEntity, Long> {

    @Query("""
        SELECT new com.carslab.crm.production.modules.visits.domain.repository.VisitDetailProjection(
            v.id,
            v.title,
            v.calendarColorId,
            v.startDate,
            v.endDate,
            CAST(v.status AS string),
            v.notes,
            CAST(v.referralSource AS string),
            v.appointmentId,
            v.keysProvided,
            v.documentsProvided,
            v.createdAt,
            v.updatedAt,
            c.id,
            CONCAT(c.firstName, ' ', c.lastName),
            c.email,
            c.phone,
            c.address,
            c.company,
            c.taxId,
            vh.id,
            vh.make,
            vh.model,
            vh.licensePlate,
            vh.year,
            vh.mileage,
            vh.vin,
            vh.color
        )
        FROM VisitEntity v
        JOIN ClientEntity c ON c.id = v.clientId AND c.companyId = v.companyId
        JOIN VehicleEntity vh ON vh.id = v.vehicleId AND vh.companyId = v.companyId
        WHERE v.id = :visitId AND v.companyId = :companyId
    """)
    fun findVisitDetailWithRelations(
        @Param("visitId") visitId: Long,
        @Param("companyId") companyId: Long
    ): VisitDetailProjection?
}

@Repository
@Transactional(readOnly = true)
class JpaVisitDetailRepositoryImpl(
    private val visitDetailJpaRepository: VisitDetailJpaRepository
) : VisitDetailRepository {

    override fun findVisitDetailWithRelations(visitId: VisitId, companyId: Long): VisitDetailProjection? {
        return visitDetailJpaRepository.findVisitDetailWithRelations(visitId.value, companyId)
    }
}