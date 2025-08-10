package com.carslab.crm.modules.finances.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.finances.api.mappers.BalanceHistoryMapper
import com.carslab.crm.modules.finances.api.responses.BalanceHistoryResponse
import com.carslab.crm.modules.finances.domain.balance.BalanceHistoryService
import com.carslab.crm.modules.finances.domain.balance.BalanceOverrideService
import com.carslab.crm.modules.finances.domain.balance.BalanceStatistics
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceHistoryEntity
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/balance-override")
@Tag(name = "Balance Override", description = "Manual balance corrections and cash management")
class BalanceOverrideController(
    private val balanceOverrideService: BalanceOverrideService,
    private val balanceHistoryService: BalanceHistoryService,
    private val securityContext: SecurityContext
) : BaseController() {

    @PostMapping("/cash/to-safe")
    @Operation(summary = "Move cash to safe", description = "Transfer cash from register to safe")
    fun moveCashToSafe(
        @Valid @RequestBody request: CashMoveRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<BalanceOverrideResult> {
        logger.info("Moving cash to safe: ${request.amount} PLN")

        val result = balanceOverrideService.moveCashToSafe(
            companyId = securityContext.getCurrentCompanyId(),
            amount = request.amount,
            userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("User not authenticated"),
            description = request.description
        )

        return if (result.success) ok(result) else badRequest(result.message)
    }

    @PostMapping("/cash/from-safe")
    @Operation(summary = "Move cash from safe", description = "Transfer cash from safe to register")
    fun moveCashFromSafe(
        @Valid @RequestBody request: CashMoveRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<BalanceOverrideResult> {
        logger.info("Moving cash from safe: ${request.amount} PLN")

        val result = balanceOverrideService.moveCashFromSafe(
            companyId = securityContext.getCurrentCompanyId(),
            amount = request.amount,
            userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("User not authenticated"),
            description = request.description
        )

        return if (result.success) ok(result) else badRequest(result.message)
    }

    @PostMapping("/bank/reconcile")
    @Operation(summary = "Reconcile with bank statement", description = "Set bank balance based on statement")
    fun reconcileWithBankStatement(
        @Valid @RequestBody request: BankReconciliationRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<BalanceOverrideResult> {
        logger.info("Bank reconciliation: ${request.statementBalance} PLN")

        val result = balanceOverrideService.reconcileWithBankStatement(
            companyId = securityContext.getCurrentCompanyId(),
            bankStatementBalance = request.statementBalance,
            userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("User not authenticated"),
            description = request.description
        )

        return if (result.success) ok(result) else badRequest(result.message)
    }

    @PostMapping("/cash/inventory")
    @Operation(summary = "Perform cash inventory", description = "Set cash balance based on physical count")
    fun performCashInventory(
        @Valid @RequestBody request: CashInventoryRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<BalanceOverrideResult> {
        logger.info("Cash inventory: ${request.countedAmount} PLN")

        val result = balanceOverrideService.performCashInventory(
            companyId = securityContext.getCurrentCompanyId(),
            countedAmount = request.countedAmount,
            userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("User not authenticated"),
            inventoryNotes = request.notes
        )

        return if (result.success) ok(result) else badRequest(result.message)
    }

    @PostMapping("/manual")
    @Operation(summary = "Manual balance override", description = "Manually set balance with custom description")
    fun manualOverride(
        @Valid @RequestBody request: ManualBalanceOverrideRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<BalanceOverrideResult> {
        logger.info("Manual balance override: ${request.newBalance} PLN for ${request.balanceType}")

        val overrideRequest = BalanceOverrideRequest(
            companyId = securityContext.getCurrentCompanyId(),
            balanceType = request.balanceType,
            newBalance = request.newBalance,
            description = request.description,
            userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("User not authenticated"),
            ipAddress = getClientIpAddress(httpRequest)
        )

        val result = balanceOverrideService.overrideBalance(overrideRequest)

        return if (result.success) ok(result) else badRequest(result.message)
    }

    @GetMapping("/history")
    @Operation(summary = "Get balance change history", description = "Retrieve paginated history of balance changes")
    fun getBalanceHistory(
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int,

        @Parameter(description = "Balance type filter")
        @RequestParam(required = false) balance_type: BalanceType?,

        @Parameter(description = "User ID filter")
        @RequestParam(required = false) user_id: String?,

        @Parameter(description = "Document ID filter")
        @RequestParam(required = false) document_id: String?,

        @Parameter(description = "Start date filter")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) start_date: LocalDateTime?,

        @Parameter(description = "End date filter")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) end_date: LocalDateTime?,

        @Parameter(description = "Search in descriptions")
        @RequestParam(required = false) search_text: String?
    ): ResponseEntity<Page<BalanceHistoryResponse>> {

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))

        val history = balanceHistoryService.searchBalanceHistory(
            companyId = securityContext.getCurrentCompanyId(),
            balanceType = balance_type,
            userId = user_id,
            documentId = document_id,
            startDate = start_date,
            endDate = end_date,
            searchText = search_text,
            pageable = pageable
        )
            .map { BalanceHistoryMapper().toResponse(it) }

        return ok(history)
    }

    @GetMapping("/history/{balanceType}")
    @Operation(summary = "Get balance history by type", description = "Get history for specific balance type")
    fun getBalanceHistoryByType(
        @PathVariable balanceType: BalanceType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<BalanceHistoryEntity>> {

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))

        val history = balanceHistoryService.getBalanceHistoryByType(
            companyId = securityContext.getCurrentCompanyId(),
            balanceType = balanceType,
            pageable = pageable
        )

        return ok(history)
    }

    @GetMapping("/history/document/{documentId}")
    @Operation(summary = "Get balance history for document", description = "Get all balance changes related to specific document")
    fun getBalanceHistoryByDocument(
        @PathVariable documentId: String
    ): ResponseEntity<List<BalanceHistoryEntity>> {

        val history = balanceHistoryService.getBalanceHistoryByDocument(
            companyId = securityContext.getCurrentCompanyId(),
            documentId = documentId
        )

        return ok(history)
    }

    @GetMapping("/statistics/{balanceType}")
    @Operation(summary = "Get balance statistics", description = "Get statistics for balance changes in time period")
    fun getBalanceStatistics(
        @PathVariable balanceType: BalanceType,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime
    ): ResponseEntity<BalanceStatistics> {

        val statistics = balanceHistoryService.getBalanceStatistics(
            companyId = securityContext.getCurrentCompanyId(),
            balanceType = balanceType,
            startDate = startDate,
            endDate = endDate
        )

        return ok(statistics)
    }

    @GetMapping("/history/last/{balanceType}")
    @Operation(summary = "Get last operation", description = "Get the most recent operation for balance type")
    @PreAuthorize("hasRole('FINANCE_MANAGER') or hasRole('ADMIN') or hasRole('FINANCE_VIEW')")
    fun getLastOperation(
        @PathVariable balanceType: BalanceType
    ): ResponseEntity<BalanceHistoryEntity?> {

        val lastOperation = balanceHistoryService.getLastOperationForBalanceType(
            companyId = securityContext.getCurrentCompanyId(),
            balanceType = balanceType
        )

        return ok(lastOperation)
    }

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr ?: "unknown"
    }
}