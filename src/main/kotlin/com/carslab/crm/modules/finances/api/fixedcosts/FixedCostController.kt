package com.carslab.crm.finances.api.fixedcosts

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.CategorySummaryResponse
import com.carslab.crm.api.model.CreateFixedCostRequest
import com.carslab.crm.api.model.FixedCostFilterDTO
import com.carslab.crm.api.model.FixedCostPaymentResponse
import com.carslab.crm.api.model.FixedCostResponse
import com.carslab.crm.api.model.RecordPaymentRequest
import com.carslab.crm.api.model.SupplierInfoDTO
import com.carslab.crm.api.model.UpcomingPaymentsResponse
import com.carslab.crm.api.model.UpdateFixedCostRequest
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.finances.domain.FixedCostService
import com.carslab.crm.finances.domain.model.fixedcosts.CostFrequency
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCost
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostCategory
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostPayment
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostStatus
import com.carslab.crm.finances.domain.model.fixedcosts.PaymentStatus
import com.carslab.crm.modules.email.domain.services.EmailSendingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

@RestController
@RequestMapping("/api/fixed-costs")
@Tag(name = "Fixed Costs", description = "API endpoints for fixed costs management")
class FixedCostController(
    private val fixedCostService: FixedCostService,
) : BaseController() {

    @GetMapping
    @Operation(summary = "Get all fixed costs", description = "Retrieves all fixed costs with optional filtering and pagination")
    fun getAllFixedCosts(
        @Parameter(description = "Cost name") @RequestParam(required = false) name: String?,
        @Parameter(description = "Cost category") @RequestParam(required = false) category: String?,
        @Parameter(description = "Cost status") @RequestParam(required = false) status: String?,
        @Parameter(description = "Cost frequency") @RequestParam(required = false) frequency: String?,
        @Parameter(description = "Supplier name") @RequestParam(required = false) supplierName: String?,
        @Parameter(description = "Contract number") @RequestParam(required = false) contractNumber: String?,
        @Parameter(description = "Start date from") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDateFrom: LocalDate?,
        @Parameter(description = "Start date to") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDateTo: LocalDate?,
        @Parameter(description = "End date from") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDateFrom: LocalDate?,
        @Parameter(description = "End date to") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDateTo: LocalDate?,
        @Parameter(description = "Minimum amount") @RequestParam(required = false) minAmount: BigDecimal?,
        @Parameter(description = "Maximum amount") @RequestParam(required = false) maxAmount: BigDecimal?,
        @Parameter(description = "Active in period") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) activeInPeriod: LocalDate?,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedResponse<FixedCostResponse>> {
        logger.info("Getting all fixed costs with filters")
        
        val filter = FixedCostFilterDTO(
            name = name,
            category = category?.let { runCatching { FixedCostCategory.valueOf(it) }.getOrNull() },
            status = status?.let { runCatching { FixedCostStatus.valueOf(it) }.getOrNull() },
            frequency = frequency?.let { runCatching { CostFrequency.valueOf(it) }.getOrNull() },
            supplierName = supplierName,
            contractNumber = contractNumber,
            startDateFrom = startDateFrom,
            startDateTo = startDateTo,
            endDateFrom = endDateFrom,
            endDateTo = endDateTo,
            minAmount = minAmount,
            maxAmount = maxAmount,
            activeInPeriod = activeInPeriod
        )

        val paginatedFixedCosts = fixedCostService.getAllFixedCosts(filter, page, size)
        val response = PaginatedResponse(
            data = paginatedFixedCosts.data.map { it.toResponse() },
            page = paginatedFixedCosts.page,
            size = paginatedFixedCosts.size,
            totalItems = paginatedFixedCosts.totalItems,
            totalPages = paginatedFixedCosts.totalPages.toLong()
        )

        return ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fixed cost by ID", description = "Retrieves a fixed cost by its ID")
    fun getFixedCostById(
        @Parameter(description = "Fixed cost ID", required = true) @PathVariable id: String
    ): ResponseEntity<FixedCostResponse> {
        logger.info("Getting fixed cost by ID: {}", id)

        val fixedCost = fixedCostService.getFixedCostById(id)
        val response = fixedCost.toResponse()

        return ok(response)
    }

    @PostMapping
    @Operation(summary = "Create a new fixed cost", description = "Creates a new fixed cost")
    fun createFixedCost(
        @Parameter(description = "Fixed cost data", required = true)
        @RequestBody @Valid request: CreateFixedCostRequest
    ): ResponseEntity<FixedCostResponse> {
        logger.info("Creating new fixed cost: {}", request.name)

        val createdFixedCost = fixedCostService.createFixedCost(request)
        val response = createdFixedCost.toResponse()

        return created(response)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a fixed cost", description = "Updates an existing fixed cost")
    fun updateFixedCost(
        @Parameter(description = "Fixed cost ID", required = true) @PathVariable id: String,
        @Parameter(description = "Fixed cost data", required = true)
        @RequestBody @Valid request: UpdateFixedCostRequest
    ): ResponseEntity<FixedCostResponse> {
        logger.info("Updating fixed cost with ID: {}", id)

        val updatedFixedCost = fixedCostService.updateFixedCost(id, request)
        val response = updatedFixedCost.toResponse()

        return ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a fixed cost", description = "Deletes a fixed cost by its ID")
    fun deleteFixedCost(
        @Parameter(description = "Fixed cost ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting fixed cost with ID: {}", id)

        val deleted = fixedCostService.deleteFixedCost(id)

        return if (deleted) {
            ok(createSuccessResponse("Fixed cost successfully deleted", mapOf("fixedCostId" to id)))
        } else {
            badRequest("Failed to delete fixed cost")
        }
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "Record a payment", description = "Records a payment for a fixed cost")
    fun recordPayment(
        @Parameter(description = "Fixed cost ID", required = true) @PathVariable id: String,
        @Parameter(description = "Payment data", required = true)
        @RequestBody @Valid request: RecordPaymentRequest
    ): ResponseEntity<FixedCostPaymentResponse> {
        logger.info("Recording payment for fixed cost ID: {}", id)

        val recordedPayment = fixedCostService.recordPayment(id, request)
        val response = recordedPayment.toResponse()

        return created(response)
    }

    @GetMapping("/categories/summary")
    @Operation(summary = "Get category summary", description = "Retrieves summary of fixed costs by category")
    fun getCategorySummary(
        @Parameter(description = "Analysis period") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) period: LocalDate?
    ): ResponseEntity<CategorySummaryResponse> {
        logger.info("Getting category summary for period: {}", period)

        val summary = fixedCostService.getCategorySummary(period)

        return ok(summary)
    }

    @GetMapping("/payments/upcoming")
    @Operation(summary = "Get upcoming payments", description = "Retrieves upcoming payments for fixed costs")
    fun getUpcomingPayments(
        @Parameter(description = "Number of days to look ahead") @RequestParam(defaultValue = "30") days: Int
    ): ResponseEntity<UpcomingPaymentsResponse> {
        logger.info("Getting upcoming payments for next {} days", days)

        val upcomingPayments = fixedCostService.getUpcomingPayments(days)

        return ok(upcomingPayments)
    }

    @GetMapping("/{id}/payments")
    @Operation(summary = "Get payments for fixed cost", description = "Retrieves payments for a specific fixed cost in a period")
    fun getPaymentsForPeriod(
        @Parameter(description = "Fixed cost ID", required = true) @PathVariable id: String,
        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<FixedCostPaymentResponse>> {
        logger.info("Getting payments for fixed cost {} in period {} to {}", id, startDate, endDate)

        val payments = fixedCostService.getPaymentsForPeriod(id, startDate, endDate)
        val response = payments.map { it.toResponse() }

        return ok(response)
    }

    @GetMapping("/active")
    @Operation(summary = "Get active fixed costs in period", description = "Retrieves active fixed costs for a specific period")
    fun getActiveFixedCostsInPeriod(
        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<FixedCostResponse>> {
        logger.info("Getting active fixed costs in period {} to {}", startDate, endDate)

        val activeCosts = fixedCostService.getActiveFixedCostsInPeriod(startDate, endDate)
        val response = activeCosts.map { it.toResponse() }

        return ok(response)
    }

    @GetMapping("/total")
    @Operation(summary = "Calculate total fixed costs", description = "Calculates total fixed costs for a specific period")
    fun calculateTotalFixedCosts(
        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Calculating total fixed costs for period {} to {}", startDate, endDate)

        val totalCosts = fixedCostService.calculateTotalFixedCostsForPeriod(startDate, endDate)

        return ok(mapOf(
            "startDate" to startDate,
            "endDate" to endDate,
            "totalFixedCosts" to totalCosts,
            "period" to "${startDate} to ${endDate}"
        ))
    }

    // Helper function to convert domain model to response DTO
    private fun FixedCost.toResponse(): FixedCostResponse {
        val totalPaid = payments.filter { it.status == PaymentStatus.PAID }
            .sumOf { it.amount }
        val totalPlanned = payments.sumOf { it.plannedAmount }
        val lastPaymentDate = payments.maxByOrNull { it.paymentDate }?.paymentDate
        val nextPaymentDate = calculateNextPaymentDate() // TODO: Implement based on frequency

        return FixedCostResponse(
            id = id.value,
            name = name,
            description = description,
            category = category,
            categoryDisplay = category.displayName,
            monthlyAmount = monthlyAmount,
            frequency = frequency,
            frequencyDisplay = frequency.displayName,
            startDate = startDate,
            endDate = endDate,
            status = status,
            statusDisplay = status.displayName,
            autoRenew = autoRenew,
            supplierInfo = supplierInfo?.let {
                SupplierInfoDTO(name = it.name, taxId = it.taxId)
            },
            contractNumber = contractNumber,
            notes = notes,
            calculatedMonthlyAmount = calculateMonthlyAmount(),
            totalPaid = totalPaid,
            totalPlanned = totalPlanned,
            lastPaymentDate = lastPaymentDate,
            nextPaymentDate = nextPaymentDate,
            paymentsCount = payments.size,
            isActiveInCurrentMonth = isActiveInPeriod(LocalDate.now()),
            createdAt = audit.createdAt,
            updatedAt = audit.updatedAt,
            payments = payments.map { it.toResponse() }
        )
    }

    // Helper function to convert payment domain model to response DTO
    private fun FixedCostPayment.toResponse(): FixedCostPaymentResponse {
        return FixedCostPaymentResponse(
            id = id,
            paymentDate = paymentDate,
            amount = amount,
            plannedAmount = plannedAmount,
            variance = amount.subtract(plannedAmount),
            status = status,
            statusDisplay = status.displayName,
            paymentMethod = paymentMethod?.name,
            paymentMethodDisplay = paymentMethod?.name, // TODO: Add display names to PaymentMethod
            documentId = documentId,
            notes = notes,
            createdAt = createdAt
        )
    }

    // TODO: Implement next payment date calculation based on frequency
    private fun FixedCost.calculateNextPaymentDate(): LocalDate? {
        val lastPayment = payments.maxByOrNull { it.paymentDate }
        if (lastPayment == null) return startDate

        return when (frequency) {
            CostFrequency.MONTHLY -> lastPayment.paymentDate.plusMonths(1)
            CostFrequency.QUARTERLY -> lastPayment.paymentDate.plusMonths(3)
            CostFrequency.YEARLY -> lastPayment.paymentDate.plusYears(1)
            CostFrequency.WEEKLY -> lastPayment.paymentDate.plusWeeks(1)
            CostFrequency.ONE_TIME -> null
        }
    }
}