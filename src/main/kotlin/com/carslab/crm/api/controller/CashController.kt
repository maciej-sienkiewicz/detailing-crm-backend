package com.carslab.crm.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.CashTransactionFilterDTO
import com.carslab.crm.api.model.request.CreateCashTransactionRequest
import com.carslab.crm.api.model.request.UpdateCashTransactionRequest
import com.carslab.crm.api.model.response.CashStatisticsResponse
import com.carslab.crm.api.model.response.CashTransactionResponse
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.domain.CashService
import com.carslab.crm.domain.model.view.finance.CashStatistics
import com.carslab.crm.domain.model.view.finance.CashTransaction
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@RestController
@RequestMapping("/api/cash")
@CrossOrigin(origins = ["*"])
@Tag(name = "Cash", description = "API endpoints for cash management")
class CashController(
    private val cashService: CashService
) : BaseController() {

    @GetMapping("/transactions")
    @Operation(summary = "Get all cash transactions", description = "Retrieves all cash transactions with optional filtering and pagination")
    fun getAllTransactions(
        @Parameter(description = "Transaction type (INCOME/EXPENSE)") @RequestParam(required = false) type: String?,
        @Parameter(description = "Transaction description") @RequestParam(required = false) description: String?,
        @Parameter(description = "Date from") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateFrom: LocalDate?,
        @Parameter(description = "Date to") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateTo: LocalDate?,
        @Parameter(description = "Visit ID") @RequestParam(required = false) visitId: String?,
        @Parameter(description = "Invoice ID") @RequestParam(required = false) invoiceId: String?,
        @Parameter(description = "Minimum amount") @RequestParam(required = false) minAmount: BigDecimal?,
        @Parameter(description = "Maximum amount") @RequestParam(required = false) maxAmount: BigDecimal?,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PaginatedResponse<CashTransactionResponse>> {
        logger.info("Getting all cash transactions with filters")

        val filter = CashTransactionFilterDTO(
            type = type,
            description = description,
            dateFrom = dateFrom,
            dateTo = dateTo,
            visitId = visitId,
            invoiceId = invoiceId,
            minAmount = minAmount,
            maxAmount = maxAmount
        )

        val paginatedTransactions = cashService.getAllCashTransactions(filter, page, size)
        val response = PaginatedResponse(
            data = paginatedTransactions.data.map { it.toResponse() },
            page = paginatedTransactions.page,
            size = paginatedTransactions.size,
            totalItems = paginatedTransactions.totalItems,
            totalPages = paginatedTransactions.totalPages
        )

        return ok(response)
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Get cash transaction by ID", description = "Retrieves a cash transaction by its ID")
    fun getTransactionById(
        @Parameter(description = "Transaction ID", required = true) @PathVariable id: String
    ): ResponseEntity<CashTransactionResponse> {
        logger.info("Getting cash transaction by ID: {}", id)

        val transaction = cashService.getCashTransactionById(id)
        val response = transaction.toResponse()

        return ok(response)
    }

    @PostMapping("/transactions")
    @Operation(summary = "Create a new cash transaction", description = "Creates a new cash transaction")
    fun createTransaction(
        @Parameter(description = "Transaction data", required = true)
        @RequestBody @Valid request: CreateCashTransactionRequest
    ): ResponseEntity<CashTransactionResponse> {
        logger.info("Creating new cash transaction: {}", request.description)

        val createdTransaction = cashService.createCashTransaction(request)
        val response = createdTransaction.toResponse()

        return created(response)
    }

    @PutMapping("/transactions/{id}")
    @Operation(summary = "Update a cash transaction", description = "Updates an existing cash transaction")
    fun updateTransaction(
        @Parameter(description = "Transaction ID", required = true) @PathVariable id: String,
        @Parameter(description = "Transaction data", required = true)
        @RequestBody @Valid request: UpdateCashTransactionRequest
    ): ResponseEntity<CashTransactionResponse> {
        logger.info("Updating cash transaction with ID: {}", id)

        val updatedTransaction = cashService.updateCashTransaction(id, request)
        val response = updatedTransaction.toResponse()

        return ok(response)
    }

    @DeleteMapping("/transactions/{id}")
    @Operation(summary = "Delete a cash transaction", description = "Deletes a cash transaction by its ID")
    fun deleteTransaction(
        @Parameter(description = "Transaction ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting cash transaction with ID: {}", id)

        val deleted = cashService.deleteCashTransaction(id)

        return if (deleted) {
            ok(createSuccessResponse("Transaction successfully deleted", mapOf("transactionId" to id)))
        } else {
            badRequest("Failed to delete transaction")
        }
    }

    @GetMapping("/balance")
    @Operation(summary = "Get current cash balance", description = "Retrieves the current cash balance")
    fun getCurrentBalance(): ResponseEntity<Map<String, Any>> {
        logger.info("Getting current cash balance")

        val balance = cashService.getCurrentBalance()

        return ok(mapOf(
            "balance" to balance,
            "date" to LocalDate.now()
        ))
    }

    @GetMapping("/statistics/current-month")
    @Operation(summary = "Get current month statistics", description = "Retrieves cash statistics for the current month")
    fun getCurrentMonthStatistics(): ResponseEntity<CashStatisticsResponse> {
        logger.info("Getting cash statistics for current month")

        val statistics = cashService.getCurrentMonthStatistics()
        val response = statistics.toResponse()

        return ok(response)
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get statistics for period", description = "Retrieves cash statistics for a specific period")
    fun getStatisticsForPeriod(
        @Parameter(description = "Start date", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "End date", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<CashStatisticsResponse> {
        logger.info("Getting cash statistics for period: {} to {}", startDate, endDate)

        val statistics = cashService.getStatisticsForPeriod(startDate, endDate)
        val response = statistics.toResponse()

        return ok(response)
    }

    @GetMapping("/statistics/{year}/{month}")
    @Operation(summary = "Get statistics for month", description = "Retrieves cash statistics for a specific month")
    fun getStatisticsForMonth(
        @Parameter(description = "Year", required = true) @PathVariable year: Int,
        @Parameter(description = "Month", required = true) @PathVariable month: Int
    ): ResponseEntity<CashStatisticsResponse> {
        logger.info("Getting cash statistics for month: {}/{}", month, year)

        val statistics = cashService.getStatisticsForMonth(year, month)
        val response = statistics.toResponse()

        return ok(response)
    }

    @GetMapping("/transactions/visit/{visitId}")
    @Operation(summary = "Get transactions by visit ID", description = "Retrieves all cash transactions related to a visit")
    fun getTransactionsByVisitId(
        @Parameter(description = "Visit ID", required = true) @PathVariable visitId: String
    ): ResponseEntity<List<CashTransactionResponse>> {
        logger.info("Getting cash transactions for visit ID: {}", visitId)

        val transactions = cashService.getTransactionsByVisitId(visitId)
        val response = transactions.map { it.toResponse() }

        return ok(response)
    }

    @GetMapping("/transactions/invoice/{invoiceId}")
    @Operation(summary = "Get transactions by invoice ID", description = "Retrieves all cash transactions related to an invoice")
    fun getTransactionsByInvoiceId(
        @Parameter(description = "Invoice ID", required = true) @PathVariable invoiceId: String
    ): ResponseEntity<List<CashTransactionResponse>> {
        logger.info("Getting cash transactions for invoice ID: {}", invoiceId)

        val transactions = cashService.getTransactionsByInvoiceId(invoiceId)
        val response = transactions.map { it.toResponse() }

        return ok(response)
    }

    // Helper function to convert domain model to response DTO
    private fun CashTransaction.toResponse(): CashTransactionResponse {
        return CashTransactionResponse(
            id = id.value,
            type = type.name,
            description = description,
            date = date,
            amount = amount,
            visitId = visitId,
            visitNumber = visitNumber,
            invoiceId = invoiceId,
            invoiceNumber = invoiceNumber,
            createdBy = createdBy.value,
            createdAt = audit.createdAt,
            updatedAt = audit.updatedAt
        )
    }

    // Helper function to convert statistics domain model to response DTO
    private fun CashStatistics.toResponse(): CashStatisticsResponse {
        return CashStatisticsResponse(
            periodStart = periodStart,
            periodEnd = periodEnd,
            income = income,
            expense = expense,
            balance = balance,
            transactionCount = transactionCount
        )
    }
}