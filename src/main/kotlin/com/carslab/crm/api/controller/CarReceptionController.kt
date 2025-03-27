package com.carslab.crm.api.controller

import com.carslab.crm.api.model.response.CarReceptionProtocolDetailResponse
import com.carslab.crm.api.model.response.CarReceptionProtocolListResponse
import com.carslab.crm.api.model.request.CarReceptionProtocolRequest
import com.carslab.crm.api.model.response.CarReceptionProtocolResponse
import com.carslab.crm.api.mapper.CarReceptionMapper
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.CarReceptionService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/receptions")
@CrossOrigin(origins = ["*"])
class CarReceptionController(
    private val carReceptionService: CarReceptionService
) {
    private val logger = LoggerFactory.getLogger(CarReceptionController::class.java)

    @PostMapping
    fun createCarReceptionProtocol(@RequestBody request: CarReceptionProtocolRequest): ResponseEntity<CarReceptionProtocolResponse> {
        logger.info("Received request to create new car reception protocol for: ${request.ownerName}, vehicle: ${request.make} ${request.model}")

        try {
            val domainProtocol = CarReceptionMapper.toDomain(request)

            val createdProtocol = carReceptionService.createProtocol(domainProtocol)

            // Konwertujemy wynik na odpowiedź API
            val response = CarReceptionMapper.toResponse(createdProtocol)

            logger.info("Successfully created car reception protocol with ID: ${response.id}")
            return ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            logger.error("Error creating car reception protocol", e)
            throw e
        }
    }

    @GetMapping("/list")
    fun getCarReceptionProtocolsList(
        @RequestParam(required = false) clientName: String?,
        @RequestParam(required = false) licensePlate: String?,
        @RequestParam(required = false) status: ProtocolStatus?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?
    ): ResponseEntity<List<CarReceptionProtocolListResponse>> {
        logger.info("Getting list view of car reception protocols with filters")

        val protocols = carReceptionService.searchProtocols(
            clientName = clientName,
            licensePlate = licensePlate,
            status = status?.let { CarReceptionMapper.mapStatus(it) },
            startDate = startDate?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) },
            endDate = endDate?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) }
        )

        val response = protocols.map { CarReceptionMapper.toListResponse(it) }
        println(response)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getAllCarReceptionProtocols(
        @RequestParam(required = false) clientName: String?,
        @RequestParam(required = false) licensePlate: String?,
        @RequestParam(required = false) status: ProtocolStatus?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?
    ): ResponseEntity<List<CarReceptionProtocolResponse>> {
        logger.info("Getting all car reception protocols with filters")

        val protocols = carReceptionService.searchProtocols(
            clientName = clientName,
            licensePlate = licensePlate,
            status = status?.let { CarReceptionMapper.mapStatus(it) },
            startDate = startDate?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) },
            endDate = endDate?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) }
        )

        val response = protocols.map { CarReceptionMapper.toResponse(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getCarReceptionProtocolById(@PathVariable id: String): ResponseEntity<CarReceptionProtocolDetailResponse> {
        logger.info("Getting car reception protocol by ID: $id")

        val protocol = carReceptionService.getProtocolById(ProtocolId(id))
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(CarReceptionMapper.toDetailResponse(protocol))
    }

    @PutMapping("/{id}")
    fun updateCarReceptionProtocol(
        @PathVariable id: String,
        @RequestBody request: CarReceptionProtocolRequest
    ): ResponseEntity<CarReceptionProtocolDetailResponse> {
        logger.info("Updating car reception protocol with ID: $id")

        // Sprawdzamy czy protokół istnieje
        val existingProtocol = carReceptionService.getProtocolById(ProtocolId(id))
            ?: return ResponseEntity.notFound().build()

        try {
            // Upewniamy się, że ID w żądaniu jest zgodne z ID w ścieżce
            val requestWithId = request.apply { this.id = id }

            // Konwertujemy żądanie na model domenowy, zachowując oryginalne daty audytowe
            val domainProtocol = CarReceptionMapper.toDomain(requestWithId).copy(
                audit = existingProtocol.audit.copy(
                    createdAt = existingProtocol.audit.createdAt
                )
            )

            // Aktualizujemy protokół przy użyciu serwisu
            val updatedProtocol = carReceptionService.updateProtocol(domainProtocol)

            // Konwertujemy wynik na odpowiedź API
            val response = CarReceptionMapper.toDetailResponse(updatedProtocol)

            logger.info("Successfully updated car reception protocol with ID: $id")
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error updating car reception protocol with ID: $id", e)
            throw e
        }
    }

    @PatchMapping("/{id}/status")
    fun updateProtocolStatus(
        @PathVariable id: String,
        @RequestBody statusUpdate: StatusUpdateRequest
    ): ResponseEntity<CarReceptionProtocolResponse> {
        logger.info("Updating status of car reception protocol with ID: $id to ${statusUpdate.status}")

        try {
            val domainStatus = CarReceptionMapper.mapStatus(statusUpdate.status)
            val updatedProtocol = carReceptionService.changeStatus(ProtocolId(id), domainStatus)

            val response = CarReceptionMapper.toResponse(updatedProtocol)
            return ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            logger.error("Protocol with ID $id not found", e)
            return ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Error updating status for protocol with ID: $id", e)
            throw e
        }
    }

    @DeleteMapping("/{id}")
    fun deleteCarReceptionProtocol(@PathVariable id: String): ResponseEntity<Void> {
        logger.info("Deleting car reception protocol with ID: $id")

        val deleted = carReceptionService.deleteProtocol(ProtocolId(id))

        return if (deleted) {
            logger.info("Successfully deleted car reception protocol with ID: $id")
            ResponseEntity.noContent().build()
        } else {
            logger.warn("Car reception protocol with ID: $id not found for deletion")
            ResponseEntity.notFound().build()
        }
    }
}

// Request dla aktualizacji statusu
class StatusUpdateRequest {
    var status: String? = null

    constructor() {}
}

// Rozszerzenie klasy CarReceptionMapper o metodę mapowania statusów
fun CarReceptionMapper.Companion.mapStatus(apiStatus: ProtocolStatus): com.carslab.crm.domain.model.ProtocolStatus {
    return when (apiStatus) {
        ProtocolStatus.SCHEDULED -> ProtocolStatus.SCHEDULED
        ProtocolStatus.PENDING_APPROVAL -> ProtocolStatus.PENDING_APPROVAL
        ProtocolStatus.IN_PROGRESS -> ProtocolStatus.IN_PROGRESS
        ProtocolStatus.READY_FOR_PICKUP -> com.carslab.crm.domain.model.ProtocolStatus.READY_FOR_PICKUP
        ProtocolStatus.COMPLETED -> com.carslab.crm.domain.model.ProtocolStatus.COMPLETED
    }
}

fun CarReceptionMapper.Companion.mapStatus(apiStatus: String?): ProtocolStatus {
    return when (apiStatus) {
        "SCHEDULED" -> ProtocolStatus.SCHEDULED
        "PENDING_APPROVAL" -> ProtocolStatus.PENDING_APPROVAL
        "IN_PROGRESS" -> ProtocolStatus.IN_PROGRESS
        "READY_FOR_PICKUP" -> ProtocolStatus.READY_FOR_PICKUP
        "COMPLETED" -> ProtocolStatus.COMPLETED
        else -> throw IllegalArgumentException("Test")
    }
}