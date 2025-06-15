package com.carslab.crm.modules.visits.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.mapper.CarReceptionDtoMapper
import com.carslab.crm.api.mapper.CarReceptionDtoMapper.fromCreateImageCommand
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.visits.api.response.ProtocolCountersResponse
import com.carslab.crm.modules.visits.api.response.ProtocolIdResponse
import com.carslab.crm.api.model.response.VehicleImageResponse
import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolComment
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.create.protocol.CreateMediaTypeModel
import com.carslab.crm.domain.model.create.protocol.DocumentType
import com.carslab.crm.domain.model.create.protocol.PaymentMethod
import com.carslab.crm.domain.model.create.protocol.VehicleReleaseDetailsModel
import com.carslab.crm.modules.visits.domain.CarReceptionFacade
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.storage.FileImageStorageService
import com.carslab.crm.infrastructure.util.ValidationUtils
import com.carslab.crm.modules.visits.api.commands.CarReceptionBasicDto
import com.carslab.crm.modules.visits.api.commands.CarReceptionDetailDto
import com.carslab.crm.modules.visits.api.commands.CarReceptionListDto
import com.carslab.crm.modules.visits.api.commands.ClientProtocolHistoryDto
import com.carslab.crm.modules.visits.api.commands.CreateCarReceptionCommand
import com.carslab.crm.modules.visits.api.commands.CreateVehicleImageCommand
import com.carslab.crm.modules.visits.api.commands.ReleaseVehicleCommand
import com.carslab.crm.modules.visits.api.commands.ServicesUpdateCommand
import com.carslab.crm.modules.visits.api.commands.UpdateCarReceptionCommand
import com.carslab.crm.modules.visits.api.commands.UpdateStatusCommand
import com.carslab.crm.modules.visits.api.commands.UpdateVehicleImageCommand
import com.carslab.crm.modules.visits.api.response.ProtocolDocumentDto
import com.carslab.crm.modules.visits.domain.SimpleAbandonedVisitsService
import com.carslab.crm.modules.visits.domain.ports.ProtocolCommentsRepository
import com.carslab.crm.modules.visits.infrastructure.storage.ProtocolDocumentStorageService
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
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/receptions")
@Tag(name = "Car Receptions", description = "Car reception protocol management endpoints")
class CarReceptionController(
    private val carReceptionFacade: CarReceptionFacade,
    private val imageStorageService: FileImageStorageService,
    private val objectMapper: ObjectMapper,
    private val simpleAbandonedVisitsService: SimpleAbandonedVisitsService,
    private val protocolDocumentStorageService: ProtocolDocumentStorageService,
    private val protocolCommentsRepository: ProtocolCommentsRepository,
    private val securityContext: SecurityContext,
) : BaseController() {

    @PostMapping("/with-files", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createCarReceptionProtocolWithFiles(request: MultipartHttpServletRequest): ResponseEntity<ProtocolIdResponse> {
        try {
            val protocolJson = request.getParameter("protocol")
                ?: return badRequest("Missing 'protocol' parameter")

            val protocolRequest: CreateCarReceptionCommand = objectMapper.readValue(protocolJson, CreateCarReceptionCommand::class.java)

            logger.info("Creating new car reception protocol with files for: ${protocolRequest.ownerName}, vehicle: ${protocolRequest.make} ${protocolRequest.model}")

            validateCarReceptionRequest(protocolRequest)

            val domainProtocol = CarReceptionDtoMapper.fromCreateCommand(protocolRequest)
            val createdProtocolId = carReceptionFacade.createProtocol(domainProtocol)

            processUploadedImages(request, protocolRequest.vehicleImages, createdProtocolId, domainProtocol.mediaItems)

            return created(ProtocolIdResponse(createdProtocolId.value))
        } catch (e: Exception) {
            return logAndRethrow("Error creating car reception protocol with files", e)
        }
    }

    @PostMapping("/{protocolId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadPhoto(@PathVariable protocolId: String, request: MultipartHttpServletRequest): ResponseEntity<ProtocolIdResponse> {
        try {
            logger.info("Adding new photo for protocol: $protocolId")

            val imageDetailsJson = request.getParameter("image")
                ?: return badRequest("Missing 'image' parameter")

            val imageCommand: CreateVehicleImageCommand = objectMapper.readValue(imageDetailsJson, CreateVehicleImageCommand::class.java)
            val image = imageCommand.fromCreateImageCommand()

            carReceptionFacade.storeUploadedImage(request, ProtocolId(protocolId), image)

            return created(ProtocolIdResponse(protocolId))
        } catch (e: Exception) {
            return logAndRethrow("Error uploading photo for car reception protocol", e)
        }
    }

    /**
     * Aktualizacja metadanych zdjęcia (nazwa, tagi, opis, lokalizacja)
     */
    @PatchMapping("/{protocolId}/image/{imageId}")
    fun updateImageMetadata(
        @PathVariable protocolId: String,
        @PathVariable imageId: String,
        @RequestBody updateCommand: UpdateVehicleImageCommand
    ): ResponseEntity<VehicleImageResponse> {
        try {
            logger.info("Updating image metadata for protocol: $protocolId, image: $imageId, data: $updateCommand")

            val updateImage = updateCommand.fromCreateImageCommand()

            // Aktualizacja metadanych obrazu
            val updatedImage = imageStorageService.updateImageMetadata(
                protocolId = ProtocolId(protocolId),
                imageId = imageId,
                name = updateImage.name,
                tags = updateImage.tags,
                description = updateImage.description,
                location = updateImage.location
            )

            // Mapowanie do odpowiedzi
            val response = VehicleImageResponse(
                id = updatedImage.id,
                name = updatedImage.name,
                size = updatedImage.size,
                tags = updatedImage.tags,
                type = "PHOTO",
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                description = "to moj opis",
                location = "pl",
            )

            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error updating image metadata", e)
        }
    }

    /**
     * Processes uploaded images from multipart request and stores them in the image storage service
     */
    private fun processUploadedImages(
        request: MultipartHttpServletRequest,
        vehicleImages: List<CreateVehicleImageCommand>?,
        protocolId: ProtocolId,
        mediaItems: List<CreateMediaTypeModel>
    ) {
        if (vehicleImages.isNullOrEmpty()) return

        request.fileMap.forEach { (paramName, file) ->
            val index = extractImageIndex(paramName) ?: return@forEach

            val imageRequest = vehicleImages.getOrNull(index)
            if (imageRequest != null && imageRequest.hasFile && index < mediaItems.size) {
                imageStorageService.storeFile(file, protocolId, mediaItems[index])
            }
        }
    }

    /**
     * Extracts image index from parameter name using regex
     * Returns null if parameter doesn't match the expected format
     */
    private fun extractImageIndex(paramName: String): Int? {
        val imageIndexRegex = """images\[(\d+)\]""".toRegex()
        val matchResult = imageIndexRegex.find(paramName) ?: return null
        return matchResult.groupValues[1].toInt()
    }

    @PostMapping
    fun createCarReceptionProtocol(@Valid @RequestBody command: CreateCarReceptionCommand): ResponseEntity<ProtocolIdResponse> {
        logger.info("Creating new car reception protocol for: ${command.ownerName}, vehicle: ${command.make} ${command.model}")

        try {
            validateCarReceptionRequest(command)

            val savedProtocolId = CarReceptionDtoMapper.fromCreateCommand(command)
                .let { carReceptionFacade.createProtocol(it) }

            logger.info("Successfully created car reception protocol with ID: $savedProtocolId")
            return created(ProtocolIdResponse(savedProtocolId.value))
        } catch (e: Exception) {
            return logAndRethrow("Error     creating car reception protocol", e)
        }
    }

    @PutMapping("/{protocolId}/services")
    fun updateServices(
        @PathVariable protocolId: String,
        @RequestBody command: ServicesUpdateCommand
    ): ResponseEntity<ProtocolIdResponse> {
        try {
            command.services
                .map { CarReceptionDtoMapper.mapCreateServiceCommandToService(it) }
                .let { carReceptionFacade.updateServices(ProtocolId(protocolId), it) }

            logger.info("Successfully updated services for protocol with ID: $protocolId")
            return created(ProtocolIdResponse(protocolId))
        } catch (e: Exception) {
            return logAndRethrow("Error updating services for car reception protocol", e)
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
            val protocolIdObj = ProtocolId(protocolId)
            val images = imageStorageService.getImagesByProtocol(protocolIdObj)
                .map {
                    ImageDTO(
                        id = it.id,
                        name = it.name,
                        size = it.size,
                        type = "image/jpeg", // Tymczasowo hardcoded
                        createdAt = LocalDateTime.now().toString(),
                        protocolId = protocolId
                    )
                }

            return ResponseEntity.ok(images.toList())
        } catch (e: Exception) {
            logger.error("Error getting images for protocol $protocolId", e)
            return ResponseEntity.internalServerError().build()
        }
    }


    @GetMapping("/list")
    @Operation(summary = "Get all car reception protocols", description = "Retrieves all car reception protocols with optional filtering and pagination")
    fun getAllCarReceptionProtocols(
        @Parameter(description = "Client name to filter by") @RequestParam(required = false) clientName: String?,
        @Parameter(description = "License plate to filter by") @RequestParam(required = false) licensePlate: String?,
        @Parameter(description = "Make") @RequestParam(required = false) make: String?,
        @Parameter(description = "Status to filter by") @RequestParam(required = false) status: String?,
        @Parameter(description = "Start date to filter by (ISO format)") @RequestParam(required = false) startDate: String?,
        @Parameter(description = "End date to filter by (ISO format)") @RequestParam(required = false) endDate: String?,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PaginatedResponse<CarReceptionListDto>> {
        logger.info("Getting all car reception protocols with filters, page: $page, size: $size")

        val domainStatus = status?.let { CarReceptionDtoMapper.mapStatus(it) }

        val paginatedProtocols = carReceptionFacade.searchProtocolsWithPagination(
            clientName = clientName,
            licensePlate = licensePlate,
            make = make,
            status = domainStatus,
            startDate = parseDateTimeParam(startDate),
            endDate = parseDateTimeParam(endDate),
            page = page,
            size = size
        )

        val response = PaginatedResponse(
            data = paginatedProtocols.data.map { CarReceptionDtoMapper.toListDto(it) },
            page = paginatedProtocols.page,
            size = paginatedProtocols.size,
            totalItems = paginatedProtocols.totalItems,
            totalPages = paginatedProtocols.totalPages
        )

        return ok(response)
    }

    @GetMapping("/counters")
    @Operation(summary = "Get protocol counters", description = "Retrieves count of protocols for each status")
    fun getProtocolCounters(): ResponseEntity<ProtocolCountersResponse> {
        logger.info("Getting protocol counters for all statuses")

        try {
            // Pobierz liczniki dla poszczególnych statusów
            val scheduledCount = carReceptionFacade.countProtocolsByStatus(ProtocolStatus.SCHEDULED)
            val inProgressCount = carReceptionFacade.countProtocolsByStatus(ProtocolStatus.IN_PROGRESS)
            val readyForPickupCount = carReceptionFacade.countProtocolsByStatus(ProtocolStatus.READY_FOR_PICKUP)
            val completedCount = carReceptionFacade.countProtocolsByStatus(ProtocolStatus.COMPLETED)
            val cancelledCount = carReceptionFacade.countProtocolsByStatus(ProtocolStatus.CANCELLED)

            // Suma wszystkich protokołów
            val totalCount = scheduledCount + inProgressCount + readyForPickupCount + completedCount + cancelledCount

            val counters = ProtocolCountersResponse(
                SCHEDULED = scheduledCount,
                IN_PROGRESS = inProgressCount,
                READY_FOR_PICKUP = readyForPickupCount,
                COMPLETED = completedCount,
                CANCELLED = cancelledCount,
                ALL = totalCount
            )

            logger.info("Protocol counters: $counters")
            return ok(counters)
        } catch (e: Exception) {
            return logAndRethrow("Error retrieving protocol counters", e)
        }
    }

    @GetMapping("/not-paginated")
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
            startDate = parseDateTimeParam(startDate),
            endDate = parseDateTimeParam(endDate)
        )

        val response = protocols.map { CarReceptionDtoMapper.toListDto(it) }
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

    @DeleteMapping("/{id}/image/{imageId}")
    fun deleteCarReceptionProtocolImage(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @Parameter(description = "Protocol ID", required = true) @PathVariable imageId: String,
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting car reception protocol with ID: $id")
        carReceptionFacade.deleteImage(ProtocolId(id), imageId)
        return ok(createSuccessResponse("Protocol successfully deleted", mapOf("protocolId" to id)))
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

    @PostMapping("/{protocolId}/document", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload protocol document", description = "Wgraj dokument do protokołu (zgody, umowy)")
    fun uploadProtocolDocument(
        @Parameter(description = "Protocol ID", required = true) @PathVariable protocolId: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("documentType") documentType: String,
        @RequestParam(value = "description", required = false) description: String?
    ): ResponseEntity<Map<String, Any>> {
        try {
            logger.info("Uploading document for protocol: $protocolId, type: $documentType")

            if (file.isEmpty) {
                return badRequest("File cannot be empty")
            }

            // Walidacja typu pliku - tylko PDF, DOC, DOCX
            val allowedTypes = setOf("application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

            if (file.contentType !in allowedTypes) {
                return badRequest("Only PDF, DOC, and DOCX files are allowed")
            }

            // Limit rozmiaru pliku - 10MB
            if (file.size > 10 * 1024 * 1024) {
                return badRequest("File size cannot exceed 10MB")
            }

            val storageId = protocolDocumentStorageService.storeDocument(
                file = file,
                protocolId = ProtocolId(protocolId),
                documentType = documentType,
                description = description
            )

            // Dodaj komentarz do protokołu
            protocolCommentsRepository.save(
                ProtocolComment(
                    protocolId = ProtocolId(protocolId),
                    author = securityContext.getCurrentUserName() ?: "System",
                    content = "Dodano dokument: ${file.originalFilename} (typ: $documentType)",
                    timestamp = Instant.now().toString(),
                    type = "system"
                )
            )

            return created(createSuccessResponse("Document uploaded successfully",
                mapOf(
                    "storageId" to storageId,
                    "protocolId" to protocolId,
                    "documentType" to documentType
                )
            ))

        } catch (e: Exception) {
            return logAndRethrow("Error uploading document for protocol $protocolId", e)
        }
    }

    @GetMapping("/{protocolId}/documents")
    @Operation(summary = "Get protocol documents", description = "Pobierz wszystkie dokumenty protokołu")
    fun getProtocolDocuments(
        @Parameter(description = "Protocol ID", required = true) @PathVariable protocolId: String
    ): ResponseEntity<List<ProtocolDocumentDto>> {
        try {
            logger.info("Getting documents for protocol: $protocolId")

            val documents = protocolDocumentStorageService.getDocumentsByProtocol(ProtocolId(protocolId))
            val response = documents.map { ProtocolDocumentDto.fromDomain(it) }

            return ok(response)

        } catch (e: Exception) {
            return logAndRethrow("Error getting documents for protocol $protocolId", e)
        }
    }

    @GetMapping("/document/{documentId}")
    @Operation(summary = "Download protocol document", description = "Pobierz dokument protokołu")
    fun downloadProtocolDocument(
        @Parameter(description = "Document ID", required = true) @PathVariable documentId: String
    ): ResponseEntity<Resource> {
        try {
            logger.info("Downloading document: $documentId")

            // Pobierz metadane dokumentu
            val metadata = protocolDocumentStorageService.getDocumentMetadata(documentId)
                ?: return ResponseEntity.notFound().build()

            // Pobierz dane pliku
            val fileData = protocolDocumentStorageService.getDocumentData(documentId)
                ?: return ResponseEntity.notFound().build()

            // Utwórz zasób
            val resource = ByteArrayResource(fileData)

            logger.info("Serving document $documentId: ${metadata.originalName} (${fileData.size} bytes)")

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${metadata.originalName}\"")
                .body(resource)

        } catch (e: Exception) {
            return logAndRethrow("Error downloading document $documentId", e)
        }
    }

    @DeleteMapping("/{protocolId}/document/{documentId}")
    @Operation(summary = "Delete protocol document", description = "Usuń dokument protokołu")
    fun deleteProtocolDocument(
        @Parameter(description = "Protocol ID", required = true) @PathVariable protocolId: String,
        @Parameter(description = "Document ID", required = true) @PathVariable documentId: String
    ): ResponseEntity<Map<String, Any>> {
        try {
            logger.info("Deleting document: $documentId for protocol: $protocolId")

            // Sprawdź czy dokument należy do tego protokołu
            val metadata = protocolDocumentStorageService.getDocumentMetadata(documentId)
            if (metadata == null || metadata.protocolId.value != protocolId) {
                return ResponseEntity.notFound().build()
            }

            val deleted = protocolDocumentStorageService.deleteDocument(documentId)

            if (deleted) {
                // Dodaj komentarz do protokołu
                protocolCommentsRepository.save(
                    ProtocolComment(
                        protocolId = ProtocolId(protocolId),
                        author = securityContext.getCurrentUserName() ?: "System",
                        content = "Usunięto dokument: ${metadata.originalName}",
                        timestamp = Instant.now().toString(),
                        type = "system"
                    )
                )

                logger.info("Successfully deleted document: $documentId")
                return ok(createSuccessResponse("Document deleted successfully",
                    mapOf("documentId" to documentId)))
            } else {
                logger.warn("Failed to delete document: $documentId")
                return ResponseEntity.internalServerError().build()
            }

        } catch (e: Exception) {
            return logAndRethrow("Error deleting document $documentId", e)
        }
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release vehicle to client", description = "Completes protocol by releasing vehicle to client with payment details")
    fun releaseVehicle(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @Valid @RequestBody command: ReleaseVehicleCommand
    ): ResponseEntity<CarReceptionDetailDto> {
        logger.info("Releasing vehicle for protocol with ID: $id with payment method: ${command.paymentMethod}")

        try {
            // Verify protocol exists
            val existingProtocol: CarReceptionProtocol = carReceptionFacade.getProtocolById(ProtocolId(id))
                ?: throw ResourceNotFoundException("Protocol", id)

            // Sprawdź, czy protokół jest w odpowiednim statusie (READY_FOR_PICKUP)
            if (existingProtocol.status != ProtocolStatus.READY_FOR_PICKUP) {
                throw ValidationException("Protocol must be in READY_FOR_PICKUP status to release vehicle")
            }

            val details = VehicleReleaseDetailsModel(
                paymentMethod = PaymentMethod.fromString(command.paymentMethod),
                documentType = DocumentType.fromString(command.documentType),
                releaseDate = LocalDateTime.now()
            )

            // Zaktualizuj status protokołu na COMPLETED
            val updatedProtocol = carReceptionFacade.releaseVehicle(
                existingProtocol = existingProtocol,
                releaseDetails = details
            )

            // Convert domain model to response
            val response = CarReceptionDtoMapper.toDetailDto(updatedProtocol)

            logger.info("Successfully released vehicle for protocol with ID: $id")
            return ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error releasing vehicle for protocol with ID: $id", e)
        }
    }

    private fun validateCarReceptionRequest(command: CreateCarReceptionCommand) {
        ValidationUtils.validateNotBlank(command.startDate, "Start date")

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
                LocalDateTime.parse(command.startDate, DateTimeFormatter.ISO_DATE_TIME)
            } catch (e: Exception) {
                try {
                    // Próba interpretacji jako samej daty i dodanie domyślnego czasu (8:00)
                    val localDate = LocalDate.parse(command.startDate, DateTimeFormatter.ISO_DATE)
                    LocalDateTime.of(localDate, LocalTime.of(8, 0))
                } catch (e2: Exception) {
                    throw ValidationException("Invalid start date format. Use ISO format (YYYY-MM-DD'T'HH:MM:SS)")
                }
            }
        }

        if (command.endDate != null) {
            try {
                var endDateTime: LocalDateTime
                var startDateTime: LocalDateTime

                // Parsowanie endDate z obsługą przypadku, gdy jest tylko data (bez czasu)
                try {
                    endDateTime = LocalDateTime.parse(command.endDate, DateTimeFormatter.ISO_DATE_TIME)
                } catch (e: Exception) {
                    // Jeśli przyszła sama data, dodajemy czas końca dnia (23:59:59)
                    val endLocalDate = LocalDate.parse(command.endDate, DateTimeFormatter.ISO_DATE)
                    endDateTime = LocalDateTime.of(endLocalDate, LocalTime.of(23, 59, 59))
                }

                // Parsowanie startDate z obsługą przypadku, gdy jest tylko data (bez czasu)
                try {
                    startDateTime = LocalDateTime.parse(command.startDate, DateTimeFormatter.ISO_DATE_TIME)
                } catch (e: Exception) {
                    // Jeśli przyszła sama data, dodajemy domyślną godzinę (8:00)
                    val startLocalDate = LocalDate.parse(command.startDate, DateTimeFormatter.ISO_DATE)
                    startDateTime = LocalDateTime.of(startLocalDate, LocalTime.of(8, 0))
                }

                if (endDateTime.isBefore(startDateTime)) {
                    throw ValidationException("End date cannot be before start date")
                }
            } catch (e: ValidationException) {
                throw e
            } catch (e: Exception) {
                throw ValidationException("Invalid date format. Use ISO format (YYYY-MM-DD'T'HH:MM:SS)")
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

    private fun parseDateTimeParam(dateTimeString: String?): LocalDateTime? {
        if (dateTimeString.isNullOrBlank()) return null

        return try {
            LocalDateTime.parse(dateTimeString)
        } catch (e: Exception) {
            try {
                // Próba sparsowania jako LocalDate i dodanie domyślnego czasu
                val date = LocalDate.parse(dateTimeString, DateTimeFormatter.ISO_DATE)
                LocalDateTime.of(date, LocalTime.of(0, 0)) // Początek dnia (00:00)
            } catch (e2: Exception) {
                logger.warn("Invalid date/time format: $dateTimeString")
                null
            }
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