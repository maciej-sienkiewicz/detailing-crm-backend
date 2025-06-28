// src/main/kotlin/com/carslab/crm/modules/visits/api/ProtocolController.kt
package com.carslab.crm.modules.visits.api

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.modules.visits.api.dto.*
import com.carslab.crm.modules.visits.api.mappers.ProtocolApiMappers
import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.infrastructure.cqrs.CommandBus
import com.carslab.crm.infrastructure.cqrs.QueryBus
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.util.ValidationUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import jakarta.validation.Valid
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/protocols")
@Tag(name = "Protocol Management", description = "CQRS-based car reception protocol management")
class ProtocolController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus
) {
    private val logger = LoggerFactory.getLogger(ProtocolController::class.java)
    private val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()

    @PostMapping
    @Operation(summary = "Create new protocol")
    fun createProtocol(@Valid @RequestBody request: CreateProtocolRequest): ResponseEntity<ProtocolIdResponse> {
        return try {
            logger.info("Creating new protocol for: ${request.ownerName}, vehicle: ${request.make} ${request.model}")

            validateCreateRequest(request)

            val command = ProtocolApiMappers.toCreateCommand(request)
            val protocolId: String = commandBus.execute(command)

            logger.info("Successfully created protocol: $protocolId")
            ResponseEntity.status(HttpStatus.CREATED).body(ProtocolIdResponse(protocolId))
        } catch (e: ValidationException) {
            logger.warn("Validation error creating protocol: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error creating protocol", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/with-files", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Create protocol with files", description = "Create protocol with attached media files")
    fun createProtocolWithFiles(request: MultipartHttpServletRequest): ResponseEntity<ProtocolIdResponse> {
        return try {
            val protocolJson = request.getParameter("protocol")
                ?: return ResponseEntity.badRequest().build()

            val protocolRequest = objectMapper.readValue(protocolJson, CreateProtocolRequest::class.java)

            logger.info("Creating protocol with files for: ${protocolRequest.ownerName}")

            validateCreateRequest(protocolRequest)

            val command = ProtocolApiMappers.toCreateCommand(protocolRequest)
            val protocolId: String = commandBus.execute(command)

            // Process uploaded files with full metadata support
            processUploadedFilesWithMetadata(request, protocolId, protocolRequest.vehicleImages)

            logger.info("Successfully created protocol with files: $protocolId")
            ResponseEntity.status(HttpStatus.CREATED).body(ProtocolIdResponse(protocolId))
        } catch (e: Exception) {
            logger.error("Error creating protocol with files", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get protocol by ID")
    fun getProtocol(@PathVariable id: String): ResponseEntity<ProtocolDetailResponse> {
        return try {
            logger.debug("Getting protocol by ID: $id")

            val query = GetProtocolByIdQuery(id)
            val readModel = queryBus.execute(query)
                ?: return ResponseEntity.notFound().build()

            val response = ProtocolApiMappers.toDetailResponse(readModel)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error getting protocol $id", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping
    @Operation(summary = "Search protocols")
    fun searchProtocols(
        @RequestParam(required = false) clientName: String?,
        @RequestParam(required = false) licensePlate: String?,
        @RequestParam(required = false) make: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PaginatedResponse<ProtocolListResponse>> {
        return try {
            logger.debug("Searching protocols with filters: clientName=$clientName, licensePlate=$licensePlate, make=$make, status=$status")

            val query = SearchProtocolsQuery(
                clientName = clientName,
                licensePlate = licensePlate,
                make = make,
                status = status?.let { ProtocolStatus.valueOf(it) },
                startDate = parseDateTimeParam(startDate),
                endDate = parseDateTimeParam(endDate),
                page = page,
                size = size
            )

            val result = queryBus.execute(query)
            val response = PaginatedResponse(
                data = result.data.map { ProtocolApiMappers.toListResponse(it) },
                page = result.page,
                size = result.size,
                totalItems = result.totalItems,
                totalPages = result.totalPages
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error searching protocols", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/counters")
    @Operation(summary = "Get protocol counters", description = "Get count of protocols for each status")
    fun getProtocolCounters(): ResponseEntity<ProtocolCountersResponse> {
        return try {
            logger.debug("Getting protocol counters")

            val query = GetProtocolCountersQuery()
            val counters = queryBus.execute(query)

            val response = ProtocolCountersResponse(
                SCHEDULED = counters.scheduled,
                IN_PROGRESS = counters.inProgress,
                READY_FOR_PICKUP = counters.readyForPickup,
                COMPLETED = counters.completed,
                CANCELLED = counters.cancelled,
                ALL = counters.all
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error getting protocol counters", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/{clientId}/protocols")
    @Operation(summary = "Get protocols for client", description = "Get all protocols for a specific client")
    fun getProtocolsByClientId(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: Long,
        @Parameter(description = "Status to filter by") @RequestParam(required = false) status: String?
    ): ResponseEntity<List<ProtocolListResponse>> {
        return try {
            logger.debug("Getting protocols for client: $clientId")

            val query = GetClientProtocolHistoryQuery(
                clientId = clientId,
                status = status?.let { ProtocolStatus.valueOf(it) }
            )

            val protocols = queryBus.execute(query)
            val response = protocols.map { ProtocolApiMappers.toListResponse(it) }

            logger.debug("Found ${protocols.size} protocols for client $clientId")
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error getting protocols for client $clientId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update protocol", description = "Update an existing protocol")
    fun updateProtocol(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @Valid @RequestBody command: UpdateProtocolRequest
    ): ResponseEntity<ProtocolDetailResponse> {
        return try {
            logger.info("Updating protocol: $id")

            validateUpdateRequest(command, id)

            val updateCommand = ProtocolApiMappers.toUpdateCommand(command, id)
            commandBus.execute(updateCommand)

            // Get updated protocol
            val query = GetProtocolByIdQuery(id)
            val readModel = queryBus.execute(query)
                ?: throw ResourceNotFoundException("Protocol", id)

            val response = ProtocolApiMappers.toDetailResponse(readModel)

            logger.info("Successfully updated protocol: $id")
            ResponseEntity.ok(response)
        } catch (e: ValidationException) {
            logger.warn("Validation error updating protocol $id: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: ResourceNotFoundException) {
            logger.warn("Protocol not found: $id")
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Error updating protocol $id", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change protocol status")
    fun changeStatus(
        @PathVariable id: String,
        @RequestBody request: ChangeStatusRequest
    ): ResponseEntity<Unit> {
        return try {
            logger.info("Changing status of protocol $id to ${request.status}")

            val command = ChangeProtocolStatusCommand(
                protocolId = id,
                newStatus = ProtocolStatus.valueOf(request.status),
                reason = request.reason
            )

            commandBus.execute(command)

            logger.info("Successfully changed status of protocol $id to ${request.status}")
            ResponseEntity.ok().build()
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid status value: ${request.status}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error changing status for protocol $id", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PutMapping("/{id}/services")
    @Operation(summary = "Update protocol services")
    fun updateServices(
        @PathVariable id: String,
        @RequestBody command: UpdateProtocolServicesRequest
    ): ResponseEntity<Unit> {
        return try {
            logger.info("Updating services for protocol: $id")

            val updateCommand = UpdateProtocolServicesCommand(
                protocolId = id,
                services = command.services.map { ProtocolApiMappers.toCreateServiceCommand(it) }
            )

            commandBus.execute(updateCommand)

            logger.info("Successfully updated services for protocol: $id")
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            logger.error("Error updating services for protocol $id", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release vehicle to client", description = "Complete protocol by releasing vehicle")
    fun releaseVehicle(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @Valid @RequestBody request: ReleaseVehicleRequest
    ): ResponseEntity<ProtocolDetailResponse> {
        return try {
            logger.info("Releasing vehicle for protocol: $id with payment method: ${request.paymentMethod}")

            val command = ReleaseVehicleCommand(
                protocolId = id,
                paymentMethod = request.paymentMethod,
                documentType = request.documentType,
                additionalNotes = request.additionalNotes
            )

            commandBus.execute(command)

            // Get updated protocol
            val query = GetProtocolByIdQuery(id)
            val readModel = queryBus.execute(query)
                ?: throw ResourceNotFoundException("Protocol", id)

            val response = ProtocolApiMappers.toDetailResponse(readModel)

            logger.info("Successfully released vehicle for protocol: $id")
            ResponseEntity.ok(response)
        } catch (e: ValidationException) {
            logger.warn("Validation error releasing vehicle for protocol $id: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: ResourceNotFoundException) {
            logger.warn("Protocol not found: $id")
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Error releasing vehicle for protocol $id", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete protocol")
    fun deleteProtocol(@PathVariable id: String): ResponseEntity<Unit> {
        return try {
            logger.info("Deleting protocol: $id")

            val command = DeleteProtocolCommand(id)
            commandBus.execute(command)

            logger.info("Successfully deleted protocol: $id")
            ResponseEntity.noContent().build()
        } catch (e: ResourceNotFoundException) {
            logger.warn("Protocol not found for deletion: $id")
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Error deleting protocol $id", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/{protocolId}/document", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload protocol document")
    fun uploadProtocolDocument(
        @PathVariable protocolId: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("documentType") documentType: String,
        @RequestParam(value = "description", required = false) description: String?
    ): ResponseEntity<Map<String, Any>> {
        return try {
            logger.info("Uploading document for protocol: $protocolId, type: $documentType")

            validateFile(file)

            val command = UploadProtocolDocumentCommand(
                protocolId = protocolId,
                file = file,
                documentType = documentType,
                description = description
            )

            val storageId = commandBus.execute(command)

            val response = mapOf(
                "success" to true,
                "message" to "Document uploaded successfully",
                "data" to mapOf(
                    "storageId" to storageId,
                    "protocolId" to protocolId,
                    "documentType" to documentType
                )
            )

            logger.info("Successfully uploaded document for protocol: $protocolId")
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: ValidationException) {
            logger.warn("Validation error uploading document: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error uploading document for protocol $protocolId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/{protocolId}/documents")
    @Operation(summary = "Get protocol documents")
    fun getProtocolDocuments(
        @PathVariable protocolId: String
    ): ResponseEntity<List<ProtocolDocumentResponse>> {
        return try {
            logger.debug("Getting documents for protocol: $protocolId")

            val query = GetProtocolDocumentsQuery(protocolId)
            val documents = queryBus.execute(query)

            ResponseEntity.ok(documents)
        } catch (e: Exception) {
            logger.error("Error getting documents for protocol $protocolId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/document/{documentId}")
    @Operation(summary = "Download protocol document")
    fun downloadProtocolDocument(
        @PathVariable documentId: String
    ): ResponseEntity<Resource> {
        return try {
            logger.debug("Downloading document: $documentId")

            val query = GetProtocolDocumentQuery(documentId)
            val documentData = queryBus.execute(query)
                ?: return ResponseEntity.notFound().build()

            val resource = ByteArrayResource(documentData.data)

            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(documentData.contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${documentData.originalName}\"")
                .body(resource)
        } catch (e: Exception) {
            logger.error("Error downloading document $documentId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @DeleteMapping("/{protocolId}/document/{documentId}")
    @Operation(summary = "Delete protocol document")
    fun deleteProtocolDocument(
        @PathVariable protocolId: String,
        @PathVariable documentId: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            logger.info("Deleting document: $documentId for protocol: $protocolId")

            val command = DeleteProtocolDocumentCommand(protocolId, documentId)
            commandBus.execute(command)

            val response = mapOf(
                "success" to true,
                "message" to "Document deleted successfully",
                "data" to mapOf("documentId" to documentId)
            )

            logger.info("Successfully deleted document: $documentId")
            ResponseEntity.ok(response)
        } catch (e: ResourceNotFoundException) {
            logger.warn("Document not found: $documentId")
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Error deleting document $documentId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/{protocolId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload single image with metadata")
    fun uploadPhoto(@PathVariable protocolId: String, request: MultipartHttpServletRequest): ResponseEntity<ProtocolIdResponse> {
        return try {
            logger.info("Adding new photo for protocol: $protocolId")

            val imageDetailsJson = request.getParameter("image")
                ?: return ResponseEntity.badRequest().body(ProtocolIdResponse("Missing 'image' parameter"))

            val imageCommand: CreateVehicleImageCommand = objectMapper.readValue(imageDetailsJson, CreateVehicleImageCommand::class.java)

            // Find the uploaded file
            val file = request.fileMap.values.firstOrNull()
                ?: return ResponseEntity.badRequest().body(ProtocolIdResponse("No file uploaded"))

            val command = UploadVisitMediaCommand(
                visitId = protocolId,
                file = file,
                mediaDetails = MediaDetailsCommand(
                    name = imageCommand.name ?: file.originalFilename ?: "image_${System.currentTimeMillis()}",
                    description = imageCommand.description,
                    location = imageCommand.location,
                    tags = imageCommand.tags,
                    type = imageCommand.type ?: "PHOTO"
                )
            )

            commandBus.execute(command)

            ResponseEntity.status(HttpStatus.CREATED).body(ProtocolIdResponse(protocolId))
        } catch (e: Exception) {
            logger.error("Error uploading photo for protocol $protocolId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PatchMapping("/{protocolId}/image/{imageId}")
    @Operation(summary = "Update image metadata", description = "Update metadata for a specific image")
    fun updateImageMetadata(
        @PathVariable protocolId: String,
        @PathVariable imageId: String,
        @RequestBody updateCommand: UpdateVehicleImageCommand
    ): ResponseEntity<Map<String, Any>> {
        return try {
            logger.info("Updating image metadata for protocol: $protocolId, image: $imageId")

            val command = UpdateVisitMediaCommand(
                visitId = protocolId,
                mediaId = imageId,
                name = updateCommand.name ?: "Unknown",
                description = updateCommand.description,
                location = updateCommand.location,
                tags = updateCommand.tags
            )

            commandBus.execute(command)

            val response = mapOf(
                "success" to true,
                "message" to "Image metadata updated successfully",
                "data" to mapOf(
                    "imageId" to imageId,
                    "protocolId" to protocolId
                )
            )

            logger.info("Successfully updated image metadata: $imageId")
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error updating image metadata", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @DeleteMapping("/{protocolId}/image/{imageId}")
    @Operation(summary = "Delete protocol image")
    fun deleteProtocolImage(
        @PathVariable protocolId: String,
        @PathVariable imageId: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            logger.info("Deleting image: $imageId from protocol: $protocolId")

            val command = DeleteVisitMediaCommand(
                visitId = protocolId,
                mediaId = imageId
            )

            commandBus.execute(command)

            val response = mapOf(
                "success" to true,
                "message" to "Image deleted successfully",
                "data" to mapOf("imageId" to imageId)
            )

            logger.info("Successfully deleted image: $imageId")
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error deleting image $imageId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    // Helper methods
    private fun validateCreateRequest(request: CreateProtocolRequest) {
        ValidationUtils.validateNotBlank(request.startDate, "Start date")
        ValidationUtils.validateNotBlank(request.make, "Vehicle make")
        ValidationUtils.validateNotBlank(request.model, "Vehicle model")
        ValidationUtils.validateNotBlank(request.ownerName, "Owner name")

        if (request.email == null && request.phone == null) {
            throw ValidationException("At least one contact method (email or phone) is required")
        }

        request.email?.let { ValidationUtils.validateEmail(it, "Email") }
        request.phone?.let { ValidationUtils.validatePhone(it, "Phone") }
    }

    private fun validateUpdateRequest(request: UpdateProtocolRequest, protocolId: String) {
        if (request.id != protocolId) {
            throw ValidationException("Protocol ID in path ($protocolId) does not match ID in request body (${request.id})")
        }

        // Reuse create validation logic by converting to CreateProtocolRequest
        validateCreateRequest(request.toCreateRequest())
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw ValidationException("File cannot be empty")
        }

        val allowedTypes = setOf("application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

        if (file.contentType !in allowedTypes) {
            throw ValidationException("Only PDF, DOC, and DOCX files are allowed")
        }

        if (file.size > 10 * 1024 * 1024) {
            throw ValidationException("File size cannot exceed 10MB")
        }
    }

    private fun parseDateTimeParam(dateTimeString: String?): LocalDateTime? {
        if (dateTimeString.isNullOrBlank()) return null

        return try {
            LocalDateTime.parse(dateTimeString)
        } catch (e: Exception) {
            try {
                val date = LocalDate.parse(dateTimeString, DateTimeFormatter.ISO_DATE)
                LocalDateTime.of(date, LocalTime.of(0, 0))
            } catch (e2: Exception) {
                logger.warn("Invalid date/time format: $dateTimeString")
                null
            }
        }
    }

    private fun processUploadedFilesWithMetadata(
        request: MultipartHttpServletRequest,
        protocolId: String,
        vehicleImages: List<CreateVehicleImageCommand>?
    ) {
        if (vehicleImages.isNullOrEmpty()) {
            return
        }

        // Process files with their corresponding metadata
        request.fileMap.forEach { (paramName, file) ->
            try {
                val index = extractImageIndex(paramName)
                if (index != null && index < vehicleImages.size) {
                    val imageMetadata = vehicleImages[index]

                    // Create command with full metadata from request
                    val command = UploadVisitMediaCommand(
                        visitId = protocolId,
                        file = file,
                        mediaDetails = MediaDetailsCommand(
                            name = imageMetadata.name ?: file.originalFilename ?: "image_${System.currentTimeMillis()}",
                            description = imageMetadata.description,
                            location = imageMetadata.location,
                            tags = imageMetadata.tags,
                            type = imageMetadata.type ?: "PHOTO"
                        )
                    )

                    commandBus.execute(command)
                    logger.debug("Successfully processed file: $paramName with metadata")
                }
            } catch (e: Exception) {
                logger.warn("Failed to upload file $paramName for protocol $protocolId", e)
            }
        }
    }

    private fun extractImageIndex(paramName: String): Int? {
        val imageIndexRegex = """images\[(\d+)\]""".toRegex()
        val matchResult = imageIndexRegex.find(paramName) ?: return null
        return matchResult.groupValues[1].toInt()
    }

    data class ChangeStatusRequest(
        val status: String,
        val reason: String? = null
    )

    data class UpdateProtocolServicesRequest(
        val services: List<CreateServiceRequest>
    )

    data class ReleaseVehicleRequest(
        val paymentMethod: String,
        val documentType: String,
        val additionalNotes: String? = null
    )

    data class ProtocolCountersResponse(
        val SCHEDULED: Int,
        val IN_PROGRESS: Int,
        val READY_FOR_PICKUP: Int,
        val COMPLETED: Int,
        val CANCELLED: Int,
        val ALL: Int
    )
}