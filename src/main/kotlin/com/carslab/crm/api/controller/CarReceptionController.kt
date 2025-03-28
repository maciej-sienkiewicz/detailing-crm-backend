package com.carslab.crm.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.mapper.CarReceptionMapper
import com.carslab.crm.api.mapper.CarReceptionMapperExtension
import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.api.model.request.CarReceptionProtocolRequest
import com.carslab.crm.api.model.request.VehicleImageMapper
import com.carslab.crm.api.model.response.CarReceptionProtocolDetailResponse
import com.carslab.crm.api.model.response.CarReceptionProtocolListResponse
import com.carslab.crm.api.model.response.CarReceptionProtocolResponse
import com.carslab.crm.api.model.response.ClientProtocolHistoryResponse
import com.carslab.crm.domain.CarReceptionFacade
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.VehicleImage
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.repository.InMemoryImageStorageService
import com.carslab.crm.infrastructure.util.ValidationUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
@RequestMapping("/api/receptions")
@CrossOrigin(origins = ["*"])
@Tag(name = "Car Receptions", description = "Car reception protocol management endpoints")
class CarReceptionController(
    private val carReceptionFacade: CarReceptionFacade,
    private val imageStorageService: InMemoryImageStorageService,
    private val objectMapper: ObjectMapper,
) : BaseController() {

    @PostMapping("/with-files", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Create a car reception protocol with files", description = "Creates a new car reception protocol with vehicle images")
    fun createCarReceptionProtocolWithFiles(request: MultipartHttpServletRequest): ResponseEntity<CarReceptionProtocolResponse> {
        try {
            // Pobierz dane protokołu z pola 'protocol'
            val protocolJson = request.getParameter("protocol")
                ?: return badRequest("Missing 'protocol' parameter")

            // Parsuj JSON na obiekt DTO
            val protocolRequest: CarReceptionProtocolRequest = objectMapper.readValue(protocolJson)

            logger.info("Creating new car reception protocol with files for: ${protocolRequest.ownerName}, vehicle: ${protocolRequest.make} ${protocolRequest.model}")

            // Walidacja danych
            validateCarReceptionRequest(protocolRequest)

            // Konwersja DTO na model domenowy
            val domainProtocol = CarReceptionMapper.toDomain(protocolRequest)

            // Lista obrazów do zapisania
            val imagesToSave = mutableListOf<VehicleImage>()

            // Przetworzenie zdjęć z VehicleImageRequest (bez plików)
            protocolRequest.vehicleImages?.forEachIndexed { index, imageRequest ->
                if (!imageRequest.has_file) {
                    // Zdjęcie bez pliku - używamy tylko metadanych
                    val image = VehicleImageMapper.toDomain(imageRequest)
                    imagesToSave.add(image)
                }
            }

            // Przetworzenie przesłanych plików
            val fileMap = request.fileMap

            fileMap.forEach { (paramName, file) ->
                // Sprawdź, czy to jest plik zdjęcia (format: images[index])
                val imageIndexRegex = """images\[(\d+)\]""".toRegex()
                val matchResult = imageIndexRegex.find(paramName)

                if (matchResult != null) {
                    val index = matchResult.groupValues[1].toInt()

                    // Pobierz odpowiedni request obrazu
                    val imageRequest = protocolRequest.vehicleImages?.getOrNull(index)

                    if (imageRequest != null && imageRequest.has_file) {
                        // Zapisz plik w pamięci i uzyskaj identyfikator
                        val storageId = imageStorageService.storeFile(file)

                        // Utwórz obiekt VehicleImage z danymi z requestu i identyfikatorem przechowywania
                        val image = VehicleImageMapper.toDomain(imageRequest, storageId)
                        imagesToSave.add(image)
                    }
                }
            }

            // Zapisz protokół
            val createdProtocol = carReceptionFacade.createProtocol(domainProtocol)

            // Konwersja modelu domenowego na DTO odpowiedzi
            val response = CarReceptionMapper.toResponse(createdProtocol)

            logger.info("Successfully created car reception protocol with ID: ${response.id} and ${imagesToSave.size} images")
            return created(response)
        } catch (e: Exception) {
            return logAndRethrow("Error creating car reception protocol with files", e)
        }
    }

    @GetMapping("/image/{fileId}")
    fun getImage(@PathVariable fileId: String): ResponseEntity<Resource> {
        try {
            // Sprawdź, czy plik istnieje
            if (!imageStorageService.fileExists(fileId)) {
                logger.warn("Image with id $fileId not found")
                return ResponseEntity.notFound().build()
            }

            // Pobierz dane pliku
            val fileData = imageStorageService.getFileData(fileId)
                ?: return ResponseEntity.notFound().build()

            // Pobierz metadane pliku
            val metadata = imageStorageService.getFileMetadata(fileId)
                ?: return ResponseEntity.internalServerError().build()

            // Utwórz zasób z danych pliku
            val resource = ByteArrayResource(fileData)

            // Określ typ MIME na podstawie metadanych
            val contentType = metadata.contentType

            logger.info("Serving image $fileId with type $contentType and size ${fileData.size} bytes")

            // Zwróć zasób
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${metadata.originalName}\"")
                .body(resource)
        } catch (e: Exception) {
            logger.error("Error serving image $fileId", e)
            return ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/receptions/{protocolId}/images")
    fun getProtocolImages(@PathVariable protocolId: String): ResponseEntity<List<ImageDTO>> {
        try {
            val images = imageStorageService.getAllFileIds()
                .map { imageStorageService.getFileMetadata(it) }
                .map {
                    ImageDTO(
                        id = "id",
                        name = it?.originalName ?: "",
                        size = it?.size ?: 0,
                        type = it?.contentType ?: "",
                        createdAt = it?.uploadTime.toString(),
                        protocolId = protocolId
                    )
                }

            return ResponseEntity.ok(images)
        } catch (e: Exception) {
            logger.error("Error getting images for protocol $protocolId", e)
            return ResponseEntity.internalServerError().build()
        }
    }


    @PostMapping
    @Operation(summary = "Create a car reception protocol", description = "Creates a new car reception protocol for a vehicle")
    fun createCarReceptionProtocol(@Valid @RequestBody request: CarReceptionProtocolRequest): ResponseEntity<CarReceptionProtocolResponse> {
        logger.info("Creating new car reception protocol for: ${request.ownerName}, vehicle: ${request.make} ${request.model}")

        try {
            // Validate request data
            validateCarReceptionRequest(request)

            // Convert request to domain model
            val domainProtocol = CarReceptionMapper.toDomain(request)

            // Create protocol using facade
            val createdProtocol = carReceptionFacade.createProtocol(domainProtocol)

            // Convert domain model to response
            val response = CarReceptionMapper.toResponse(createdProtocol)

            logger.info("Successfully created car reception protocol with ID: ${response.id}")
            return created(response)
        } catch (e: Exception) {
            return logAndRethrow("Error creating car reception protocol", e)
        }
    }

    @GetMapping("/list")
    @Operation(summary = "Get list of car reception protocols", description = "Retrieves a list of car reception protocols with optional filtering")
    fun getCarReceptionProtocolsList(
        @Parameter(description = "Client name to filter by") @RequestParam(required = false) clientName: String?,
        @Parameter(description = "License plate to filter by") @RequestParam(required = false) licensePlate: String?,
        @Parameter(description = "Status to filter by") @RequestParam(required = false) status: com.carslab.crm.api.model.request.ProtocolStatus?,
        @Parameter(description = "Start date to filter by (ISO format)") @RequestParam(required = false) startDate: String?,
        @Parameter(description = "End date to filter by (ISO format)") @RequestParam(required = false) endDate: String?
    ): ResponseEntity<List<CarReceptionProtocolListResponse>> {
        logger.info("Getting list of car reception protocols with filters")

        val domainStatus = status?.let { CarReceptionMapper.mapStatus(it.name) }

        val protocols = carReceptionFacade.searchProtocols(
            clientName = clientName,
            licensePlate = licensePlate,
            status = domainStatus,
            startDate = parseDateParam(startDate),
            endDate = parseDateParam(endDate)
        )

        val response = protocols.map { CarReceptionMapper.toListResponse(it) }
        return ok(response)
    }

    @GetMapping
    @Operation(summary = "Get all car reception protocols", description = "Retrieves all car reception protocols with optional filtering")
    fun getAllCarReceptionProtocols(
        @Parameter(description = "Client name to filter by") @RequestParam(required = false) clientName: String?,
        @Parameter(description = "License plate to filter by") @RequestParam(required = false) licensePlate: String?,
        @Parameter(description = "Status to filter by") @RequestParam(required = false) status: com.carslab.crm.api.model.request.ProtocolStatus?,
        @Parameter(description = "Start date to filter by (ISO format)") @RequestParam(required = false) startDate: String?,
        @Parameter(description = "End date to filter by (ISO format)") @RequestParam(required = false) endDate: String?
    ): ResponseEntity<List<CarReceptionProtocolResponse>> {
        logger.info("Getting all car reception protocols with filters")

        val domainStatus = status?.let { CarReceptionMapper.mapStatus(it.name) }

        val protocols = carReceptionFacade.searchProtocols(
            clientName = clientName,
            licensePlate = licensePlate,
            status = domainStatus,
            startDate = parseDateParam(startDate),
            endDate = parseDateParam(endDate)
        )

        val response = protocols.map { CarReceptionMapper.toResponse(it) }
        return ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get car reception protocol by ID", description = "Retrieves a specific car reception protocol by its ID")
    fun getCarReceptionProtocolById(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String
    ): ResponseEntity<CarReceptionProtocolDetailResponse> {
        logger.info("Getting car reception protocol by ID: $id")

        val protocol = carReceptionFacade.getProtocolById(ProtocolId(id))
            ?: throw ResourceNotFoundException("Protocol", id)

        return ok(CarReceptionMapper.toDetailResponse(protocol))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update car reception protocol", description = "Updates an existing car reception protocol")
    fun updateCarReceptionProtocol(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @Valid @RequestBody request: CarReceptionProtocolRequest
    ): ResponseEntity<CarReceptionProtocolDetailResponse> {
        logger.info("Updating car reception protocol with ID: $id")

        // Verify protocol exists
        val existingProtocol = carReceptionFacade.getProtocolById(ProtocolId(id))
            ?: throw ResourceNotFoundException("Protocol", id)

        try {
            // Validate request data
            validateCarReceptionRequest(request)

            // Ensure ID in request matches path ID
            val requestWithId = request.apply { this.id = id }

            // Convert request to domain model
            val domainProtocol = CarReceptionMapper.toDomain(requestWithId).copy(
                audit = existingProtocol.audit.copy(
                    createdAt = existingProtocol.audit.createdAt
                )
            )

            // Update protocol using facade
            val updatedProtocol = carReceptionFacade.updateProtocol(domainProtocol)

            // Convert domain model to response
            val response = CarReceptionMapper.toDetailResponse(updatedProtocol)

            logger.info("Successfully updated car reception protocol with ID: $id")
            return ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error updating car reception protocol with ID: $id", e)
        }
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update protocol status", description = "Updates the status of a car reception protocol")
    fun updateProtocolStatus(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @RequestBody statusUpdate: StatusUpdateRequest
    ): ResponseEntity<CarReceptionProtocolResponse> {
        logger.info("Updating status of car reception protocol with ID: $id to ${statusUpdate.status}")

        if (statusUpdate.status.isNullOrBlank()) {
            throw ValidationException("Status cannot be empty")
        }

        try {
            val domainStatus = CarReceptionMapper.mapStatus(statusUpdate.status!!)
            val updatedProtocol = carReceptionFacade.changeStatus(ProtocolId(id), domainStatus)

            val response = CarReceptionMapper.toResponse(updatedProtocol)
            return ok(response)
        } catch (e: IllegalArgumentException) {
            throw ValidationException("Invalid status value: ${statusUpdate.status}")
        } catch (e: Exception) {
            return logAndRethrow("Error updating status for protocol with ID: $id", e)
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete car reception protocol", description = "Deletes a car reception protocol by its ID")
    fun deleteCarReceptionProtocol(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting car reception protocol with ID: $id")

        val deleted = carReceptionFacade.deleteProtocol(ProtocolId(id))

        return if (deleted) {
            logger.info("Successfully deleted car reception protocol with ID: $id")
            ok(createSuccessResponse("Protocol successfully deleted", mapOf("protocolId" to id)))
        } else {
            logger.warn("Car reception protocol with ID: $id not found for deletion")
            throw ResourceNotFoundException("Protocol", id)
        }
    }

    @GetMapping("/{clientId}/protocols")
    @Operation(
        summary = "Get protocols for client",
        description = "Retrieves all car reception protocols for a specific client"
    )
    fun getProtocolsByClientId(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: Long,
        @Parameter(description = "Status to filter by") @RequestParam(required = false) status: ApiProtocolStatus?
    ): ResponseEntity<List<ClientProtocolHistoryResponse>> {
        logger.info("Getting protocols for client with ID: $clientId")


        val domainStatus = status?.let { CarReceptionMapperExtension.mapStatus(it) }

        val protocols = carReceptionFacade.searchProtocols(
            clientId = clientId,
            status = domainStatus
        )

        if (protocols.isEmpty()) {
            logger.info("No protocols found for client with ID: $clientId")
        } else {
            logger.info("Found ${protocols.size} protocols for client with ID: $clientId")
        }

        val response = protocols.map { CarReceptionMapper.toClientProtocolHistoryResponse(it) }
        return ok(response)
    }

    private fun validateCarReceptionRequest(request: CarReceptionProtocolRequest) {
        ValidationUtils.validateNotBlank(request.startDate, "Start date")

        if (request.startDate != null) {
            try {
                LocalDate.parse(request.startDate, DateTimeFormatter.ISO_DATE)
            } catch (e: Exception) {
                throw ValidationException("Invalid start date format. Use ISO format (YYYY-MM-DD)")
            }
        }

        if (request.endDate != null) {
            try {
                val endDate = LocalDate.parse(request.endDate, DateTimeFormatter.ISO_DATE)
                val startDate = LocalDate.parse(request.startDate, DateTimeFormatter.ISO_DATE)

                if (endDate.isBefore(startDate)) {
                    throw ValidationException("End date cannot be before start date")
                }
            } catch (e: ValidationException) {
                throw e
            } catch (e: Exception) {
                throw ValidationException("Invalid end date format. Use ISO format (YYYY-MM-DD)")
            }
        }

        ValidationUtils.validateNotBlank(request.licensePlate, "License plate")
        ValidationUtils.validateNotBlank(request.make, "Vehicle make")
        ValidationUtils.validateNotBlank(request.model, "Vehicle model")
        ValidationUtils.validateNotBlank(request.ownerName, "Owner name")

        if (request.ownerName != null && request.email == null && request.phone == null) {
            throw ValidationException("At least one contact method (email or phone) is required")
        }

        if (request.email != null) {
            ValidationUtils.validateEmail(request.email, "Email")
        }

        if (request.phone != null) {
            ValidationUtils.validatePhone(request.phone, "Phone")
        }
    }

    private fun parseDateParam(dateString: String?): LocalDate? {
        if (dateString.isNullOrBlank()) return null

        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE)
        } catch (e: Exception) {
            logger.warn("Invalid date format: $dateString")
            null
        }
    }
}

class StatusUpdateRequest {
    var status: String? = null

    constructor() {}
}

data class ImageDTO(
    val id: String,
    val name: String,
    val size: Long,
    val type: String,
    val createdAt: String,
    val protocolId: String,
    val description: String? = null,
    val location: String? = null
)

/**
 * DTO dla metadanych obrazu
 */
data class ImageMetadataDTO(
    val id: String,
    val protocolId: String,
    val originalName: String,
    val contentType: String,
    val size: Long,
    val uploadDate: Date,
    val description: String? = null,
    val location: String? = null
)