package com.carslab.crm.clients.infrastructure.persistence.adapter

import com.carslab.crm.clients.api.responses.VehicleTableResponse
import com.carslab.crm.clients.api.responses.VehicleOwnerSummary
import com.carslab.crm.clients.domain.VehicleTableSearchCriteria
import com.carslab.crm.clients.domain.port.VehicleTableRepository
import com.carslab.crm.clients.infrastructure.persistence.repository.VehicleTableJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
@Transactional(readOnly = true)
class VehicleTableRepositoryAdapter(
    private val vehicleTableJpaRepository: VehicleTableJpaRepository,
    private val securityContext: SecurityContext
) : VehicleTableRepository {

    override fun findVehiclesForTable(
        criteria: VehicleTableSearchCriteria,
        pageable: Pageable
    ): Page<VehicleTableResponse> {
        val companyId = securityContext.getCurrentCompanyId()

        // Calculate offset for native query
        val offset = pageable.pageNumber * pageable.pageSize
        val limit = pageable.pageSize

        // Determine sort field and direction for the native query
        val (sortField, sortDirection) = if (pageable.sort.isSorted) {
            val sortOrder = pageable.sort.first()
            val field = when (sortOrder.property) {
                "make" -> "make"
                "model" -> "model"
                "year" -> "year"
                "licensePlate" -> "licensePlate"
                "visitCount" -> "visitCount"
                "lastVisitDate" -> "lastVisitDate"
                "totalRevenue" -> "totalRevenue"
                "createdAt" -> "createdAt"
                else -> "createdAt"
            }
            val direction = if (sortOrder.isDescending) "DESC" else "ASC"
            Pair(field, direction)
        } else {
            Pair("createdAt", "DESC")
        }

        // Execute native query to get vehicle table data
        val vehicleTableData = vehicleTableJpaRepository.findVehiclesForTableNative(
            make = criteria.make,
            model = criteria.model,
            licensePlate = criteria.licensePlate,
            ownerName = criteria.ownerName,
            minVisits = criteria.minVisits?.toLong(),
            maxVisits = criteria.maxVisits?.toLong(),
            companyId = companyId,
            sortField = sortField,
            sortDirection = sortDirection,
            limit = limit,
            offset = offset
        )

        // Get total count for pagination
        val totalCount = vehicleTableJpaRepository.countVehiclesForTableNative(
            make = criteria.make,
            model = criteria.model,
            licensePlate = criteria.licensePlate,
            ownerName = criteria.ownerName,
            minVisits = criteria.minVisits?.toLong(),
            maxVisits = criteria.maxVisits?.toLong(),
            companyId = companyId
        )

        // Convert to response objects
        val vehicleResponses = vehicleTableData.map { row ->
            // Get owners for this vehicle
            val vehicleId = row[0] as Long
            val owners = vehicleTableJpaRepository.findVehicleOwnersNative(vehicleId, companyId)
                .map { ownerRow ->
                    VehicleOwnerSummary(
                        id = ownerRow[0] as Long,
                        firstName = ownerRow[1] as String,
                        lastName = ownerRow[2] as String,
                        fullName = "${ownerRow[1]} ${ownerRow[2]}",
                        email = ownerRow[3] as String?,
                        phone = ownerRow[4] as String?
                    )
                }

            VehicleTableResponse(
                id = vehicleId,
                make = row[1] as String,
                model = row[2] as String,
                year = row[3] as Int?,
                licensePlate = row[4] as String,
                color = row[5] as String?,
                vin = row[6] as String?,
                mileage = (row[7] as Number?)?.toLong(),
                owners = owners,
                visitCount = (row[8] as Number).toLong(),
                lastVisitDate = row[9] as LocalDateTime?,
                totalRevenue = (row[10] as Number).let { BigDecimal.valueOf(it.toDouble()) },
                createdAt = row[11] as LocalDateTime,
                updatedAt = row[12] as LocalDateTime
            )
        }

        return PageImpl(vehicleResponses, pageable, totalCount)
    }
}