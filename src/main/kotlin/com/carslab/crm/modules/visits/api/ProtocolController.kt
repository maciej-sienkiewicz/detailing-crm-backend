package com.carslab.crm.modules.visits.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.visits.api.response.ProtocolCountersResponse
import com.carslab.crm.modules.visits.api.response.ProtocolIdResponse
import com.carslab.crm.api.model.response.VehicleImageResponse
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.util.ValidationUtils
import com.carslab.crm.modules.visits.api.commands.*
import com.carslab.crm.modules.visits.api.mappers.ProtocolApiMappers
import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.infrastructure.cqrs.CommandBus
import com.carslab.crm.infrastructure.cqrs.QueryBus
import com.carslab.crm.modules.visits.api.response.ProtocolDocumentDto
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateClientCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateDocumentsCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreatePeriodCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateVehicleCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.UpdateClientCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.UpdateDocumentsCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.UpdatePeriodCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.UpdateVehicleCommand
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
@RequestMapping("/api/v1/protocols")
@Tag(name = "Protocol Management", description = "CQRS-based car reception protocol management")
class ProtocolController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
    private val objectMapper: ObjectMapper
) : BaseController() {

    @PostMapping("/with-files", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createCarReceptionProtocolWithFiles(request: MultipartHttpServletRequest): ResponseEntity<ProtocolIdResponse> {
        val protocolJson = request.getParameter("protocol")
            ?: return badRequest("Missing 'protocol' parameter")

        val protocolRequest: CreateCarReceptionCommand = objectMapper.readValue(protocolJson, CreateCarReceptionCommand::class.java)

        validateCarReceptionRequest(protocolRequest)

        val command = mapToCreateProtocolCommand(protocolRequest)
        val createdProtocolId = commandBus.execute(command)

        processUploadedImages(request, protocolRequest.vehicleImages, createdProtocolId)

        return created(ProtocolIdResponse(createdProtocolId))
    }

    @PostMapping("/{protocolId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadPhoto(@PathVariable protocolId: String, request: MultipartHttpServletRequest): ResponseEntity<ProtocolIdResponse> {
        val imageDetailsJson = request.getParameter("image")
            ?: return badRequest("Missing 'image' parameter")

        val imageCommand: CreateVehicleImageCommand = objectMapper.readValue(imageDetailsJson, CreateVehicleImageCommand::class.java)

        val file = request.fileMap.values.firstOrNull()
            ?: return badRequest("No file uploaded")

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
        return created(ProtocolIdResponse(protocolId))
    }

    @PatchMapping("/{protocolId}/image/{imageId}")
    fun updateImageMetadata(
        @PathVariable protocolId: String,
        @PathVariable imageId: String,
        @RequestBody updateCommand: UpdateVehicleImageCommand
    ): ResponseEntity<VehicleImageResponse> {
        val command = UpdateVisitMediaCommand(
            visitId = protocolId,
            mediaId = imageId,
            name = updateCommand.name ?: "Unknown",
            description = updateCommand.description,
            location = updateCommand.location,
            tags = updateCommand.tags
        )

        commandBus.execute(command)

        val response = VehicleImageResponse(
            id = imageId,
            name = updateCommand.name ?: "Unknown",
            size = 0L,
            tags = updateCommand.tags,
            type = "PHOTO",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            description = updateCommand.description,
            location = updateCommand.location,
        )

        return ResponseEntity.ok(response)
    }

    @PutMapping("/{protocolId}/services")
    fun updateServices(
        @PathVariable protocolId: String,
        @RequestBody command: ServicesUpdateCommand
    ): ResponseEntity<ProtocolIdResponse> {
        val updateCommand = UpdateProtocolServicesCommand(
            protocolId = protocolId,
            services = command.services.map { ProtocolApiMappers.toCreateServiceCommand(it) }
        )

        commandBus.execute(updateCommand)
        return created(ProtocolIdResponse(protocolId))
    }

    @GetMapping("/image/{fileId}")
    fun getImage(@PathVariable fileId: String): ResponseEntity<Resource> {
        val query = GetMediaFileQuery(fileId)
        val fileData = queryBus.execute(query)
            ?: return ResponseEntity.notFound().build()

        val resource = ByteArrayResource(fileData.data)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(fileData.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${fileData.originalName}\"")
            .body(resource)
    }

    @GetMapping("/protocols/{protocolId}/images")
    fun getProtocolImages(@PathVariable protocolId: String): ResponseEntity<List<ImageDTO>> {
        val query = GetVisitMediaQuery(protocolId)
        val images = queryBus.execute(query)
            .map { media ->
                ImageDTO(
                    id = media.id,
                    name = media.name,
                    size = media.size,
                    type = media.contentType,
                    createdAt = media.createdAt,
                    protocolId = protocolId,
                    description = media.description,
                    location = media.location
                )
            }

        return ResponseEntity.ok(images)
    }

    @DeleteMapping("/{protocolId}/image/{imageId}")
    fun deleteCarReceptionProtocolImage(
        @PathVariable protocolId: String,
        @PathVariable imageId: String,
    ): ResponseEntity<Map<String, Any>> {
        val command = DeleteVisitMediaCommand(
            visitId = protocolId,
            mediaId = imageId
        )

        commandBus.execute(command)
        return ok(createSuccessResponse("Image successfully deleted", mapOf("protocolId" to protocolId, "imageId" to imageId)))
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
        val domainStatus = status?.let { ProtocolStatus.valueOf(it) }

        val query = SearchProtocolsQuery(
            clientName = clientName,
            licensePlate = licensePlate,
            make = make,
            status = domainStatus,
            startDate = parseDateTimeParam(startDate),
            endDate = parseDateTimeParam(endDate),
            page = page,
            size = size
        )

        val paginatedProtocols = queryBus.execute(query)

        val response = PaginatedResponse(
            data = paginatedProtocols.data.map { convertToCarReceptionListDto(it) },
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

        return ok(response)
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
        val domainStatus = status?.let { ProtocolStatus.valueOf(it) }

        val query = SearchProtocolsQuery(
            clientName = clientName,
            licensePlate = licensePlate,
            status = domainStatus,
            startDate = parseDateTimeParam(startDate),
            endDate = parseDateTimeParam(endDate),
            page = 0,
            size = 1000
        )

        val protocols = queryBus.execute(query)
        val response = protocols.data.map { convertToCarReceptionListDto(it) }
        return ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get car reception protocol by ID", description = "Retrieves a specific car reception protocol by its ID")
    fun getCarReceptionProtocolById(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String
    ): ResponseEntity<CarReceptionDetailDto> {
        val query = GetProtocolByIdQuery(id)
        val protocol = queryBus.execute(query)
            ?: throw ResourceNotFoundException("Protocol", id)

        return ok(convertToCarReceptionDetailDto(protocol))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update car reception protocol", description = "Updates an existing car reception protocol")
    fun updateCarReceptionProtocol(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @Valid @RequestBody command: UpdateCarReceptionCommand
    ): ResponseEntity<CarReceptionDetailDto> {
        validateCarReceptionRequest(command)

        if (command.id != id) {
            throw ValidationException("Protocol ID in path ($id) does not match ID in request body (${command.id})")
        }

        val updateCommand = mapToUpdateProtocolCommand(command, id)
        commandBus.execute(updateCommand)

        val query = GetProtocolByIdQuery(id)
        val updatedProtocol = queryBus.execute(query)
            ?: throw ResourceNotFoundException("Protocol", id)

        val response = convertToCarReceptionDetailDto(updatedProtocol)
        return ok(response)
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update protocol status", description = "Updates the status of a car reception protocol")
    fun updateProtocolStatus(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @RequestBody command: UpdateStatusCommand
    ): ResponseEntity<CarReceptionBasicDto> {
        val statusCommand = ChangeProtocolStatusCommand(
            protocolId = id,
            newStatus = mapApiStatusToDomain(command.status)
        )

        commandBus.execute(statusCommand)

        val query = GetProtocolByIdQuery(id)
        val updatedProtocol = queryBus.execute(query)
            ?: throw ResourceNotFoundException("Protocol", id)

        val response = CarReceptionBasicDto(
            id = updatedProtocol.id,
            createdAt = updatedProtocol.audit.createdAt,
            updatedAt = updatedProtocol.audit.updatedAt,
            statusUpdatedAt = updatedProtocol.audit.statusUpdatedAt,
            status = mapDomainStatusToApi(ProtocolStatus.valueOf(updatedProtocol.status))
        )

        return ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete car reception protocol", description = "Deletes a car reception protocol by its ID")
    fun deleteCarReceptionProtocol(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        val command = DeleteProtocolCommand(id)
        commandBus.execute(command)
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
        val domainStatus = status?.let { ProtocolStatus.valueOf(it) }

        val query = GetClientProtocolHistoryQuery(
            clientId = clientId,
            status = domainStatus
        )

        val protocols = queryBus.execute(query)
        val response = protocols.map { convertToClientProtocolHistoryDto(it) }
        return ok(response)
    }

    @PostMapping("/{protocolId}/document", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload protocol document", description = "Upload document to protocol (agreements, contracts)")
    fun uploadProtocolDocument(
        @Parameter(description = "Protocol ID", required = true) @PathVariable protocolId: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("documentType") documentType: String,
        @RequestParam(value = "description", required = false) description: String?
    ): ResponseEntity<Map<String, Any>> {
        if (file.isEmpty) {
            return badRequest("File cannot be empty")
        }

        val allowedTypes = setOf("application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

        if (file.contentType !in allowedTypes) {
            return badRequest("Only PDF, DOC, and DOCX files are allowed")
        }

        if (file.size > 10 * 1024 * 1024) {
            return badRequest("File size cannot exceed 10MB")
        }

        val command = UploadProtocolDocumentCommand(
            protocolId = protocolId,
            file = file,
            documentType = documentType,
            description = description
        )

        val storageId = commandBus.execute(command)

        return created(createSuccessResponse("Document uploaded successfully",
            mapOf(
                "storageId" to storageId,
                "protocolId" to protocolId,
                "documentType" to documentType
            )
        ))
    }

    @GetMapping("/{protocolId}/documents")
    @Operation(summary = "Get protocol documents", description = "Get all documents for protocol")
    fun getProtocolDocuments(
        @Parameter(description = "Protocol ID", required = true) @PathVariable protocolId: String
    ): ResponseEntity<List<ProtocolDocumentDto>> {
        val query = GetProtocolDocumentsQuery(protocolId)
        val documents = queryBus.execute(query)
        val response = documents.map { ProtocolDocumentDto.fromQueryResponse(it) }
        return ok(response)
    }

    @GetMapping("/document/{documentId}")
    @Operation(summary = "Download protocol document", description = "Download protocol document")
    fun downloadProtocolDocument(
        @Parameter(description = "Document ID", required = true) @PathVariable documentId: String
    ): ResponseEntity<Resource> {
        val query = GetProtocolDocumentQuery(documentId)
        val documentData = queryBus.execute(query)
            ?: return ResponseEntity.notFound().build()

        val resource = ByteArrayResource(documentData.data)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(documentData.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${documentData.originalName}\"")
            .body(resource)
    }

    @DeleteMapping("/{protocolId}/document/{documentId}")
    @Operation(summary = "Delete protocol document", description = "Delete protocol document")
    fun deleteProtocolDocument(
        @Parameter(description = "Protocol ID", required = true) @PathVariable protocolId: String,
        @Parameter(description = "Document ID", required = true) @PathVariable documentId: String
    ): ResponseEntity<Map<String, Any>> {
        val command = DeleteProtocolDocumentCommand(protocolId, documentId)
        commandBus.execute(command)
        return ok(createSuccessResponse("Document deleted successfully",
            mapOf("documentId" to documentId)))
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release vehicle to client", description = "Completes protocol by releasing vehicle to client with payment details")
    fun releaseVehicle(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @Valid @RequestBody command: com.carslab.crm.modules.visits.api.commands.ReleaseVehicleCommand
    ): ResponseEntity<CarReceptionDetailDto> {
        val releaseCommand = com.carslab.crm.modules.visits.application.commands.models.ReleaseVehicleCommand(
            protocolId = id,
            paymentMethod = command.paymentMethod,
            documentType = command.documentType,
            additionalNotes = command.additionalNotes
        )

        commandBus.execute(releaseCommand)

        val query = GetProtocolByIdQuery(id)
        val updatedProtocol = queryBus.execute(query)
            ?: throw ResourceNotFoundException("Protocol", id)

        val response = convertToCarReceptionDetailDto(updatedProtocol)
        return ok(response)
    }

    private fun processUploadedImages(
        request: MultipartHttpServletRequest,
        vehicleImages: List<CreateVehicleImageCommand>?,
        protocolId: String
    ) {
        if (vehicleImages.isNullOrEmpty()) return

        request.fileMap.forEach { (paramName, file) ->
            val index = extractImageIndex(paramName) ?: return@forEach

            val imageRequest = vehicleImages.getOrNull(index)
            if (imageRequest != null && imageRequest.hasFile && index < vehicleImages.size) {
                val command = UploadVisitMediaCommand(
                    visitId = protocolId,
                    file = file,
                    mediaDetails = MediaDetailsCommand(
                        name = imageRequest.name ?: file.originalFilename ?: "image_${System.currentTimeMillis()}",
                        description = imageRequest.description,
                        location = imageRequest.location,
                        tags = imageRequest.tags,
                        type = imageRequest.type ?: "PHOTO"
                    )
                )

                commandBus.execute(command)
            }
        }
    }

    private fun extractImageIndex(paramName: String): Int? {
        val imageIndexRegex = """images\[(\d+)\]""".toRegex()
        val matchResult = imageIndexRegex.find(paramName) ?: return null
        return matchResult.groupValues[1].toInt()
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

        try {
            LocalDateTime.parse(command.startDate, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            try {
                val localDate = LocalDate.parse(command.startDate, DateTimeFormatter.ISO_DATE)
                LocalDateTime.of(localDate, LocalTime.of(8, 0))
            } catch (e2: Exception) {
                throw ValidationException("Invalid start date format. Use ISO format (YYYY-MM-DD'T'HH:MM:SS)")
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
                val date = LocalDate.parse(dateTimeString, DateTimeFormatter.ISO_DATE)
                LocalDateTime.of(date, LocalTime.of(0, 0))
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun mapApiStatusToDomain(apiStatus: com.carslab.crm.api.model.ApiProtocolStatus): ProtocolStatus {
        return when (apiStatus) {
            com.carslab.crm.api.model.ApiProtocolStatus.SCHEDULED -> ProtocolStatus.SCHEDULED
            com.carslab.crm.api.model.ApiProtocolStatus.PENDING_APPROVAL -> ProtocolStatus.PENDING_APPROVAL
            com.carslab.crm.api.model.ApiProtocolStatus.IN_PROGRESS -> ProtocolStatus.IN_PROGRESS
            com.carslab.crm.api.model.ApiProtocolStatus.READY_FOR_PICKUP -> ProtocolStatus.READY_FOR_PICKUP
            com.carslab.crm.api.model.ApiProtocolStatus.COMPLETED -> ProtocolStatus.COMPLETED
            com.carslab.crm.api.model.ApiProtocolStatus.CANCELLED -> ProtocolStatus.CANCELLED
        }
    }

    private fun mapDomainStatusToApi(domainStatus: ProtocolStatus): com.carslab.crm.api.model.ApiProtocolStatus {
        return when (domainStatus) {
            ProtocolStatus.SCHEDULED -> com.carslab.crm.api.model.ApiProtocolStatus.SCHEDULED
            ProtocolStatus.PENDING_APPROVAL -> com.carslab.crm.api.model.ApiProtocolStatus.PENDING_APPROVAL
            ProtocolStatus.IN_PROGRESS -> com.carslab.crm.api.model.ApiProtocolStatus.IN_PROGRESS
            ProtocolStatus.READY_FOR_PICKUP -> com.carslab.crm.api.model.ApiProtocolStatus.READY_FOR_PICKUP
            ProtocolStatus.COMPLETED -> com.carslab.crm.api.model.ApiProtocolStatus.COMPLETED
            ProtocolStatus.CANCELLED -> com.carslab.crm.api.model.ApiProtocolStatus.CANCELLED
        }
    }

    private fun convertToCarReceptionListDto(readModel: ProtocolListReadModel): CarReceptionListDto {
        return CarReceptionListDto(
            id = readModel.id,
            title = readModel.title,
            vehicle = VehicleBasicDto(
                make = readModel.vehicle.make,
                model = readModel.vehicle.model,
                licensePlate = readModel.vehicle.licensePlate,
                productionYear = readModel.vehicle.productionYear,
                color = readModel.vehicle.color
            ),
            calendarColorId = readModel.calendarColorId,
            period = PeriodDto(
                startDate = readModel.period.startDate,
                endDate = readModel.period.endDate
            ),
            owner = OwnerBasicDto(
                name = readModel.client.name,
                companyName = readModel.client.companyName
            ),
            status = mapDomainStatusToApi(ProtocolStatus.valueOf(readModel.status)),
            totalServiceCount = readModel.totalServiceCount,
            totalAmount = readModel.totalAmount,
            selectedServices = emptyList(),
            lastUpdate = readModel.lastUpdate
        )
    }

    private fun convertToCarReceptionDetailDto(readModel: ProtocolDetailReadModel): CarReceptionDetailDto {
        return CarReceptionDetailDto(
            id = readModel.id,
            title = readModel.title,
            calendarColorId = readModel.calendarColorId,
            startDate = readModel.period.startDate,
            endDate = readModel.period.endDate,
            licensePlate = readModel.vehicle.licensePlate,
            make = readModel.vehicle.make,
            model = readModel.vehicle.model,
            productionYear = readModel.vehicle.productionYear,
            mileage = readModel.vehicle.mileage,
            vin = readModel.vehicle.vin,
            color = readModel.vehicle.color,
            keysProvided = readModel.documents.keysProvided,
            documentsProvided = readModel.documents.documentsProvided,
            ownerId = readModel.client.id?.toLongOrNull(),
            ownerName = readModel.client.name,
            companyName = readModel.client.companyName,
            taxId = readModel.client.taxId,
            email = readModel.client.email,
            phone = readModel.client.phone,
            notes = readModel.notes,
            selectedServices = readModel.services.map { service ->
                ServiceDto(
                    id = service.id,
                    name = service.name,
                    price = service.basePrice,
                    discountType = service.discountType?.let {
                        com.carslab.crm.modules.visits.api.request.ApiDiscountType.valueOf(it)
                    },
                    discountValue = service.discountValue,
                    finalPrice = service.finalPrice,
                    approvalStatus = com.carslab.crm.modules.visits.api.request.ServiceApprovalStatus.valueOf(service.approvalStatus),
                    note = service.note,
                    quantity = service.quantity
                )
            },
            status = mapDomainStatusToApi(ProtocolStatus.valueOf(readModel.status)),
            referralSource = readModel.referralSource?.let {
                com.carslab.crm.modules.visits.api.request.ApiReferralSource.valueOf(it)
            },
            otherSourceDetails = readModel.otherSourceDetails,
            vehicleImages = readModel.mediaItems.map { media ->
                VehicleImageDto(
                    id = media.id,
                    name = media.name,
                    size = media.size,
                    type = media.type,
                    storageId = media.id,
                    createdAt = Instant.now(),
                    description = media.description,
                    location = media.location,
                    tags = media.tags
                )
            },
            createdAt = readModel.audit.createdAt,
            updatedAt = readModel.audit.updatedAt,
            statusUpdatedAt = readModel.audit.statusUpdatedAt,
            appointmentId = readModel.appointmentId
        )
    }

    private fun mapToCreateProtocolCommand(request: CreateCarReceptionCommand): CreateProtocolCommand {
        return CreateProtocolCommand(
            title = request.title,
            calendarColorId = request.calendarColorId,
            vehicle = CreateVehicleCommand(
                brand = request.make,
                model = request.model,
                licensePlate = request.licensePlate,
                productionYear = request.productionYear,
                vin = request.vin,
                color = request.color,
                mileage = request.mileage
            ),
            client = CreateClientCommand(
                name = request.ownerName,
                email = request.email,
                phone = request.phone,
                companyName = request.companyName,
                taxId = request.taxId
            ),
            period = CreatePeriodCommand(
                startDate = parseDateTime(request.startDate),
                endDate = request.endDate?.let { parseDateTime(it) } ?: parseDateTime(request.startDate).plusHours(8)
            ),
            status = request.status?.let { mapApiStatusToDomain(it) } ?: ProtocolStatus.SCHEDULED,
            services = request.selectedServices?.map { ProtocolApiMappers.toCreateServiceCommand(it) } ?: emptyList(),
            notes = request.notes,
            referralSource = request.referralSource?.name,
            documents = CreateDocumentsCommand(
                keysProvided = request.keysProvided ?: false,
                documentsProvided = request.documentsProvided ?: false
            ),
            appointmentId = request.appointmentId
        )
    }

    private fun mapToUpdateProtocolCommand(request: UpdateCarReceptionCommand, protocolId: String): UpdateProtocolCommand {
        return UpdateProtocolCommand(
            protocolId = protocolId,
            title = request.title,
            calendarColorId = request.calendarColorId,
            vehicle = UpdateVehicleCommand(
                id = protocolId,
                brand = request.make,
                model = request.model,
                licensePlate = request.licensePlate ?: "",
                productionYear = request.productionYear,
                vin = request.vin,
                color = request.color,
                mileage = request.mileage
            ),
            client = UpdateClientCommand(
                id = protocolId,
                name = request.ownerName,
                email = request.email,
                phone = request.phone,
                companyName = request.companyName,
                taxId = request.taxId
            ),
            period = UpdatePeriodCommand(
                startDate = parseDateTime(request.startDate),
                endDate = request.endDate?.let { parseDateTime(it) } ?: parseDateTime(request.startDate).plusHours(8)
            ),
            status = request.status?.let { mapApiStatusToDomain(it) } ?: ProtocolStatus.SCHEDULED,
            services = request.selectedServices?.map { service ->
                com.carslab.crm.modules.visits.application.commands.models.valueobjects.UpdateServiceCommand(
                    id = service.id,
                    name = service.name,
                    basePrice = service.price,
                    quantity = service.quantity,
                    discountType = service.discountType?.name,
                    discountValue = service.discountValue,
                    finalPrice = service.finalPrice,
                    approvalStatus = service.approvalStatus?.name ?: "PENDING",
                    note = service.note
                )
            } ?: emptyList(),
            notes = request.notes,
            referralSource = request.referralSource?.name,
            documents = UpdateDocumentsCommand(
                keysProvided = request.keysProvided ?: false,
                documentsProvided = request.documentsProvided ?: false
            ),
            appointmentId = request.appointmentId
        )
    }

    private fun parseDateTime(dateTimeString: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            try {
                val date = LocalDate.parse(dateTimeString, DateTimeFormatter.ISO_DATE)
                LocalDateTime.of(date, LocalTime.of(8, 0))
            } catch (e2: Exception) {
                throw IllegalArgumentException("Invalid date format: $dateTimeString")
            }
        }
    }

    private fun convertToClientProtocolHistoryDto(readModel: ProtocolListReadModel): ClientProtocolHistoryDto {
        return ClientProtocolHistoryDto(
            id = readModel.id,
            startDate = readModel.period.startDate,
            endDate = readModel.period.endDate,
            status = mapDomainStatusToApi(ProtocolStatus.valueOf(readModel.status)),
            carMake = readModel.vehicle.make,
            carModel = readModel.vehicle.model,
            licensePlate = readModel.vehicle.licensePlate,
            totalAmount = readModel.totalAmount
        )
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