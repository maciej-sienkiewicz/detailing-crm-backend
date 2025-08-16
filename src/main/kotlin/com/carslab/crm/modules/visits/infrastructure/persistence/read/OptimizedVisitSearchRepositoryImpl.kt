package com.carslab.crm.modules.visits.infrastructure.persistence.read

import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.modules.visits.infrastructure.monitoring.VisitSearchPerformanceMonitor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import jakarta.persistence.EntityManager
import jakarta.persistence.Entity
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Repository
class OptimizedVisitSearchRepositoryImpl(
    private val entityManager: EntityManager,
    private val performanceMonitor: VisitSearchPerformanceMonitor
) : VisitSearchRepositoryDeprecated {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

    override fun searchVisits(query: SearchVisitsQuery): PaginatedResponse<VisitListReadModel> {
        return performanceMonitor.monitorQuery(
            "searchVisits",
            mapOf(
                "companyId" to getCurrentCompanyId(),
                "filters" to query,
                "page" to query.page,
                "size" to query.size
            )
        ) {
            executeSearchQuery(query)
        }
    }

    private fun executeSearchQuery(query: SearchVisitsQuery): PaginatedResponse<VisitListReadModel> {
        val companyId = getCurrentCompanyId()
        val (sql, countSql, parameters) = buildOptimizedQuery(query, companyId)

        val visitQuery = entityManager.createNativeQuery(sql, VisitSearchResult::class.java)
        val countQuery = entityManager.createNativeQuery(countSql)

        parameters.forEach { (key, value) ->
            visitQuery.setParameter(key, value)
            countQuery.setParameter(key, value)
        }

        visitQuery.firstResult = query.page * query.size
        visitQuery.maxResults = query.size

        val visits = visitQuery.resultList as List<VisitSearchResult>
        val totalCount = (countQuery.singleResult as Number).toLong()

        val visitModels = visits.map { result ->
            VisitListReadModel(
                id = result.id,
                title = result.title,
                vehicle = VehicleBasicReadModel(
                    make = result.vehicleMake ?: "",
                    model = result.vehicleModel ?: "",
                    licensePlate = result.vehicleLicensePlate ?: "",
                    productionYear = result.vehicleYear ?: 0,
                    color = result.vehicleColor
                ),
                client = ClientBasicReadModel(
                    name = result.clientFullName ?: "",
                    companyName = result.clientCompany
                ),
                period = PeriodReadModel(
                    startDate = result.startDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    endDate = result.endDate.format(DateTimeFormatter.ISO_DATE_TIME)
                ),
                status = result.status,
                calendarColorId = result.calendarColorId,
                totalServiceCount = result.serviceCount,
                totalAmount = result.totalAmount,
                services = parseServices(result.servicesJson),
                lastUpdate = result.updatedAt.format(dateTimeFormatter)
            )
        }

        return PaginatedResponse(
            data = visitModels,
            page = query.page,
            size = query.size,
            totalItems = totalCount,
            totalPages = (totalCount + query.size - 1) / query.size
        )
    }

    private fun buildOptimizedQuery(query: SearchVisitsQuery, companyId: Long): Triple<String, String, Map<String, Any>> {
        val baseSelect = """
            SELECT 
                p.id,
                p.title,
                p.status,
                p.calendar_color_id as calendar_color_id,
                p.start_date as start_date,
                p.end_date as end_date,
                p.updated_at as updated_at,
                CONCAT(c.first_name, ' ', c.last_name) as client_full_name,
                c.company as client_company,
                v.make as vehicle_make,
                v.model as vehicle_model,
                v.license_plate as vehicle_license_plate,
                v.year as vehicle_year,
                v.color as vehicle_color,
                COALESCE(service_agg.service_count, 0) as service_count,
                COALESCE(service_agg.total_amount, 0) as total_amount,
                CASE 
                    WHEN service_agg.services_json IS NULL THEN '[]'
                    ELSE service_agg.services_json::text 
                END as services_json
        """.trimIndent()

        val baseFrom = """
            FROM protocols p
            LEFT JOIN clients c ON p.client_id = c.id AND c.company_id = p.company_id
            LEFT JOIN vehicles v ON p.vehicle_id = v.id AND v.company_id = p.company_id
            LEFT JOIN (
                SELECT 
                    ps.protocol_id,
                    COUNT(*) as service_count,
                    SUM(ps.final_price) as total_amount,
                    json_agg(
                        json_build_object(
                            'id', ps.id::text,
                            'name', ps.name,
                            'finalPrice', ps.final_price
                        )
                    ) as services_json
                FROM protocol_services ps
                WHERE ps.company_id = :companyId
                GROUP BY ps.protocol_id
            ) service_agg ON p.id = service_agg.protocol_id
        """.trimIndent()

        val whereConditions = mutableListOf<String>()
        val parameters = mutableMapOf<String, Any>()

        whereConditions.add("p.company_id = :companyId")
        parameters["companyId"] = companyId

        query.clientName?.let { name ->
            whereConditions.add("(LOWER(c.first_name) LIKE :clientName OR LOWER(c.last_name) LIKE :clientName)")
            parameters["clientName"] = "%${name.lowercase()}%"
        }

        query.licensePlate?.let { plate ->
            whereConditions.add("LOWER(v.license_plate) LIKE :licensePlate")
            parameters["licensePlate"] = "%${plate.lowercase()}%"
        }

        query.status?.let { status ->
            whereConditions.add("p.status = :status")
            parameters["status"] = status.name
        }

        query.startDate?.let { startDate ->
            whereConditions.add("p.start_date >= :startDate")
            parameters["startDate"] = startDate
        }

        query.endDate?.let { endDate ->
            whereConditions.add("p.end_date <= :endDate")
            parameters["endDate"] = endDate
        }

        query.make?.let { make ->
            whereConditions.add("LOWER(v.make) LIKE :make")
            parameters["make"] = "%${make.lowercase()}%"
        }

        query.model?.let { model ->
            whereConditions.add("LOWER(v.model) LIKE :model")
            parameters["model"] = "%${model.lowercase()}%"
        }

        query.title?.let { title ->
            whereConditions.add("LOWER(p.title) LIKE :title")
            parameters["title"] = "%${title.lowercase()}%"
        }

        query.serviceName?.let { serviceName ->
            whereConditions.add("EXISTS (SELECT 1 FROM protocol_services ps2 JOIN service_recipes sr ON ps2.name = sr.name WHERE ps2.protocol_id = p.id AND ps2.company_id = p.company_id AND sr.company_id = p.company_id AND LOWER(sr.name) LIKE :serviceName)")
            parameters["serviceName"] = "%${serviceName.lowercase()}%"
        }

        query.serviceIds?.let { serviceIds ->
            if (serviceIds.isNotEmpty()) {
                val serviceIdPlaceholders = serviceIds.mapIndexed { index, _ -> ":serviceId$index" }.joinToString(",")
                whereConditions.add("EXISTS (SELECT 1 FROM protocol_services ps3 JOIN service_recipes sr2 ON ps3.name = sr2.name WHERE ps3.protocol_id = p.id AND ps3.company_id = p.company_id AND sr2.company_id = p.company_id AND sr2.id IN ($serviceIdPlaceholders))")
                serviceIds.forEachIndexed { index, serviceId ->
                    parameters["serviceId$index"] = serviceId.toLongOrNull() ?: 0L
                }
            }
        }

        query.minPrice?.let { minPrice ->
            whereConditions.add("service_agg.total_amount >= :minPrice")
            parameters["minPrice"] = minPrice
        }

        query.maxPrice?.let { maxPrice ->
            whereConditions.add("service_agg.total_amount <= :maxPrice")
            parameters["maxPrice"] = maxPrice
        }

        val whereClause = if (whereConditions.isNotEmpty()) {
            "WHERE ${whereConditions.joinToString(" AND ")}"
        } else {
            "WHERE p.company_id = :companyId"
        }

        val orderBy = "ORDER BY p.updated_at DESC"

        val fullQuery = "$baseSelect $baseFrom $whereClause $orderBy"

        val countQuery = """
            SELECT COUNT(*) 
            FROM protocols p
            LEFT JOIN clients c ON p.client_id = c.id AND c.company_id = p.company_id
            LEFT JOIN vehicles v ON p.vehicle_id = v.id AND v.company_id = p.company_id
            LEFT JOIN (
                SELECT 
                    ps.protocol_id,
                    SUM(ps.final_price) as total_amount
                FROM protocol_services ps
                WHERE ps.company_id = :companyId
                GROUP BY ps.protocol_id
            ) service_agg ON p.id = service_agg.protocol_id
            $whereClause
        """.trimIndent()

        return Triple(fullQuery, countQuery, parameters)
    }

    private fun parseServices(servicesJson: String?): List<VisitServiceReadModel> {
        if (servicesJson.isNullOrBlank() || servicesJson == "[]") return emptyList()

        return try {
            val services = com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(servicesJson, Array<ServiceJsonModel>::class.java)

            services.map { service ->
                VisitServiceReadModel(
                    id = service.id,
                    name = service.name,
                    finalPrice = service.finalPrice
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getCurrentCompanyId(): Long {
        return (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
    }
}

@Entity
data class VisitSearchResult(
    @jakarta.persistence.Id
    val id: String,
    val title: String,
    val status: String,
    @jakarta.persistence.Column(name = "calendar_color_id")
    val calendarColorId: String,
    @jakarta.persistence.Column(name = "start_date")
    val startDate: java.time.LocalDateTime,
    @jakarta.persistence.Column(name = "end_date")
    val endDate: java.time.LocalDateTime,
    @jakarta.persistence.Column(name = "updated_at")
    val updatedAt: java.time.LocalDateTime,
    @jakarta.persistence.Column(name = "client_full_name")
    val clientFullName: String?,
    @jakarta.persistence.Column(name = "client_company")
    val clientCompany: String?,
    @jakarta.persistence.Column(name = "vehicle_make")
    val vehicleMake: String?,
    @jakarta.persistence.Column(name = "vehicle_model")
    val vehicleModel: String?,
    @jakarta.persistence.Column(name = "vehicle_license_plate")
    val vehicleLicensePlate: String?,
    @jakarta.persistence.Column(name = "vehicle_year")
    val vehicleYear: Int?,
    @jakarta.persistence.Column(name = "vehicle_color")
    val vehicleColor: String?,
    @jakarta.persistence.Column(name = "service_count")
    val serviceCount: Int,
    @jakarta.persistence.Column(name = "total_amount")
    val totalAmount: BigDecimal,
    @jakarta.persistence.Column(name = "services_json")
    val servicesJson: String?
)

data class ServiceJsonModel(
    val id: String,
    val name: String,
    val finalPrice: BigDecimal
)