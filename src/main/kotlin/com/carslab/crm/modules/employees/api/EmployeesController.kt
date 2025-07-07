// src/main/kotlin/com/carslab/crm/modules/employees/api/EmployeesController.kt
package com.carslab.crm.modules.employees.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.employees.api.dto.*
import com.carslab.crm.modules.employees.application.commands.models.*
import com.carslab.crm.modules.employees.application.queries.models.*
import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.infrastructure.cqrs.CommandBus
import com.carslab.crm.infrastructure.cqrs.QueryBus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employee Management", description = "Employee management endpoints")
class EmployeesController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus
) : BaseController() {

    @GetMapping("/list")
    @Operation(summary = "Get paginated employees list")
    fun getEmployeesList(
        @Parameter(description = "Search query")
        @RequestParam(required = false) searchQuery: String?,

        @Parameter(description = "Position filter")
        @RequestParam(required = false) position: String?,

        @Parameter(description = "Role filter")
        @RequestParam(required = false) role: UserRole?,

        @Parameter(description = "Active status filter")
        @RequestParam(required = false) isActive: Boolean?,

        @Parameter(description = "Contract type filter")
        @RequestParam(required = false) contractType: ContractType?,

        @Parameter(description = "Sort field")
        @RequestParam(defaultValue = "fullName") sortBy: String,

        @Parameter(description = "Sort order")
        @RequestParam(defaultValue = "asc") sortOrder: String,

        @Parameter(description = "Page number")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedResponse<EmployeeListItemDto>> {

        logger.info("Fetching employees list with filters: role=$role, isActive=$isActive")

        try {
            val query = GetEmployeesQuery(
                searchQuery = searchQuery,
                position = position,
                role = role,
                isActive = isActive,
                contractType = contractType,
                sortBy = sortBy,
                sortOrder = sortOrder,
                page = page,
                size = size
            )

            val result = queryBus.execute(query)
            val response = PaginatedResponse(
                data = result.data.map { EmployeeListItemDto.from(it) },
                page = result.page,
                size = result.size,
                totalItems = result.totalItems,
                totalPages = result.totalPages
            )

            return ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error fetching employees list", e)
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID")
    fun getEmployeeById(
        @Parameter(description = "Employee ID", required = true)
        @PathVariable id: String
    ): ResponseEntity<ExtendedEmployeeDto> {

        logger.info("Fetching employee by ID: $id")

        try {
            val query = GetEmployeeByIdQuery(id)
            val result = queryBus.execute(query)

            return if (result != null) {
                ok(ExtendedEmployeeDto.from(result))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            return logAndRethrow("Error fetching employee $id", e)
        }
    }

    @PostMapping
    @Operation(summary = "Create new employee")
    fun createEmployee(
        @Valid @RequestBody payload: EmployeeCreatePayloadDto
    ): ResponseEntity<Map<String, Any>> {

        logger.info("Creating employee: ${payload.fullName}")

        try {
            val emergencyContact = payload.emergencyContact?.let {
                EmergencyContact(it.name, it.phone)
            }

            val command = CreateEmployeeCommand(
                fullName = payload.fullName,
                birthDate = payload.birthDate,
                hireDate = payload.hireDate,
                position = payload.position,
                email = payload.email,
                phone = payload.phone,
                role = payload.role,
                hourlyRate = payload.hourlyRate,
                bonusFromRevenue = payload.bonusFromRevenue,
                isActive = payload.isActive,
                workingHoursPerWeek = payload.workingHoursPerWeek,
                contractType = payload.contractType,
                emergencyContact = emergencyContact,
                notes = payload.notes
            )

            val employeeId = commandBus.execute(command)

            logger.info("Successfully created employee: $employeeId")
            return created(createSuccessResponse("Employee created successfully", mapOf("id" to employeeId)))
        } catch (e: Exception) {
            return logAndRethrow("Error creating employee", e)
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee")
    fun updateEmployee(
        @PathVariable id: String,
        @Valid @RequestBody payload: EmployeeUpdatePayloadDto
    ): ResponseEntity<Map<String, Any>> {

        logger.info("Updating employee: $id")

        try {
            val emergencyContact = payload.emergencyContact?.let {
                EmergencyContact(it.name, it.phone)
            }

            val command = UpdateEmployeeCommand(
                id = id,
                fullName = payload.fullName,
                birthDate = payload.birthDate,
                hireDate = payload.hireDate,
                position = payload.position,
                email = payload.email,
                phone = payload.phone,
                role = payload.role,
                hourlyRate = payload.hourlyRate,
                bonusFromRevenue = payload.bonusFromRevenue,
                isActive = payload.isActive,
                workingHoursPerWeek = payload.workingHoursPerWeek,
                contractType = payload.contractType,
                emergencyContact = emergencyContact,
                notes = payload.notes
            )

            val employeeId = commandBus.execute(command)

            logger.info("Successfully updated employee: $employeeId")
            return ok(createSuccessResponse("Employee updated successfully", mapOf("id" to employeeId)))
        } catch (e: Exception) {
            return logAndRethrow("Error updating employee $id", e)
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee")
    fun deleteEmployee(
        @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {

        logger.info("Deleting employee: $id")

        try {
            val command = DeleteEmployeeCommand(id)
            val deleted = commandBus.execute(command)

            return if (deleted) {
                logger.info("Successfully deleted employee: $id")
                ok(createSuccessResponse("Employee deleted successfully", mapOf("id" to id)))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            return logAndRethrow("Error deleting employee $id", e)
        }
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "Get employee documents")
    fun getEmployeeDocuments(
        @PathVariable id: String
    ): ResponseEntity<List<EmployeeDocumentDto>> {

        logger.info("Fetching documents for employee: $id")

        try {
            val query = GetEmployeeDocumentsQuery(id)
            val result = queryBus.execute(query)

            return ok(result.map { EmployeeDocumentDto.from(it) })
        } catch (e: Exception) {
            return logAndRethrow("Error fetching documents for employee $id", e)
        }
    }

    @PostMapping("/documents", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload employee document")
    fun uploadEmployeeDocument(
        @RequestParam("employeeId") employeeId: String,
        @RequestParam("name") name: String,
        @RequestParam("type") type: String,
        @RequestParam(value = "file", required = false) file: MultipartFile?
    ): ResponseEntity<Map<String, Any>> {

        logger.info("Uploading document for employee: $employeeId")

        try {
            // TODO: Handle file upload to storage service and get URL
            val fileUrl = file?.let { "https://storage.example.com/documents/${java.util.UUID.randomUUID()}" }
            val fileSize = file?.size
            val mimeType = file?.contentType

            val command = CreateEmployeeDocumentCommand(
                employeeId = employeeId,
                name = name,
                type = type,
                fileUrl = fileUrl,
                fileSize = fileSize,
                mimeType = mimeType
            )

            val documentId = commandBus.execute(command)

            logger.info("Successfully uploaded document: $documentId")
            return created(createSuccessResponse("Document uploaded successfully", mapOf("id" to documentId)))
        } catch (e: Exception) {
            return logAndRethrow("Error uploading document for employee $employeeId", e)
        }
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "Delete employee document")
    fun deleteEmployeeDocument(
        @PathVariable documentId: String
    ): ResponseEntity<Map<String, Any>> {

        logger.info("Deleting document: $documentId")

        try {
            val command = DeleteEmployeeDocumentCommand(documentId)
            val deleted = commandBus.execute(command)

            return if (deleted) {
                logger.info("Successfully deleted document: $documentId")
                ok(createSuccessResponse("Document deleted successfully", mapOf("id" to documentId)))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            return logAndRethrow("Error deleting document $documentId", e)
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get employee statistics")
    fun getEmployeeStatistics(): ResponseEntity<EmployeeStatisticsDto> {

        logger.info("Fetching employee statistics")

        try {
            val query = GetEmployeeStatisticsQuery()
            val result = queryBus.execute(query)

            return ok(EmployeeStatisticsDto.from(result))
        } catch (e: Exception) {
            return logAndRethrow("Error fetching employee statistics", e)
        }
    }

    @PostMapping("/bulk-update")
    @Operation(summary = "Bulk update employees")
    fun bulkUpdateEmployees(
        @Valid @RequestBody updates: List<EmployeeUpdatePayloadDto>
    ): ResponseEntity<Map<String, Any>> {

        logger.info("Bulk updating ${updates.size} employees")

        try {
            val results = mutableListOf<String>()
            val errors = mutableListOf<String>()

            updates.forEach { payload ->
                try {
                    val emergencyContact = payload.emergencyContact?.let {
                        EmergencyContact(it.name, it.phone)
                    }

                    val command = UpdateEmployeeCommand(
                        id = payload.id,
                        fullName = payload.fullName,
                        birthDate = payload.birthDate,
                        hireDate = payload.hireDate,
                        position = payload.position,
                        email = payload.email,
                        phone = payload.phone,
                        role = payload.role,
                        hourlyRate = payload.hourlyRate,
                        bonusFromRevenue = payload.bonusFromRevenue,
                        isActive = payload.isActive,
                        workingHoursPerWeek = payload.workingHoursPerWeek,
                        contractType = payload.contractType,
                        emergencyContact = emergencyContact,
                        notes = payload.notes
                    )

                    val employeeId = commandBus.execute(command)
                    results.add(employeeId)
                } catch (e: Exception) {
                    logger.error("Failed to update employee ${payload.id}: ${e.message}")
                    errors.add("Employee ${payload.id}: ${e.message}")
                }
            }

            val response = mutableMapOf<String, Any>(
                "updated" to results.size,
                "failed" to errors.size,
                "updatedIds" to results
            )

            if (errors.isNotEmpty()) {
                response["errors"] = errors
            }

            logger.info("Bulk update completed: ${results.size} updated, ${errors.size} failed")
            return ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error in bulk update", e)
        }
    }

    @GetMapping("/export")
    @Operation(summary = "Export employees to various formats")
    fun exportEmployees(
        @Parameter(description = "Export format (csv, xlsx, pdf)")
        @RequestParam(defaultValue = "csv") format: String,

        @Parameter(description = "Search query")
        @RequestParam(required = false) searchQuery: String?,

        @Parameter(description = "Position filter")
        @RequestParam(required = false) position: String?,

        @Parameter(description = "Role filter")
        @RequestParam(required = false) role: UserRole?,

        @Parameter(description = "Active status filter")
        @RequestParam(required = false) isActive: Boolean?,

        @Parameter(description = "Contract type filter")
        @RequestParam(required = false) contractType: ContractType?
    ): ResponseEntity<ByteArray> {

        logger.info("Exporting employees in format: $format")

        try {
            val query = ExportEmployeesQuery(
                format = format,
                searchQuery = searchQuery,
                position = position,
                role = role,
                isActive = isActive,
                contractType = contractType
            )

            val result = queryBus.execute(query)

            val contentType = when (format.lowercase()) {
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "pdf" -> "application/pdf"
                else -> "text/csv"
            }

            val fileName = "employees_export_${java.time.LocalDate.now()}.$format"

            return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .header("Content-Disposition", "attachment; filename=\"$fileName\"")
                .body(result)
        } catch (e: Exception) {
            logger.error("Error exporting employees in format $format", e)
            throw e
        }
    }
}