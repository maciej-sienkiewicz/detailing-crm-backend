package com.carslab.crm.clients.domain

import com.carslab.crm.clients.api.responses.VehicleTableResponse
import com.carslab.crm.clients.api.responses.VehicleOwnerSummary
import com.carslab.crm.clients.domain.port.VehicleTableRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Domain service for handling vehicle table view operations
 */
@Service
@Transactional(readOnly = true)
class VehicleTableService(
    private val vehicleTableRepository: VehicleTableRepository
) {
    private val logger = LoggerFactory.getLogger(VehicleTableService::class.java)

    /**
     * Retrieves vehicles with all necessary statistics for table display
     */
    fun getVehiclesForTable(
        pageable: Pageable,
        make: String? = null,
        model: String? = null,
        licensePlate: String? = null,
        ownerName: String? = null,
        minVisits: Int? = null,
        maxVisits: Int? = null
    ): Page<VehicleTableResponse> {
        logger.debug("Retrieving vehicles for table with filters: make=$make, model=$model, licensePlate=$licensePlate, ownerName=$ownerName, minVisits=$minVisits, maxVisits=$maxVisits")

        return try {
            val criteria = VehicleTableSearchCriteria(
                make = make,
                model = model,
                licensePlate = licensePlate,
                ownerName = ownerName,
                minVisits = minVisits,
                maxVisits = maxVisits
            )

            val vehicleTablePage = vehicleTableRepository.findVehiclesForTable(criteria, pageable)

            logger.debug("Successfully retrieved ${vehicleTablePage.numberOfElements} vehicles for table view")
            vehicleTablePage
        } catch (e: Exception) {
            logger.error("Error retrieving vehicles for table view", e)
            throw RuntimeException("Failed to retrieve vehicles for table view", e)
        }
    }
}

/**
 * Search criteria for vehicle table queries
 */
data class VehicleTableSearchCriteria(
    val make: String? = null,
    val model: String? = null,
    val licensePlate: String? = null,
    val ownerName: String? = null,
    val minVisits: Int? = null,
    val maxVisits: Int? = null
)