package com.carslab.crm.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.mapper.CarReceptionDtoMapper
import com.carslab.crm.api.mapper.CarReceptionDtoMapper.DATETIME_FORMATTER
import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.api.model.commands.*
import com.carslab.crm.domain.CarReceptionFacade
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.VehicleImage
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.repository.InMemoryImageStorageService
import com.carslab.crm.infrastructure.util.ValidationUtils
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    fun createCarReceptionProtocolWithFiles(request: MultipartHttpServletRequest): ResponseEntity<CarReceptionBasicDto> {
        try {
            // Pobierz dane protokołu z pola 'protocol'
            val protocolJson = request.getParameter("protocol")
                ?: return badRequest("Missing 'protocol' parameter")

            // Parsuj JSON na obiekt DTO
            val protocolRequest: CreateCarReceptionCommand = objectMapper.readValue(protocolJson, CreateCarReceptionCommand::class.java)

            logger.info("Creating new car reception protocol with files for: ${protocolRequest.ownerName}, vehicle: ${protocolRequest.make} ${protocolRequest.model}")

            // Walidacja danych
            validateCarReceptionRequest(protocolRequest)

            // Konwersja DTO na model domenowy
            val domainProtocol = CarReceptionDtoMapper.fromCreateCommand(protocolRequest)

            // Lista obrazów do zapisania
            val imagesToSave = mutableListOf<VehicleImage>()

            // Przetworzenie zdjęć z VehicleImageRequest (bez plików)
            protocolRequest.vehicleImages?.forEachIndexed { index, imageRequest ->
                if (!imageRequest.hasFile) {
                    // Zdjęcie bez pliku - używamy tylko metadanych
                    val image = VehicleImage(
                        id = java.util.UUID.randomUUID().toString(),
                        name = imageRequest.name ?: "",
                        size = imageRequest.size ?: 0,
                        type = imageRequest.type ?: "",
                        storageId = "",
                        description = imageRequest.description,
                        location = imageRequest.location
                    )
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

                    if (imageRequest != null && imageRequest.hasFile) {
                        // Zapisz plik w pamięci i uzyskaj identyfikator
                        val storageId = imageStorageService.storeFile(file)

                        // Utwórz obiekt VehicleImage z danymi z requestu i identyfikatorem przechowywania
                        val image = VehicleImage(
                            id = java.util.UUID.randomUUID().toString(),
                            name = imageRequest.name ?: file.originalFilename ?: "",
                            size = imageRequest.size ?: file.size,
                            type = imageRequest.type ?: file.contentType ?: "",
                            storageId = storageId,
                            description = imageRequest.description,
                            location = imageRequest.location
                        )
                        imagesToSave.add(image)
                    }
                }
            }

            // Zapisz protokół
            val createdProtocol = carReceptionFacade.createProtocol(domainProtocol)

            // Konwersja modelu domenowego na DTO odpowiedzi
            val response = CarReceptionBasicDto(
                id = createdProtocol.value,
                createdAt = LocalDateTime.now().format(DATETIME_FORMATTER),
                updatedAt = LocalDateTime.now().format(DATETIME_FORMATTER),
                statusUpdatedAt = LocalDateTime.now().format(DATETIME_FORMATTER),
                status = ApiProtocolStatus.SCHEDULED
            )

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

    @GetMapping("/protocols/{protocolId}/images")
    fun getProtocolImages(@PathVariable protocolId: String): ResponseEntity<List<ImageDTO>> {
        try {
            val images = imageStorageService.getAllFileIds()
                .map { imageStorageService.getFileMetadata(it) }
                .filterNotNull()
                .map {
                    ImageDTO(
                        id = "id",
                        name = it.originalName,
                        size = it.size,
                        type = it.contentType,
                        createdAt = it.uploadTime.toString(),
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
    fun createCarReceptionProtocol(@Valid @RequestBody command: CreateCarReceptionCommand): ResponseEntity<CarReceptionBasicDto> {
        logger.info("Creating new car reception protocol for: ${command.ownerName}, vehicle: ${command.make} ${command.model}")

        try {
            validateCarReceptionRequest(command)

            // Convert command to domain model
            val domainProtocol = CarReceptionDtoMapper.fromCreateCommand(command)

            // Create protocol using facade
            val createdProtocol = carReceptionFacade.createProtocol(domainProtocol)

            // Convert domain model to response
            val response = CarReceptionBasicDto(
                id = createdProtocol.value,
                createdAt = LocalDateTime.now().format(DATETIME_FORMATTER),
                updatedAt = LocalDateTime.now().format(DATETIME_FORMATTER),
                statusUpdatedAt = LocalDateTime.now().format(DATETIME_FORMATTER),
                status = ApiProtocolStatus.SCHEDULED
            )

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
        @Parameter(description = "Status to filter by") @RequestParam(required = false) status: String?,
        @Parameter(description = "Start date to filter by (ISO format)") @RequestParam(required = false) startDate: String?,
        @Parameter(description = "End date to filter by (ISO format)") @RequestParam(required = false) endDate: String?
    ): ResponseEntity<List<CarReceptionListDto>> {
        logger.info("Getting list of car reception protocols with filters")

        val domainStatus = status?.let { CarReceptionDtoMapper.mapStatus(it) }

        val protocols = carReceptionFacade.searchProtocols(
            clientName = clientName,
            licensePlate = licensePlate,
            status = domainStatus,
            startDate = parseDateParam(startDate),
            endDate = parseDateParam(endDate)
        )

        val response = protocols.map { CarReceptionDtoMapper.toListDto(it) }
        return ok(response)
    }

    @GetMapping
    @Operation(summary = "Get all car reception protocols", description = "Retrieves all car reception protocols with optional filtering")
    fun getAllCarReceptionProtocols(
        @Parameter(description = "Client name to filter by") @RequestParam(required = false) clientName: String?,
        @Parameter(description = "License plate to filter by") @RequestParam(required = false) licensePlate: String?,
        @Parameter(description = "Status to filter by") @RequestParam(required = false) status: String?,
        @Parameter(description = "Start date to filter by (ISO format)") @RequestParam(required = false) startDate: String?,
        @Parameter(description = "End date to filter by (ISO format)") @RequestParam(required = false) endDate: String?
    ): ResponseEntity<List<CarReceptionBasicDto>> {
        logger.info("Getting all car reception protocols with filters")

        val domainStatus = status?.let { CarReceptionDtoMapper.mapStatus(it) }

        val protocols = carReceptionFacade.searchProtocols(
            clientName = clientName,
            licensePlate = licensePlate,
            status = domainStatus,
            startDate = parseDateParam(startDate),
            endDate = parseDateParam(endDate)
        )

        val response = protocols.map { CarReceptionDtoMapper.toBasicDto(it) }
        return ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get car reception protocol by ID", description = "Retrieves a specific car reception protocol by its ID")
    fun getCarReceptionProtocolById(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String
    ): ResponseEntity<CarReceptionDetailDto> {
        logger.info("Getting car reception protocol by ID: $id")

        val protocol = carReceptionFacade.getProtocolById(ProtocolId(id))
            ?: throw ResourceNotFoundException("Protocol", id)

        return ok(CarReceptionDtoMapper.toDetailDto(protocol))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update car reception protocol", description = "Updates an existing car reception protocol")
    fun updateCarReceptionProtocol(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @Valid @RequestBody command: UpdateCarReceptionCommand
    ): ResponseEntity<CarReceptionDetailDto> {
        logger.info("Updating car reception protocol with ID: $id")

        // Verify protocol exists
        val existingProtocol = carReceptionFacade.getProtocolById(ProtocolId(id))
            ?: throw ResourceNotFoundException("Protocol", id)

        try {
            // Validate command data
            validateCarReceptionRequest(command)

            // Ensure ID in command matches path ID
            if (command.id != id) {
                throw ValidationException("Protocol ID in path ($id) does not match ID in request body (${command.id})")
            }

            // Convert command to domain model
            val domainProtocol = CarReceptionDtoMapper.fromUpdateCommand(command, existingProtocol)

            // Update protocol using facade
            val updatedProtocol = carReceptionFacade.updateProtocol(domainProtocol)

            // Convert domain model to response
            val response = CarReceptionDtoMapper.toDetailDto(updatedProtocol)

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
        @RequestBody command: UpdateStatusCommand
    ): ResponseEntity<CarReceptionBasicDto> {
        logger.info("Updating status of car reception protocol with ID: $id to ${command.status}")

        try {
            val domainStatus = CarReceptionDtoMapper.mapApiStatusToDomain(command.status)
            val updatedProtocol = carReceptionFacade.changeStatus(ProtocolId(id), domainStatus)

            val response = CarReceptionDtoMapper.toBasicDto(updatedProtocol)
            return ok(response)
        } catch (e: IllegalArgumentException) {
            throw ValidationException("Invalid status value: ${command.status}")
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
        @Parameter(description = "Status to filter by") @RequestParam(required = false) status: String?
    ): ResponseEntity<List<ClientProtocolHistoryDto>> {
        logger.info("Getting protocols for client with ID: $clientId")

        val domainStatus = status?.let { CarReceptionDtoMapper.mapStatus(it) }

        val protocols = carReceptionFacade.searchProtocols(
            clientId = clientId,
            status = domainStatus
        )

        if (protocols.isEmpty()) {
            logger.info("No protocols found for client with ID: $clientId")
        } else {
            logger.info("Found ${protocols.size} protocols for client with ID: $clientId")
        }

        val response = protocols.map { CarReceptionDtoMapper.toClientHistoryDto(it) }
        return ok(response)
    }

    // Metody pomocnicze

    private fun validateCarReceptionRequest(command: CreateCarReceptionCommand) {
        ValidationUtils.validateNotBlank(command.startDate, "Start date")

        if (command.startDate != null) {
            try {
                LocalDate.parse(command.startDate, DateTimeFormatter.ISO_DATE)
            } catch (e: Exception) {
                throw ValidationException("Invalid start date format. Use ISO format (YYYY-MM-DD)")
            }
        }

        if (command.endDate != null) {
            try {
                val endDate = LocalDate.parse(command.endDate, DateTimeFormatter.ISO_DATE)
                val startDate = LocalDate.parse(command.startDate, DateTimeFormatter.ISO_DATE)

                if (endDate.isBefore(startDate)) {
                    throw ValidationException("End date cannot be before start date")
                }
            } catch (e: ValidationException) {
                throw e
            } catch (e: Exception) {
                throw ValidationException("Invalid end date format. Use ISO format (YYYY-MM-DD)")
            }
        }

        ValidationUtils.validateNotBlank(command.licensePlate, "License plate")
        ValidationUtils.validateNotBlank(command.make, "Vehicle make")
        ValidationUtils.validateNotBlank(command.model, "Vehicle model")
        ValidationUtils.validateNotBlank(command.ownerName, "Owner name")

        if (command.ownerName != null && command.email == null && command.phone == null) {
            throw ValidationException("At least one contact method (email or phone) is required")
        }

        if (command.email != null) {
            ValidationUtils.validateEmail(command.email, "Email")
        }

        if (command.phone != null) {
            ValidationUtils.validatePhone(command.phone, "Phone")
        }
    }

    private fun validateCarReceptionRequest(command: UpdateCarReceptionCommand) {
        ValidationUtils.validateNotBlank(command.id, "Protocol ID")
        ValidationUtils.validateNotBlank(command.startDate, "Start date")

        if (command.startDate != null) {
            try {
                LocalDate.parse(command.startDate, DateTimeFormatter.ISO_DATE)
            } catch (e: Exception) {
                throw ValidationException("Invalid start date format. Use ISO format (YYYY-MM-DD)")
            }
        }

        if (command.endDate != null) {
            try {
                val endDate = LocalDate.parse(command.endDate, DateTimeFormatter.ISO_DATE)
                val startDate = LocalDate.parse(command.startDate, DateTimeFormatter.ISO_DATE)

                if (endDate.isBefore(startDate)) {
                    throw ValidationException("End date cannot be before start date")
                }
            } catch (e: ValidationException) {
                throw e
            } catch (e: Exception) {
                throw ValidationException("Invalid end date format. Use ISO format (YYYY-MM-DD)")
            }
        }

        ValidationUtils.validateNotBlank(command.licensePlate, "License plate")
        ValidationUtils.validateNotBlank(command.make, "Vehicle make")
        ValidationUtils.validateNotBlank(command.model, "Vehicle model")
        ValidationUtils.validateNotBlank(command.ownerName, "Owner name")

        if (command.ownerName != null && command.email == null && command.phone == null) {
            throw ValidationException("At least one contact method (email or phone) is required")
        }

        if (command.email != null) {
            ValidationUtils.validateEmail(command.email, "Email")
        }

        if (command.phone != null) {
            ValidationUtils.validatePhone(command.phone, "Phone")
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