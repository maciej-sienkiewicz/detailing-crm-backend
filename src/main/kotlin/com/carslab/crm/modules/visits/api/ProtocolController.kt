package com.carslab.crm.modules.visits.api

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.modules.visits.api.dto.*
import com.carslab.crm.modules.visits.api.mappers.ProtocolApiMappers
import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.infrastructure.cqrs.CommandBus
import com.carslab.crm.infrastructure.cqrs.QueryBus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/v1/protocols")
@Tag(name = "Protocol Management", description = "Car reception protocol management")
class ProtocolController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus, 
    ) {

    @PostMapping
    @Operation(summary = "Create new protocol")
    fun createProtocol(@Valid @RequestBody request: CreateProtocolRequest): ResponseEntity<ProtocolIdResponse> {
        val command = ProtocolApiMappers.toCreateCommand(request)
        val protocolId: String = commandBus.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(ProtocolIdResponse(protocolId))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get protocol by ID")
    fun getProtocol(@PathVariable id: String): ResponseEntity<ProtocolDetailResponse> {
        val query = GetProtocolByIdQuery(id)
        val readModel = queryBus.execute(query) ?: return ResponseEntity.notFound().build()
        val response = ProtocolApiMappers.toDetailResponse(readModel)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Operation(summary = "Search protocols")
    fun searchProtocols(
        @RequestParam(required = false) clientName: String?,
        @RequestParam(required = false) licensePlate: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PaginatedResponse<ProtocolListResponse>> {
        val query = SearchProtocolsQuery(
            clientName = clientName,
            licensePlate = licensePlate,
            status = status?.let { ProtocolStatus.valueOf(it) },
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
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change protocol status")
    fun changeStatus(
        @PathVariable id: String,
        @RequestBody request: ChangeStatusRequest
    ): ResponseEntity<Unit> {
        val command = ChangeProtocolStatusCommand(
            protocolId = id,
            newStatus = ProtocolStatus.valueOf(request.status),
            reason = request.reason
        )
        commandBus.execute(command)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete protocol")
    fun deleteProtocol(@PathVariable id: String): ResponseEntity<Unit> {
        val command = DeleteProtocolCommand(id)
        commandBus.execute(command)
        return ResponseEntity.noContent().build()
    }
}

data class ChangeStatusRequest(
    val status: String,
    val reason: String? = null
)