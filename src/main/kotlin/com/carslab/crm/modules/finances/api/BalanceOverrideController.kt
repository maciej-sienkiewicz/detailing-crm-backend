package com.carslab.crm.modules.finances.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.finances.domain.balance.BalanceOverrideService
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/balance-override")
@Tag(name = "Balance Override", description = "Manual balance corrections and cash management")
class BalanceOverrideController(
    private val balanceOverrideService: BalanceOverrideService,
    private val securityContext: SecurityContext
) : BaseController() {

    @PostMapping("/cash/to-safe")
    @Operation(summary = "Move cash to safe", description = "Transfer cash from register to safe")
    fun moveCashToSafe(
        @RequestBody request: CashMoveRequest
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
        @RequestBody request: CashMoveRequest
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
        @RequestBody request: BankReconciliationRequest
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
        @RequestBody request: CashInventoryRequest
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
    @Operation(summary = "Manual balance override", description = "Manually set balance with reason")
    fun manualOverride(
        @RequestBody request: ManualOverrideRequest
    ): ResponseEntity<BalanceOverrideResult> {
        logger.info("Manual balance override: ${request.newBalance} PLN")

        val overrideRequest = BalanceOverrideRequest(
            companyId = securityContext.getCurrentCompanyId(),
            balanceType = request.balanceType,
            newBalance = request.newBalance,
            reason = request.reason,
            userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("User not authenticated"),
            description = request.description,
            ipAddress = "",
            userAgent = ""
        )

        val result = balanceOverrideService.overrideBalance(overrideRequest)

        return if (result.success) ok(result) else badRequest(result.message)
    }
}
