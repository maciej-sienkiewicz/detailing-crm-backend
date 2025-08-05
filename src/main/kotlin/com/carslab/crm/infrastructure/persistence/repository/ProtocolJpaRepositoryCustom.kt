package com.carslab.crm.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface ProtocolJpaRepositoryCustom {

    @Query(value = """
        SELECT 
            p.id,
            p.title,
            p.status,
            p.calendar_color_id as calendarColorId,
            p.start_date as startDate,
            p.end_date as endDate,
            p.updated_at as updatedAt,
            p.company_id as companyId,
            CONCAT(c.first_name, ' ', c.last_name) as clientFullName,
            c.company as clientCompany,
            c.first_name as clientFirstName,
            c.last_name as clientLastName,
            v.make as vehicleMake,
            v.model as vehicleModel,
            v.license_plate as vehicleLicensePlate,
            v.year as vehicleYear,
            v.color as vehicleColor,
            COALESCE(service_summary.service_count, 0) as serviceCount,
            COALESCE(service_summary.total_amount, 0) as totalAmount,
            service_summary.services_json as servicesJson
        FROM protocols p
        LEFT JOIN clients c ON p.client_id = c.id AND c.company_id = p.company_id
        LEFT JOIN vehicles v ON p.vehicle_id = v.id AND v.company_id = p.company_id
        LEFT JOIN (
            SELECT 
                ps.protocol_id,
                COUNT(*) as service_count,
                SUM(ps.final_price) as total_amount,
                JSON_ARRAYAGG(
                    JSON_OBJECT(
                        'id', ps.id,
                        'name', ps.name,
                        'finalPrice', ps.final_price
                    )
                ) as services_json
            FROM protocol_services ps
            WHERE ps.company_id = :companyId
            GROUP BY ps.protocol_id
        ) service_summary ON p.id = service_summary.protocol_id
        WHERE p.company_id = :companyId
    """,
        countQuery = """
        SELECT COUNT(*)
        FROM protocols p
        WHERE p.company_id = :companyId
    """,
        nativeQuery = true)
    fun findVisitsProjection(@Param("companyId") companyId: Long, pageable: Pageable): Page<ProtocolProjection>
}

interface ProtocolProjection {
    fun getId(): String
    fun getTitle(): String
    fun getStatus(): String
    fun getCalendarColorId(): String
    fun getStartDate(): java.time.LocalDateTime
    fun getEndDate(): java.time.LocalDateTime
    fun getUpdatedAt(): java.time.LocalDateTime
    fun getCompanyId(): Long
    fun getClientFullName(): String?
    fun getClientCompany(): String?
    fun getClientFirstName(): String?
    fun getClientLastName(): String?
    fun getVehicleMake(): String?
    fun getVehicleModel(): String?
    fun getVehicleLicensePlate(): String?
    fun getVehicleYear(): Int?
    fun getVehicleColor(): String?
    fun getServiceCount(): Int
    fun getTotalAmount(): BigDecimal
    fun getServicesJson(): String?
}