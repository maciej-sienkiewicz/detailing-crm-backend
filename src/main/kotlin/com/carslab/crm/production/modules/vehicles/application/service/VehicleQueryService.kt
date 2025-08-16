package com.carslab.crm.production.modules.vehicles.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.clients.domain.ClientApplicationService
import com.carslab.crm.production.modules.associations.application.service.AssociationQueryService
import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.repository.ClientRepository
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleResponse
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleWithStatisticsResponse
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleRepository
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleSearchCriteria
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleStatisticsRepository
import com.carslab.crm.production.modules.vehicles.domain.service.VehicleDomainService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class VehicleQueryService(
    private val vehicleRepository: VehicleRepository,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository,
    private val vehicleDomainService: VehicleDomainService,
    private val associationQueryService: AssociationQueryService,
    private val clientApplicationService: ClientQueryService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VehicleQueryService::class.java)

    fun getVehiclesForCurrentCompany(pageable: Pageable): Page<VehicleResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching vehicles for company: {}", companyId)

        val vehicles = vehicleRepository.findByCompanyId(companyId, pageable)
        logger.debug("Found {} vehicles for company: {}", vehicles.numberOfElements, companyId)

        return vehicles.map { VehicleResponse.from(it) }
    }

    fun getVehicle(vehicleId: String): VehicleWithStatisticsResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching vehicle: {} for company: {}", vehicleId, companyId)

        val vehicle = vehicleDomainService.getVehicleForCompany(VehicleId.of(vehicleId.toLong()), companyId)
        val statistics = vehicleStatisticsRepository.findByVehicleId(vehicle.id)

        logger.debug("Vehicle found: {}", vehicle.displayName)

        return VehicleWithStatisticsResponse.from(vehicle, statistics)
    }

    fun searchVehicles(
        make: String?,
        model: String?,
        licensePlate: String?,
        vin: String?,
        year: Int?,
        ownerName: String?,
        minVisits: Int?,
        maxVisits: Int?,
        pageable: Pageable
    ): Page<VehicleResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Searching vehicles for company: {} with criteria", companyId)

        val searchCriteria = VehicleSearchCriteria(
            make = make,
            model = model,
            licensePlate = licensePlate,
            vin = vin,
            year = year,
            ownerName = ownerName,
            minVisits = minVisits,
            maxVisits = maxVisits
        )

        val vehicles = vehicleRepository.searchVehicles(companyId, searchCriteria, pageable)
        logger.debug("Found {} vehicles matching criteria for company: {}", vehicles.numberOfElements, companyId)

        return vehicles.map { VehicleResponse.from(it) }
    }

    fun getVehiclesForTable(
        make: String?,
        model: String?,
        licensePlate: String?,
        ownerName: String?,
        minVisits: Int?,
        maxVisits: Int?,
        pageable: Pageable
    ): Page<VehicleTableResponse> {
        val vehiclesPage = searchVehicles(
            make, model, licensePlate, null, null, ownerName, minVisits, maxVisits, pageable
        )

        if (vehiclesPage.isEmpty) return Page.empty()

        val vehicleIds = vehiclesPage.content.map { VehicleId.of(it.id.toLong()) }
        val ownersMap: Map<VehicleId, List<ClientId>> = associationQueryService.getVehicleOwnersMap(vehicleIds)
        val clientsMap: Map<String, ClientResponse> = clientApplicationService.findByIds(ownersMap.values.flatten()).associateBy { it.id }
        val statisticsMap = vehicleStatisticsRepository.findByVehicleIds(vehicleIds).associateBy { it.vehicleId }

        return vehiclesPage.map { vehicle ->
            val vehicleId = VehicleId.of(vehicle.id.toLong())
            val owners = ownersMap[vehicleId]?.mapNotNull { clientId ->
                clientsMap[clientId]?.let { client ->
                    VehicleOwnerSummary(
                        id = client.id.value,
                        firstName = client.firstName,
                        lastName = client.lastName,
                        fullName = client.fullName,
                        email = client.email,
                        phone = client.phone
                    )
                }
            } ?: emptyList()

            val statistics = statisticsMap[vehicleId]

            VehicleTableResponse(
                id = vehicle.id.toLong(),
                make = vehicle.make,
                model = vehicle.model,
                year = vehicle.year,
                licensePlate = vehicle.licensePlate,
                color = vehicle.color,
                vin = vehicle.vin,
                mileage = vehicle.mileage,
                owners = owners,
                visitCount = statistics?.visitCount ?: 0L,
                lastVisitDate = statistics?.lastVisitDate,
                totalRevenue = statistics?.totalRevenue ?: BigDecimal.ZERO,
                createdAt = vehicle.createdAt,
                updatedAt = vehicle.updatedAt
            )
        }
    }
}

data class VehicleTableResponse(
    val id: Long,
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String?,
    val vin: String?,
    val mileage: Long?,
    val owners: List<VehicleOwnerSummary>,
    val visitCount: Long,
    val lastVisitDate: LocalDateTime?,
    val totalRevenue: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class VehicleOwnerSummary(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String?,
    val phone: String?
)