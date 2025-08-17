package com.carslab.crm.production.modules.visits.api

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.visits.api.commands.CarReceptionDetailDto
import com.carslab.crm.modules.visits.application.queries.models.VisitListReadModel
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.application.service.query.VisitListQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/protocols")
@Tag(name = "Visit List Operations", description = "List and detailed view operations for visits")
class VisitListController(
    private val visitListQueryService: VisitListQueryService,
    private val visitDetailQueryService: VisitDetailQueryService
) {

    @GetMapping("/list")
    @Operation(summary = "Get visits with pagination")
    fun getVisits(
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") sortBy: String,
        @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") sortDirection: String
    ): ResponseEntity<PaginatedResponse<VisitListReadModel>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy)
        val pageable = PageRequest.of(page, size, sort)
        val visits = visitListQueryService.getVisitList(pageable)
        return ResponseEntity.ok(visits)
    }

    @GetMapping("/{visitId}/detail")
    @Operation(summary = "Get detailed visit information")
    fun getVisitDetail(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<CarReceptionDetailDto> {
        val visit = visitDetailQueryService.getVisitDetail(visitId)
        return ResponseEntity.ok(visit)
    }
}