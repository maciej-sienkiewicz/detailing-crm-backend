package com.carslab.crm.api.controller

import com.carslab.crm.api.model.request.ServiceHistoryRequest
import com.carslab.crm.api.model.response.ServiceHistoryResponse
import com.carslab.crm.api.model.response.VehicleOwnerResponse
import com.carslab.crm.api.model.request.VehicleRequest
import com.carslab.crm.api.mapper.ServiceHistoryMapper
import com.carslab.crm.domain.VehicleFacade
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.model.ServiceHistoryId
import com.carslab.crm.api.model.response.VehicleResponse
import com.carslab.crm.api.model.response.VehicleStatisticsResponse
import com.carslab.crm.domain.port.VehicleStatisticsRepository
import com.carslab.crm.presentation.mapper.VehicleMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = ["*"])
class VehicleController(
    private val vehicleFacade: VehicleFacade,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository
) {
    private val logger = LoggerFactory.getLogger(VehicleController::class.java)

    @PostMapping
    fun createVehicle(@RequestBody request: VehicleRequest): ResponseEntity<VehicleResponse> {
        logger.info("Received request to create new vehicle: ${request.make} ${request.model}, plate: ${request.licensePlate}")

        try {
            // Konwertujemy żądanie na model domenowy
            val domainVehicle = VehicleMapper.toDomain(request)

            // Tworzymy pojazd za pomocą serwisu
            val createdVehicle = vehicleFacade.createVehicle(domainVehicle)

            // Konwertujemy wynik na odpowiedź API
            val response = VehicleMapper.toResponse(createdVehicle)

            logger.info("Successfully created vehicle with ID: ${response.id}")
            return ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            logger.error("Error creating vehicle", e)
            throw e
        }
    }

    @GetMapping
    fun getAllVehicles(): ResponseEntity<List<VehicleResponse>> {
        logger.info("Getting all vehicles")

        val vehicles = vehicleFacade.getAllVehicles()
        val response = vehicles.map { VehicleMapper.toResponse(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/owners")
    fun getOwners(@PathVariable id: String): ResponseEntity<List<VehicleOwnerResponse>> {

        val owners = vehicleFacade.getVehicleOwners(VehicleId(id.toLong()))
        return ResponseEntity.ok(owners.map { VehicleOwnerResponse(it.id.value, it.fullName) })
    }

    @GetMapping("/{id}")
    fun getVehicleById(@PathVariable id: String): ResponseEntity<VehicleResponse> {
        logger.info("Getting vehicle by ID: $id")

        val vehicle = vehicleFacade.getVehicleById(VehicleId(id.toLong()))
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(VehicleMapper.toResponse(vehicle))
    }

    @GetMapping("/{id}/statistics")
    fun getVehicleStatistics(@PathVariable id: String): ResponseEntity<VehicleStatisticsResponse> {
        logger.info("Getting vehicle statistics: $id")

        val stats = vehicleStatisticsRepository.findById(VehicleId((id.toLong())))
            .let { VehicleStatisticsResponse(it.visitNo, it.gmv) }

        return ResponseEntity.ok(stats)
    }

    @GetMapping("/owner/{ownerId}")
    fun getVehiclesByOwnerId(@PathVariable ownerId: String): ResponseEntity<List<VehicleResponse>> {
        logger.info("Getting vehicles by owner ID: $ownerId")

        val vehicles = vehicleFacade.getVehiclesByOwnerId(ownerId)
        val response = vehicles.map { VehicleMapper.toResponse(it) }
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}")
    fun updateVehicle(
        @PathVariable id: String,
        @RequestBody request: VehicleRequest
    ): ResponseEntity<VehicleResponse> {
        logger.info("Updating vehicle with ID: $id")

        // Sprawdzamy czy pojazd istnieje
        val existingVehicle = vehicleFacade.getVehicleById(VehicleId(id.toLong()))
            ?: return ResponseEntity.notFound().build()

        try {
            // Upewniamy się, że ID w żądaniu jest zgodne z ID w ścieżce
            val requestWithId = request.apply { this.id = id }

            // Konwertujemy żądanie na model domenowy, zachowując oryginalne dane audytowe
            val domainVehicle = VehicleMapper.toDomain(requestWithId).copy(
                audit = existingVehicle.audit.copy(
                    createdAt = existingVehicle.audit.createdAt
                )
            )

            // Aktualizujemy pojazd przy użyciu serwisu
            val updatedVehicle = vehicleFacade.updateVehicle(domainVehicle)

            // Konwertujemy wynik na odpowiedź API
            val response = VehicleMapper.toResponse(updatedVehicle)

            logger.info("Successfully updated vehicle with ID: $id")
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error updating vehicle with ID: $id", e)
            throw e
        }
    }

    @DeleteMapping("/{id}")
    fun deleteVehicle(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting vehicle with ID: $id")

        val deleted = vehicleFacade.deleteVehicle(VehicleId(id.toLong()))

        return if (deleted) {
            logger.info("Successfully deleted vehicle with ID: $id")
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Vehicle successfully deleted",
                "vehicleId" to id
            ))
        } else {
            logger.warn("Vehicle with ID: $id not found for deletion")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "success" to false,
                "message" to "Vehicle not found",
                "vehicleId" to id
            ))
        }
    }

    @GetMapping("/{id}/service-history")
    fun getVehicleServiceHistory(@PathVariable id: String): ResponseEntity<List<ServiceHistoryResponse>> {
        logger.info("Getting service history for vehicle: $id")

        val serviceHistory = vehicleFacade.getServiceHistoryByVehicleId(VehicleId(id.toLong()))
        val response = serviceHistory.map { ServiceHistoryMapper.toResponse(it) }

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{id}/service-history")
    fun addServiceHistoryEntry(
        @PathVariable id: String,
        @RequestBody request: ServiceHistoryRequest
    ): ResponseEntity<ServiceHistoryResponse> {
        logger.info("Adding service history entry for vehicle: $id")

        try {
            // Sprawdzamy czy pojazd istnieje
            val vehicle = vehicleFacade.getVehicleById(VehicleId(id.toLong()))
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)

            // Upewniamy się, że ID pojazdu w żądaniu jest zgodne z ID w ścieżce
            val requestWithVehicleId = request.apply { this.vehicleId = id }

            // Konwertujemy żądanie na model domenowy
            val domainServiceHistory = ServiceHistoryMapper.toDomain(requestWithVehicleId)

            // Dodajemy wpis do historii serwisowej
            val createdServiceHistory = vehicleFacade.addServiceHistoryEntry(domainServiceHistory)

            // Konwertujemy wynik na odpowiedź API
            val response = ServiceHistoryMapper.toResponse(createdServiceHistory)

            logger.info("Successfully added service history entry with ID: ${response.id}")
            return ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            logger.error("Error adding service history entry for vehicle: $id", e)
            throw e
        }
    }

    @DeleteMapping("/service-history/{id}")
    fun deleteServiceHistoryEntry(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting service history entry with ID: $id")

        val deleted = vehicleFacade.deleteServiceHistoryEntry(ServiceHistoryId(id))

        return if (deleted) {
            logger.info("Successfully deleted service history entry with ID: $id")
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Service history entry successfully deleted",
                "serviceHistoryId" to id
            ))
        } else {
            logger.warn("Service history entry with ID: $id not found for deletion")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "success" to false,
                "message" to "Service history entry not found",
                "serviceHistoryId" to id
            ))
        }
    }

    @GetMapping("/search")
    fun searchVehicles(
        @RequestParam(required = false) licensePlate: String?,
        @RequestParam(required = false) make: String?,
        @RequestParam(required = false) model: String?
    ): ResponseEntity<List<VehicleResponse>> {
        logger.info("Searching vehicles with filters: licensePlate=$licensePlate, make=$make, model=$model")

        val vehicles = vehicleFacade.searchVehicles(
            licensePlate = licensePlate,
            make = make,
            model = model
        )

        val response = vehicles.map { VehicleMapper.toResponse(it) }
        return ResponseEntity.ok(response)
    }
}