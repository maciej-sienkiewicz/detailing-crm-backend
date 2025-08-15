package com.carslab.crm.production.modules.activities.presentation

import com.carslab.crm.production.modules.activities.application.dto.ActivityPageResponse
import com.carslab.crm.production.modules.activities.application.dto.ActivityResponse
import com.carslab.crm.production.modules.activities.application.service.ActivityQueryService
import com.carslab.crm.production.shared.presentation.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/activities")
@Tag(name = "Activities", description = "Activity tracking and monitoring")
class ActivityControllerNew(
    private val queryService: ActivityQueryService
) : BaseController() {
    
    @GetMapping
    @Operation(summary = "Get paginated activities for current company")
    fun getActivities(
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<ActivityPageResponse> {
        val activities = queryService.getActivitiesForCurrentCompany(page, size)
        return ok(activities)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID")
    fun getActivity(
        @PathVariable id: String
    ): ResponseEntity<ActivityResponse> {
        val activity = queryService.getActivity(id)
        return if (activity != null) {
            ok(activity)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}