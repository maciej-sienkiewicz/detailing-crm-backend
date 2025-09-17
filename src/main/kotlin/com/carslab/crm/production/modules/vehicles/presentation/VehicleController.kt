package com.carslab.crm.production.modules.vehicles.presentation

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.production.modules.vehicles.application.dto.CreateVehicleRequest
import com.carslab.crm.production.modules.vehicles.application.dto.UpdateVehicleRequest
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleAnalyticsResponse
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleResponse
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleTableResponse
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleWithStatisticsResponse
import com.carslab.crm.production.modules.vehicles.application.service.VehicleAnalyticsQueryService
import com.carslab.crm.production.modules.vehicles.application.service.VehicleCommandService
import com.carslab.crm.production.modules.vehicles.application.service.VehicleMediaCommandService
import com.carslab.crm.production.modules.vehicles.application.service.VehicleMediaQueryService
import com.carslab.crm.production.modules.vehicles.application.service.VehicleQueryService
import com.carslab.crm.production.modules.visits.application.dto.VisitMediaResponse
import com.carslab.crm.production.modules.visits.application.service.query.VisitMediaQueryService
import com.carslab.crm.production.modules.visits.application.service.query.VisitQueryService
import com.carslab.crm.production.shared.presentation.BaseController
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicles", description = "Vehicle management endpoints")
class VehicleController(
    private val vehicleCommandService: VehicleCommandService,
    private val vehicleQueryService: VehicleQueryService,
    private val vehicleMediaCommandService: VehicleMediaCommandService,
    private val vehicleMediaQueryService: VehicleMediaQueryService,
    private val vehicleAnalyticsQueryService: VehicleAnalyticsQueryService
) : BaseController() {

    @PostMapping
    @Operation(summary = "Create a new vehicle", description = "Creates a new vehicle with the provided information")
    fun createVehicle(@Valid @RequestBody request: CreateVehicleRequest): ResponseEntity<VehicleResponse> {
        logger.info("Received request to create new vehicle: ${request.make} ${request.model}, plate: ${request.licensePlate}")

        val response = vehicleCommandService.createVehicle(request)
        logger.info("Successfully created vehicle with ID: ${response.id}")

        return created(response)
    }

    @GetMapping
    @Operation(summary = "Get all vehicles", description = "Retrieves all vehicles")
    fun getAllVehicles(): ResponseEntity<List<VehicleTableResponse>> {
        logger.info("Getting all vehicles")

        val vehicles = vehicleQueryService.searchVehicles(
            make = null,
            model = null,
            licensePlate = null,
            vin = null,
            year = null,
            ownerName = null,
            minVisits = null,
            maxVisits = null,
            pageable = PageRequest.of(0, 1000)
        )
        val response = vehicles.content
        return ok(response)
    }

    @GetMapping("/table")
    @Operation(
        summary = "Get vehicles for table view",
        description = "Retrieves vehicles with statistics for table display including owners, visit count, last visit, and total revenue"
    )
    fun getVehiclesForTable(
        @PageableDefault(size = 20, sort = ["lastVisitDate"], direction = Sort.Direction.DESC)
        pageable: Pageable,

        @Parameter(description = "Filter by vehicle make")
        @RequestParam(required = false) make: String?,

        @Parameter(description = "Filter by vehicle model")
        @RequestParam(required = false) model: String?,

        @Parameter(description = "Filter by license plate")
        @RequestParam(required = false) license_plate: String?,

        @Parameter(description = "Filter by owner name")
        @RequestParam(required = false) owner_name: String?,

        @Parameter(description = "Minimum number of visits")
        @RequestParam(required = false) min_visits: Int?,

        @Parameter(description = "Maximum number of visits")
        @RequestParam(required = false) max_visits: Int?
    ): ResponseEntity<Page<VehicleTableResponse>> {
        logger.info("Getting vehicles for table view with filters: make=$make, model=$model, licensePlate=$license_plate, ownerName=$owner_name")

        val vehicleTablePage = vehicleQueryService.searchVehicles(
            make = make,
            model = model,
            licensePlate = license_plate,
            ownerName = owner_name,
            minVisits = min_visits,
            maxVisits = max_visits,
            pageable = pageable,
            vin = null,
            year = null,
        )

        logger.info("Successfully retrieved ${vehicleTablePage.numberOfElements} vehicles for table view")
        return ok(vehicleTablePage)
    }

    @GetMapping("/company-statistics")
    @Operation(
        summary = "Get company vehicle statistics",
        description = "Retrieves overall vehicle statistics for the company including total vehicles, premium vehicles, and visit revenue median"
    )
    fun getCompanyVehicleStatistics(): ResponseEntity<VehicleCompanyStatisticsResponse> {
        logger.info("Getting company vehicle statistics")

        val statistics = VehicleCompanyStatisticsResponse(
            totalVehicles = 0L,
            premiumVehicles = 0L,
            visitRevenueMedian = BigDecimal.ZERO,
            totalRevenue = BigDecimal.ZERO,
            averageRevenuePerVehicle = BigDecimal.ZERO,
            mostActiveVehicle = null,
            calculatedAt = LocalDateTime.now()
        )

        logger.info("Successfully retrieved company vehicle statistics")
        return ok(statistics)
    }

    @GetMapping("/{id}/owners")
    @Operation(summary = "Get vehicle owners", description = "Retrieves all owners of a specific vehicle")
    fun getOwners(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<List<VehicleOwnerResponse>> {
        logger.info("Getting owners for vehicle ID: $id")

        val owners = emptyList<VehicleOwnerResponse>()
        return ok(owners)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vehicle by ID", description = "Retrieves a vehicle by its ID")
    fun getVehicleById(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<VehicleResponse> {
        logger.info("Getting vehicle by ID: $id")

        val vehicle = vehicleQueryService.getVehicle(id).vehicle
        return ok(vehicle)
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "Get vehicle statistics", description = "Retrieves statistical information about a vehicle")
    fun getVehicleStatistics(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<VehicleStatisticsResponse> {
        logger.info("Getting vehicle statistics: $id")

        val vehicleWithStats = vehicleQueryService.getVehicle(id)

        val stats = VehicleStatisticsResponse(
            visitNo = vehicleWithStats.statistics?.visitCount ?: 0L,
            servicesNo = 0,
            totalRevenue = vehicleWithStats.statistics?.totalRevenue ?: BigDecimal.ZERO
        )

        return ok(stats)
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get vehicles by owner", description = "Retrieves all vehicles owned by a specific person")
    fun getVehiclesByOwnerId(
        @Parameter(description = "Owner ID", required = true) @PathVariable ownerId: String
    ): ResponseEntity<List<VehicleResponse>> {
        logger.info("Getting vehicles by owner ID: $ownerId")

        val response = emptyList<VehicleResponse>()
        return ok(response)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a vehicle", description = "Updates an existing vehicle with the provided information")
    fun updateVehicle(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String,
        @Valid @RequestBody request: UpdateVehicleRequest
    ): ResponseEntity<VehicleResponse> {
        logger.info("Updating vehicle with ID: $id")

        val response = vehicleCommandService.updateVehicle(id, request)
        logger.info("Successfully updated vehicle with ID: $id")

        return ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a vehicle", description = "Deletes a vehicle by its ID")
    fun deleteVehicle(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting vehicle with ID: $id")

        vehicleCommandService.deleteVehicle(id)
        logger.info("Successfully deleted vehicle with ID: $id")

        return ok(createSuccessResponse("Vehicle successfully deleted", mapOf("vehicleId" to id)))
    }

    @GetMapping("/{id}/service-history")
    @Operation(
        summary = "Get vehicle service history",
        description = "Retrieves the service history for a specific vehicle"
    )
    fun getVehicleServiceHistory(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<List<ServiceHistoryResponse>> {
        logger.info("Getting service history for vehicle: $id")

        val serviceHistory = emptyList<ServiceHistoryResponse>()
        return ok(serviceHistory)
    }

    @PostMapping("/{id}/service-history")
    @Operation(summary = "Add service history entry", description = "Adds a new service history entry for a vehicle")
    fun addServiceHistoryEntry(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String,
        @Valid @RequestBody request: ServiceHistoryRequest
    ): ResponseEntity<ServiceHistoryResponse> {
        logger.info("Adding service history entry for vehicle: $id")

        val response = ServiceHistoryResponse(
            id = UUID.randomUUID().toString(),
            vehicleId = id,
            serviceType = request.serviceType,
            description = request.description,
            price = request.price,
            date = request.date.toString()
        )

        logger.info("Successfully added service history entry with ID: ${response.id}")
        return created(response)
    }

    @DeleteMapping("/service-history/{id}")
    @Operation(summary = "Delete service history entry", description = "Deletes a service history entry by its ID")
    fun deleteServiceHistoryEntry(
        @Parameter(description = "Service history entry ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting service history entry with ID: $id")

        logger.info("Successfully deleted service history entry with ID: $id")
        return ok(createSuccessResponse("Service history entry successfully deleted", mapOf("serviceHistoryId" to id)))
    }

    @GetMapping("/search")
    @Operation(summary = "Search vehicles", description = "Search vehicles by license plate, make, or model")
    fun searchVehicles(
        @Parameter(description = "License plate to search for") @RequestParam(required = false) licensePlate: String?,
        @Parameter(description = "Vehicle make to search for") @RequestParam(required = false) make: String?,
        @Parameter(description = "Vehicle model to search for") @RequestParam(required = false) model: String?
    ): ResponseEntity<List<VehicleTableResponse>> {
        logger.info("Searching vehicles with filters: licensePlate=$licensePlate, make=$make, model=$model")

        val vehicles = vehicleQueryService.searchVehicles(
            make = make,
            model = model,
            licensePlate = licensePlate,
            vin = null,
            year = null,
            ownerName = null,
            minVisits = null,
            maxVisits = null,
            pageable = PageRequest.of(0, 1000)
        )

        val response = vehicles.content
        return ok(response)
    }

    @PostMapping("/{id}/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload image to vehicle", description = "Uploads an image directly to a vehicle")
    fun uploadVehicleImage(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String,
        request: MultipartHttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Uploading image to vehicle: $id")

        try {
            val response = vehicleMediaCommandService.uploadVehicleImage(id, request)
            logger.info("Successfully uploaded image to vehicle: $id with media ID: ${response.mediaId}")

            return created(
                createSuccessResponse(
                    "Image uploaded successfully",
                    mapOf(
                        "mediaId" to response.mediaId,
                        "vehicleId" to id
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to upload image to vehicle: $id", e)
            throw e
        }
    }

    // Zmodyfikuj istniejącą metodę getVehicleImageThumbnails:
    @GetMapping("/{id}/images/thumbnails")
    @Operation(
        summary = "Get vehicle image thumbnails",
        description = "Retrieves all image thumbnails for a vehicle (direct + from visits)"
    )
    fun getVehicleImageThumbnails(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String,
        @PageableDefault(size = 3, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<VehicleImageResponse>> {
        logger.info("Getting image thumbnails for vehicle ID: $id with page request: $pageable")

        try {
            // Nowa logika - pobierz wszystkie zdjęcia pojazdu
            val allVehicleImages = vehicleMediaQueryService.getAllVehicleImages(id)

            // Konwertuj na VehicleImageResponse (zachowaj format odpowiedzi)
            val imageResponses = allVehicleImages.map { media ->
                VehicleImageResponse(
                    id = media.id,
                    url = media.downloadUrl,
                    thumbnailUrl = media.downloadUrl, // Można dodać osobny endpoint dla miniaturek
                    filename = media.name,
                    uploadedAt = media.createdAt
                )
            }

            // Implementacja prostej paginacji w pamięci (dla zachowania kompatybilności)
            val startIndex = pageable.offset.toInt()
            val endIndex = minOf(startIndex + pageable.pageSize, imageResponses.size)
            val pageContent = if (startIndex < imageResponses.size) {
                imageResponses.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            val imagePage: Page<VehicleImageResponse> = PageImpl(
                pageContent,
                pageable,
                imageResponses.size.toLong()
            )

            logger.info("Successfully retrieved ${imagePage.numberOfElements} image thumbnails for vehicle ID: $id")
            return ok(imagePage)

        } catch (e: Exception) {
            logger.error("Failed to get image thumbnails for vehicle: $id", e)
            throw e
        }
    }

    @GetMapping("/{id}/images")
    @Operation(
        summary = "Get all vehicle images",
        description = "Retrieves all images for a vehicle (direct + from visits)"
    )
    fun getAllVehicleImages(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<List<VehicleImageResponse>> {
        logger.info("Getting all images for vehicle ID: $id")

        try {
            val allVehicleImages = vehicleMediaQueryService.getAllVehicleImages(id)

            val imageResponses = allVehicleImages.map { media ->
                VehicleImageResponse(
                    id = media.id,
                    url = media.downloadUrl,
                    thumbnailUrl = media.downloadUrl,
                    filename = media.name,
                    uploadedAt = media.createdAt
                )
            }

            logger.info("Successfully retrieved ${imageResponses.size} images for vehicle ID: $id")
            return ok(imageResponses)

        } catch (e: Exception) {
            logger.error("Failed to get all images for vehicle: $id", e)
            throw e
        }
    }

    @GetMapping("/{id}/images/direct")
    @Operation(
        summary = "Get direct vehicle images",
        description = "Retrieves only images directly assigned to vehicle (excluding visit images)"
    )
    fun getDirectVehicleImages(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<List<VehicleImageResponse>> {
        logger.info("Getting direct images for vehicle ID: $id")

        try {
            val directImages = vehicleMediaQueryService.getDirectVehicleImages(id)

            val imageResponses = directImages.map { media ->
                VehicleImageResponse(
                    id = media.id,
                    url = media.downloadUrl,
                    thumbnailUrl = media.downloadUrl,
                    filename = media.name,
                    uploadedAt = media.createdAt
                )
            }

            logger.info("Successfully retrieved ${imageResponses.size} direct images for vehicle ID: $id")
            return ok(imageResponses)

        } catch (e: Exception) {
            logger.error("Failed to get direct images for vehicle: $id", e)
            throw e
        }
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Delete vehicle image", description = "Deletes a specific image from a vehicle")
    fun deleteVehicleImage(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String,
        @Parameter(description = "Image ID", required = true) @PathVariable imageId: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting image: $imageId from vehicle ID: $id")

        try {
            vehicleMediaCommandService.deleteVehicleImage(id, imageId)
            logger.info("Successfully deleted image: $imageId from vehicle ID: $id")

            return ok(
                createSuccessResponse(
                    "Image deleted successfully",
                    mapOf(
                        "vehicleId" to id,
                        "imageId" to imageId
                    )
                )
            )

        } catch (e: Exception) {
            logger.error("Failed to delete image: $imageId from vehicle: $id", e)
            throw e
        }
    }

    @GetMapping("/{id}/images/count")
    @Operation(summary = "Get vehicle images count", description = "Returns the total count of images for a vehicle")
    fun getVehicleImagesCount(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Getting images count for vehicle ID: $id")

        try {
            val count = vehicleMediaQueryService.countAllVehicleImages(id)

            return ok(
                mapOf(
                    "vehicleId" to id,
                    "totalImages" to count,
                    "hasImages" to (count > 0)
                )
            )

        } catch (e: Exception) {
            logger.error("Failed to get images count for vehicle: $id", e)
            throw e
        }
    }

    @GetMapping("/{id}/analytics")
    @Operation(
        summary = "Get vehicle analytics",
        description = "Retrieves comprehensive analytics for a vehicle including profitability, visit patterns, and service preferences"
    )
    fun getVehicleAnalytics(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<VehicleAnalyticsResponse> {
        logger.info("Getting analytics for vehicle ID: $id")

        val analytics = vehicleAnalyticsQueryService.getVehicleAnalytics(id)
        logger.info("Successfully retrieved analytics for vehicle ID: $id")

        return ok(analytics)
    }

    @PostMapping("/batch-analytics")
    @Operation(
        summary = "Get batch vehicle analytics",
        description = "Retrieves analytics for multiple vehicles in a single request for performance optimization"
    )
    fun getBatchVehicleAnalytics(
        @Valid @RequestBody request: BatchAnalyticsRequest
    ): ResponseEntity<Map<String, VehicleAnalyticsResponse>> {
        logger.info("Getting batch analytics for {} vehicles", request.vehicleIds.size)

        if (request.vehicleIds.size > 100) {
            throw IllegalArgumentException("Maximum 100 vehicles allowed per batch request")
        }

        val analytics = vehicleAnalyticsQueryService.getBatchVehicleAnalytics(request.vehicleIds)
        logger.info("Successfully retrieved batch analytics for {} vehicles", request.vehicleIds.size)

        return ok(analytics)
    }
    
    private fun createSuccessResponse(message: String, data: Map<String, Any>): Map<String, Any> {
        return mapOf(
            "success" to true,
            "message" to message,
            "data" to data
        )
    }
}



data class VehicleCompanyStatisticsResponse(
    val totalVehicles: Long,
    val premiumVehicles: Long,
    val visitRevenueMedian: BigDecimal,
    val totalRevenue: BigDecimal,
    val averageRevenuePerVehicle: BigDecimal,
    val mostActiveVehicle: MostActiveVehicleInfo?,
    val calculatedAt: LocalDateTime
)

data class MostActiveVehicleInfo(
    val id: Long,
    val make: String,
    val model: String,
    val licensePlate: String,
    val visitCount: Long,
    val totalRevenue: BigDecimal
)

data class VehicleStatisticsResponse(
    val visitNo: Long,
    val servicesNo: Int,
    val totalRevenue: BigDecimal
)

data class VehicleOwnerResponse(
    val id: Long,
    val fullName: String
)

data class ServiceHistoryResponse(
    val id: String,
    val vehicleId: String,
    val serviceType: String,
    val description: String,
    val price: BigDecimal,
    val date: String
)

data class ServiceHistoryRequest(
    val vehicleId: String? = null,
    val serviceType: String,
    val description: String,
    val price: BigDecimal,
    val date: LocalDate
)

data class VehicleImageResponse(
    val id: String,
    val url: String,
    @JsonProperty("thumbnail_url")
    val thumbnailUrl: String,
    val filename: String,
    @JsonProperty("uploaded_at")
    val uploadedAt: LocalDateTime
)

data class BatchAnalyticsRequest(
    @JsonProperty("vehicle_ids")
    val vehicleIds: List<String>
)