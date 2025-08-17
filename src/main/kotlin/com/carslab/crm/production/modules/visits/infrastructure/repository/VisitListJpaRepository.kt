package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitEntity
import com.carslab.crm.production.modules.visits.infrastructure.projection.VisitListProjection
import com.carslab.crm.production.modules.visits.infrastructure.projection.VisitServiceProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VisitListJpaRepository : JpaRepository<VisitEntity, Long> {

    @Query("""
        SELECT 
            v.id as visitId,
            v.title as title,
            CONCAT(c.firstName, ' ', c.lastName) as clientName,
            c.company as companyName,
            vh.make as vehicleMake,
            vh.model as vehicleModel,
            vh.licensePlate as licensePlate,
            vh.year as productionYear,
            vh.color as color,
            v.startDate as startDate,
            v.endDate as endDate,
            v.status as status,
            COALESCE(COUNT(vs.id), 0) as totalServiceCount,
            COALESCE(SUM(vs.finalPrice), 0) as totalAmount,
            v.calendarColorId as calendarColorId,
            v.updatedAt as lastUpdate
        FROM VisitEntity v
        JOIN ClientEntity c ON c.id = v.clientId AND c.companyId = v.companyId
        JOIN VehicleEntity vh ON vh.id = v.vehicleId AND vh.companyId = v.companyId
        LEFT JOIN VisitServiceEntity vs ON vs.visitId = v.id
        WHERE v.companyId = :companyId
        GROUP BY v.id, v.title, c.firstName, c.lastName, c.company, vh.make, vh.model, 
                 vh.licensePlate, vh.year, vh.color, v.startDate, v.endDate, v.status, 
                 v.calendarColorId, v.updatedAt
        ORDER BY v.updatedAt DESC
    """)
    fun findVisitListForCompany(
        @Param("companyId") companyId: Long,
        pageable: Pageable
    ): Page<VisitListProjection>

    @Query("""
        SELECT 
            vs.visitId as visitId,
            vs.id as serviceId,
            vs.name as serviceName,
            vs.finalPrice as finalPrice
        FROM VisitServiceEntity vs
        JOIN VisitEntity v ON v.id = vs.visitId
        WHERE v.companyId = :companyId AND vs.visitId IN :visitIds
        ORDER BY vs.visitId, vs.name
    """)
    fun findVisitServicesForVisits(
        @Param("companyId") companyId: Long,
        @Param("visitIds") visitIds: List<Long>
    ): List<VisitServiceProjection>
}