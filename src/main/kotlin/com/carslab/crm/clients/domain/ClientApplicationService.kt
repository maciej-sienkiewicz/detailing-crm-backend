package com.carslab.crm.clients.domain

import com.carslab.crm.clients.api.CreateClientCommand
import com.carslab.crm.clients.api.UpdateClientCommand
import com.carslab.crm.domain.exception.DomainException
import com.carslab.crm.clients.domain.model.Client
import com.carslab.crm.clients.domain.model.ClientId
import com.carslab.crm.clients.domain.model.ClientStatistics
import com.carslab.crm.clients.domain.model.ClientSummary
import com.carslab.crm.clients.domain.model.ClientWithStatistics
import com.carslab.crm.clients.domain.model.Vehicle
import com.carslab.crm.clients.domain.model.VehicleServiceInfo
import com.carslab.crm.clients.domain.model.VehicleStatistics
import com.carslab.crm.clients.domain.model.VehicleWithStatistics
import com.carslab.crm.clients.domain.port.ClientSearchCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class ClientApplicationService(
    private val clientDomainService: ClientDomainService,
    private val associationService: ClientVehicleAssociationService
) {
    private val logger = LoggerFactory.getLogger(ClientApplicationService::class.java)

    fun createClient(request: CreateClientRequest): ClientDetailResponse {
        logger.info("Creating client: ${request.firstName} ${request.lastName}")

        try {
            validateCreateClientRequest(request)

            val command = CreateClientCommand(
                firstName = request.firstName,
                lastName = request.lastName,
                email = request.email,
                phone = request.phone,
                address = request.address,
                company = request.company,
                taxId = request.taxId,
                notes = request.notes
            )

            val client = clientDomainService.createClient(command)

            logger.info("Successfully created client with ID: ${client.id.value}")

            // Get client with statistics and vehicles
            val clientWithStats = clientDomainService.getClientWithStatistics(client.id)!!
            val vehicles = associationService.getClientVehicles(client.id)

            return ClientDetailResponse.from(clientWithStats, vehicles)
        } catch (e: DomainException) {
            logger.error("Failed to create client: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error creating client", e)
            throw RuntimeException("Failed to create client", e)
        }
    }

    fun updateClient(id: Long, request: UpdateClientRequest): ClientDetailResponse {
        logger.info("Updating client with ID: $id")

        try {
            validateUpdateClientRequest(request)

            val command = UpdateClientCommand(
                id = id.toString(),
                firstName = request.firstName,
                lastName = request.lastName,
                email = request.email,
                phone = request.phone,
                address = request.address,
                company = request.company,
                taxId = request.taxId,
                notes = request.notes
            )

            val client = clientDomainService.updateClient(ClientId.of(id), command)

            logger.info("Successfully updated client with ID: $id")

            // Get client with statistics and vehicles
            val clientWithStats = clientDomainService.getClientWithStatistics(client.id)!!
            val vehicles = associationService.getClientVehicles(client.id)

            return ClientDetailResponse.from(clientWithStats, vehicles)
        } catch (e: DomainException) {
            logger.error("Failed to update client $id: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error updating client $id", e)
            throw RuntimeException("Failed to update client", e)
        }
    }

    fun updateClientStatistics(clientId: ClientId, totalGmv: BigDecimal, counter: Long = 0): Unit {
        logger.info("Updating statistics for client with ID: $clientId")
        try {
            clientDomainService.updateClientStatistics(clientId, totalGmv, counter)
            logger.info("Successfully updated statistics for client with ID: $clientId")
        } catch (e: DomainException) {
            logger.error("Failed to update statistics for client $clientId: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error updating statistics for client $clientId", e)
            throw RuntimeException("Failed to update client statistics", e)
        }
    }

    @Transactional(readOnly = true)
    fun getClientById(id: Long): ClientDetailResponse? {
        logger.debug("Getting client by ID: $id")

        val clientWithStats = clientDomainService.getClientWithStatistics(ClientId.of(id))
            ?: return null

        val vehicles = associationService.getClientVehicles(ClientId.of(id))

        return ClientDetailResponse.from(clientWithStats, vehicles)
    }

    @Transactional(readOnly = true)
    fun searchClients(
        name: String? = null,
        email: String? = null,
        phone: String? = null,
        company: String? = null,
        pageable: Pageable
    ): Page<ClientDetailResponse> {
        logger.debug("Searching clients with criteria")

        val criteria = ClientSearchCriteria(
            name = name,
            email = email,
            phone = phone,
            company = company
        )

        return clientDomainService.searchClients(criteria, pageable)
            .map { client ->
                val clientWithStats = ClientWithStatistics(
                    client = client,
                    statistics = clientDomainService.getClientWithStatistics(client.id)?.statistics ?: ClientStatistics(clientId = client.id.value)
                )
                val vehicles = associationService.getClientVehicles(client.id)
                ClientDetailResponse.from(clientWithStats, vehicles)
            }
    }

    fun deleteClient(id: Long): Boolean {
        logger.info("Deleting client with ID: $id")

        val deleted = clientDomainService.deleteClient(ClientId.of(id))

        if (deleted) {
            logger.info("Successfully deleted client with ID: $id")
        } else {
            logger.warn("Client with ID: $id not found for deletion")
        }

        return deleted
    }

    private fun validateCreateClientRequest(request: CreateClientRequest) {
        require(request.firstName.isNotBlank()) { "First name cannot be blank" }
        require(request.lastName.isNotBlank()) { "Last name cannot be blank" }
        require(request.email?.isNotBlank() ?: false || request.phone?.isNotBlank() ?: false) {
            "Either email or phone must be provided"
        }
    }

    private fun validateUpdateClientRequest(request: UpdateClientRequest) {
        require(request.firstName.isNotBlank()) { "First name cannot be blank" }
        require(request.lastName.isNotBlank()) { "Last name cannot be blank" }
        require(request.email.isNotBlank() || request.phone.isNotBlank()) {
            "Either email or phone must be provided"
        }
        if (request.email.isNotBlank()) {
            require(isValidEmail(request.email)) { "Invalid email format" }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }
}

data class CreateClientRequest(
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phone : String? = null,
    val address: String? = null,
    val company: String? = null,
    val taxId: String? = null,
    val notes: String? = null
)

data class UpdateClientRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val company: String? = null,
    val taxId: String? = null,
    val notes: String? = null
)

data class CreateVehicleRequest(
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String? = null,
    val vin: String? = null,
    val mileage: Long? = null,
    val ownerIds: List<Long> = emptyList()
)

data class UpdateVehicleRequest(
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String? = null,
    val vin: String? = null,
    val mileage: Long? = null
)

// Response DTOs
data class ClientResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String?,
    val company: String?,
    val taxId: String?,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(client: Client): ClientResponse = ClientResponse(
            id = client.id.value,
            firstName = client.firstName,
            lastName = client.lastName,
            fullName = client.fullName,
            email = client.email,
            phone = client.phone,
            address = client.address,
            company = client.company,
            taxId = client.taxId,
            notes = client.notes,
            createdAt = client.audit.createdAt,
            updatedAt = client.audit.updatedAt
        )
    }
}

data class ClientDetailResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String?,
    val company: String?,
    val taxId: String?,
    val notes: String?,
    val statistics: ClientStatisticsResponse,
    val vehicles: List<VehicleSummaryResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(clientWithStats: ClientWithStatistics, vehicles: List<Vehicle>): ClientDetailResponse {
            val client = clientWithStats.client
            return ClientDetailResponse(
                id = client.id.value,
                firstName = client.firstName,
                lastName = client.lastName,
                fullName = client.fullName,
                email = client.email,
                phone = client.phone,
                address = client.address,
                company = client.company,
                taxId = client.taxId,
                notes = client.notes,
                statistics = ClientStatisticsResponse.from(clientWithStats.statistics),
                vehicles = vehicles.map { VehicleSummaryResponse.from(it) },
                createdAt = client.audit.createdAt,
                updatedAt = client.audit.updatedAt
            )
        }
    }
}

data class ClientStatisticsResponse(
    val visitCount: Long,
    val totalRevenue: BigDecimal,
    val vehicleCount: Long,
    val lastVisitDate: LocalDateTime?
) {
    companion object {
        fun from(stats: ClientStatistics): ClientStatisticsResponse = ClientStatisticsResponse(
            visitCount = stats.visitCount,
            totalRevenue = stats.totalRevenue,
            vehicleCount = stats.vehicleCount,
            lastVisitDate = stats.lastVisitDate
        )
    }
}

data class VehicleResponse(
    val id: Long,
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String?,
    val vin: String?,
    val mileage: Long?,
    val displayName: String,
    val serviceInfo: VehicleServiceInfoResponse,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(vehicle: Vehicle): VehicleResponse = VehicleResponse(
            id = vehicle.id.value,
            make = vehicle.make,
            model = vehicle.model,
            year = vehicle.year,
            licensePlate = vehicle.licensePlate,
            color = vehicle.color,
            vin = vehicle.vin,
            mileage = vehicle.mileage,
            displayName = vehicle.displayName,
            serviceInfo = VehicleServiceInfoResponse.from(vehicle.serviceInfo),
            createdAt = vehicle.audit.createdAt,
            updatedAt = vehicle.audit.updatedAt
        )
    }
}

data class VehicleDetailResponse(
    val id: Long,
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String?,
    val vin: String?,
    val mileage: Long?,
    val displayName: String,
    val serviceInfo: VehicleServiceInfoResponse,
    val statistics: VehicleStatisticsResponse,
    val owners: List<ClientSummaryResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(vehicleWithStats: VehicleWithStatistics, owners: List<ClientSummary>): VehicleDetailResponse {
            val vehicle = vehicleWithStats.vehicle
            return VehicleDetailResponse(
                id = vehicle.id.value,
                make = vehicle.make,
                model = vehicle.model,
                year = vehicle.year,
                licensePlate = vehicle.licensePlate,
                color = vehicle.color,
                vin = vehicle.vin,
                mileage = vehicle.mileage,
                displayName = vehicle.displayName,
                serviceInfo = VehicleServiceInfoResponse.from(vehicle.serviceInfo),
                statistics = VehicleStatisticsResponse.from(vehicleWithStats.statistics),
                owners = owners.map { ClientSummaryResponse.from(it) },
                createdAt = vehicle.audit.createdAt,
                updatedAt = vehicle.audit.updatedAt
            )
        }
    }
}

data class VehicleServiceInfoResponse(
    val totalServices: Int,
    val lastServiceDate: LocalDateTime?,
    val totalSpent: BigDecimal
) {
    companion object {
        fun from(serviceInfo: VehicleServiceInfo): VehicleServiceInfoResponse = VehicleServiceInfoResponse(
            totalServices = serviceInfo.totalServices,
            lastServiceDate = serviceInfo.lastServiceDate,
            totalSpent = serviceInfo.totalSpent
        )
    }
}

data class VehicleStatisticsResponse(
    val visitCount: Long,
    val totalRevenue: BigDecimal,
    val lastVisitDate: LocalDateTime?
) {
    companion object {
        fun from(stats: VehicleStatistics): VehicleStatisticsResponse = VehicleStatisticsResponse(
            visitCount = stats.visitCount,
            totalRevenue = stats.totalRevenue,
            lastVisitDate = stats.lastVisitDate
        )
    }
}

data class VehicleSummaryResponse(
    val id: Long,
    val make: String,
    val model: String,
    val licensePlate: String,
    val displayName: String
) {
    companion object {
        fun from(vehicle: Vehicle): VehicleSummaryResponse = VehicleSummaryResponse(
            id = vehicle.id.value,
            make = vehicle.make,
            model = vehicle.model,
            licensePlate = vehicle.licensePlate,
            displayName = vehicle.displayName
        )
    }
}

data class ClientSummaryResponse(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String
) {
    companion object {
        fun from(clientSummary: ClientSummary): ClientSummaryResponse = ClientSummaryResponse(
            id = clientSummary.id.value,
            fullName = clientSummary.fullName,
            email = clientSummary.email,
            phone = clientSummary.phone
        )
    }
}