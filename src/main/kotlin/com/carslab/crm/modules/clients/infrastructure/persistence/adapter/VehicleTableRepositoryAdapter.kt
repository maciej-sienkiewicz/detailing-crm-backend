package com.carslab.crm.modules.clients.infrastructure.persistence.adapter

import com.carslab.crm.modules.clients.api.responses.VehicleTableResponse
import com.carslab.crm.modules.clients.api.responses.VehicleOwnerSummary
import com.carslab.crm.modules.clients.domain.VehicleTableSearchCriteria
import com.carslab.crm.modules.clients.domain.port.VehicleTableRepository
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleTableJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

@Repository
@Transactional(readOnly = true)
class VehicleTableRepositoryAdapter(
    private val vehicleTableJpaRepository: VehicleTableJpaRepository,
    private val securityContext: SecurityContext
) : VehicleTableRepository {

    private val logger = LoggerFactory.getLogger(VehicleTableRepositoryAdapter::class.java)

    override fun findVehiclesForTable(
        criteria: VehicleTableSearchCriteria,
        pageable: Pageable
    ): Page<VehicleTableResponse> {
        val companyId = securityContext.getCurrentCompanyId()

        logger.debug("Retrieving vehicles for table with criteria: $criteria, page: ${pageable.pageNumber}, size: ${pageable.pageSize}")

        // Calculate offset for native query
        val offset = pageable.pageNumber * pageable.pageSize
        val limit = pageable.pageSize

        // POPRAWIONE: Lepsze mapowanie sortowania z bezpiecznymi domyślnymi wartościami
        val (sortField, sortDirection) = extractSortParameters(pageable)

        return try {
            // Execute native query to get vehicle table data
            val vehicleTableData = vehicleTableJpaRepository.findVehiclesForTableNative(
                make = criteria.make?.takeIf { it.isNotBlank() },
                model = criteria.model?.takeIf { it.isNotBlank() },
                licensePlate = criteria.licensePlate?.takeIf { it.isNotBlank() },
                ownerName = criteria.ownerName?.takeIf { it.isNotBlank() },
                minVisits = criteria.minVisits?.toLong(),
                maxVisits = criteria.maxVisits?.toLong(),
                companyId = companyId,
                sortField = sortField,
                sortDirection = sortDirection,
                limit = limit,
                offset = offset
            )

            logger.debug("Found ${vehicleTableData.size} vehicles from database")

            // Get total count for pagination
            val totalCount = vehicleTableJpaRepository.countVehiclesForTableNative(
                make = criteria.make?.takeIf { it.isNotBlank() },
                model = criteria.model?.takeIf { it.isNotBlank() },
                licensePlate = criteria.licensePlate?.takeIf { it.isNotBlank() },
                ownerName = criteria.ownerName?.takeIf { it.isNotBlank() },
                minVisits = criteria.minVisits?.toLong(),
                maxVisits = criteria.maxVisits?.toLong(),
                companyId = companyId
            )

            logger.debug("Total count: $totalCount")

            // Convert to response objects with error handling
            val vehicleResponses = vehicleTableData.mapNotNull { row ->
                try {
                    convertRowToVehicleResponse(row, companyId)
                } catch (e: Exception) {
                    logger.error("Error converting row to VehicleTableResponse: ${e.message}", e)
                    null // Skip this row instead of failing entire request
                }
            }

            logger.debug("Successfully converted ${vehicleResponses.size} vehicles to response objects")

            PageImpl(vehicleResponses, pageable, totalCount)

        } catch (e: Exception) {
            logger.error("Error retrieving vehicles for table view", e)
            throw RuntimeException("Failed to retrieve vehicles for table view: ${e.message}", e)
        }
    }

    /**
     * POPRAWIONA: Bezpieczna konwersja wiersza na VehicleTableResponse z obsługą java.sql.Timestamp
     */
    private fun convertRowToVehicleResponse(row: Array<Any>, companyId: Long): VehicleTableResponse {
        try {
            val vehicleId = (row[0] as Number).toLong()

            // Get owners for this vehicle with error handling
            val owners = try {
                vehicleTableJpaRepository.findVehicleOwnersNative(vehicleId, companyId)
                    .mapNotNull { ownerRow ->
                        try {
                            VehicleOwnerSummary(
                                id = (ownerRow[0] as Number).toLong(),
                                firstName = ownerRow[1] as String,
                                lastName = ownerRow[2] as String,
                                fullName = "${ownerRow[1]} ${ownerRow[2]}",
                                email = ownerRow[3] as String?,
                                phone = ownerRow[4] as String?
                            )
                        } catch (e: Exception) {
                            logger.warn("Error converting owner row for vehicle $vehicleId: ${e.message}")
                            null
                        }
                    }
            } catch (e: Exception) {
                logger.warn("Error fetching owners for vehicle $vehicleId: ${e.message}")
                emptyList()
            }

            return VehicleTableResponse(
                id = vehicleId,
                make = row[1] as String,
                model = row[2] as String,
                year = (row[3] as Number?)?.toInt(),
                licensePlate = row[4] as String,
                color = row[5] as String?,
                vin = row[6] as String?,
                mileage = (row[7] as Number?)?.toLong(),
                owners = owners,
                visitCount = (row[8] as Number).toLong(),
                lastVisitDate = convertToLocalDateTime(row[9]),
                totalRevenue = convertToDecimal(row[10]),
                createdAt = convertToLocalDateTime(row[11])!!,
                updatedAt = convertToLocalDateTime(row[12])!!
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid row data structure: ${e.message}", e)
        }
    }

    /**
     * NOWA: Bezpieczna konwersja na BigDecimal
     */
    private fun convertToDecimal(value: Any): BigDecimal {
        return when (value) {
            is BigDecimal -> value
            is Number -> BigDecimal.valueOf(value.toDouble())
            is String -> value.toBigDecimalOrNull() ?: BigDecimal.ZERO
            else -> BigDecimal.ZERO
        }
    }

    /**
     * NOWA: Bezpieczna konwersja java.sql.Timestamp na LocalDateTime
     */
    private fun convertToLocalDateTime(value: Any?): LocalDateTime? {
        return when (value) {
            null -> null
            is LocalDateTime -> value
            is java.sql.Timestamp -> value.toLocalDateTime()
            is java.util.Date -> {
                // Konwersja przez Instant dla innych typów Date
                java.time.Instant.ofEpochMilli(value.time)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime()
            }
            is String -> {
                try {
                    LocalDateTime.parse(value)
                } catch (e: Exception) {
                    logger.warn("Cannot parse string to LocalDateTime: $value")
                    null
                }
            }
            else -> {
                logger.warn("Unexpected value type for LocalDateTime conversion: ${value::class.simpleName}")
                null
            }
        }
    }

    /**
     * POPRAWIONA: Bardziej niezawodne mapowanie parametrów sortowania
     */
    private fun extractSortParameters(pageable: Pageable): Pair<String, String> {
        if (!pageable.sort.isSorted) {
            return Pair("createdAt", "DESC")
        }

        val sortOrder = pageable.sort.first()

        val field = when (sortOrder.property.lowercase()) {
            "make" -> "make"
            "model" -> "model"
            "year" -> "year"
            "licenseplate", "license_plate" -> "licensePlate"
            "visitcount", "visit_count" -> "visitCount"
            "lastvisitdate", "last_visit_date" -> "lastVisitDate"
            "totalrevenue", "total_revenue" -> "totalRevenue"
            "createdat", "created_at" -> "createdAt"
            "updatedat", "updated_at" -> "createdAt" // fallback to createdAt
            else -> {
                logger.warn("Unknown sort field: ${sortOrder.property}, falling back to createdAt")
                "createdAt"
            }
        }

        val direction = if (sortOrder.isDescending) "DESC" else "ASC"

        logger.debug("Sort parameters: field=$field, direction=$direction")
        return Pair(field, direction)
    }
}