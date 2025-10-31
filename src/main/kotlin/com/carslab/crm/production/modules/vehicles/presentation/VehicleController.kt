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
import com.carslab.crm.production.shared.observability.annotations.HttpMonitored
import com.carslab.crm.production.shared.presentation.BaseController
import com.carslab.crm.production.shared.presentation.dto.PriceResponseDto
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
    @HttpMonitored(endpoint = "POST_/api/vehicles")
    @Operation(summary = "Create a new vehicle", description = "Creates a new vehicle with the provided information")
    fun createVehicle(@Valid @RequestBody request: CreateVehicleRequest): ResponseEntity<VehicleResponse> {
        logger.info("Received request to create new vehicle: ${request.make} ${request.model}, plate: ${request.licensePlate}")

        val response = vehicleCommandService.createVehicle(request)
        logger.info("Successfully created vehicle with ID: ${response.id}")

        return created(response)
    }

    @GetMapping
    @HttpMonitored(endpoint = "GET_/api/vehicles")
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
    @HttpMonitored(endpoint = "GET_/api/vehicles/table")
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

    @GetMapping("/{id}")
    @HttpMonitored(endpoint = "GET_/api/vehicles/{id}")
    @Operation(summary = "Get vehicle by ID", description = "Retrieves a vehicle by its ID")
    fun getVehicleById(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<VehicleResponse> {
        logger.info("Getting vehicle by ID: $id")

        val vehicle = vehicleQueryService.getVehicle(id).vehicle
        return ok(vehicle)
    }

    @PutMapping("/{id}")
    @HttpMonitored(endpoint = "PUT_/api/vehicles/{id}")
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
    @HttpMonitored(endpoint = "DELETE_/api/vehicles/{id}")
    @Operation(summary = "Delete a vehicle", description = "Deletes a vehicle by its ID")
    fun deleteVehicle(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting vehicle with ID: $id")

        vehicleCommandService.deleteVehicle(id)
        logger.info("Successfully deleted vehicle with ID: $id")

        return ok(createSuccessResponse("Vehicle successfully deleted", mapOf("vehicleId" to id)))
    }

    @GetMapping("/search")
    @HttpMonitored(endpoint = "GET_/api/vehicles/search")
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

    @GetMapping("/{id}/statistics")
    @HttpMonitored(endpoint = "GET_/api/vehicles/{id}/statistics")
    @Operation(summary = "Get vehicle statistics", description = "Retrieves statistical information about a vehicle")
    fun getVehicleStatistics(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<VehicleStatisticsResponse> {
        logger.info("Getting vehicle statistics: $id")

        val vehicleWithStats = vehicleQueryService.getVehicle(id)

        val stats = VehicleStatisticsResponse(
            visitNo = vehicleWithStats.statistics?.visitCount ?: 0L,
            servicesNo = 0,
            totalRevenue = PriceResponseDto(
                priceNetto = vehicleWithStats.statistics?.totalRevenue?.priceNetto ?: BigDecimal.ZERO,
                priceBrutto = vehicleWithStats.statistics?.totalRevenue?.priceBrutto ?: BigDecimal.ZERO,
                taxAmount = vehicleWithStats.statistics?.totalRevenue?.taxAmount ?: BigDecimal.ZERO,
            )
        )

        return ok(stats)
    }

    @GetMapping("/{id}/analytics")
    @HttpMonitored(endpoint = "GET_/api/vehicles/{id}/analytics")
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
    @HttpMonitored(endpoint = "POST_/api/vehicles/batch-analytics")
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

    @PostMapping("/{id}/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @HttpMonitored(endpoint = "POST_/api/vehicles/{id}/images")
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

    @GetMapping("/{id}/images/thumbnails")
    @HttpMonitored(endpoint = "GET_/api/vehicles/{id}/images/thumbnails")
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

    @DeleteMapping("/{id}/images/{imageId}")
    @HttpMonitored(endpoint = "DELETE_/api/vehicles/{id}/images/{imageId}")
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

    private fun createSuccessResponse(message: String, data: Map<String, Any>): Map<String, Any> {
        return mapOf(
            "success" to true,
            "message" to message,
            "data" to data
        )
    }
}

data class VehicleStatisticsResponse(
    val visitNo: Long,
    val servicesNo: Int,
    val totalRevenue: PriceResponseDto
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