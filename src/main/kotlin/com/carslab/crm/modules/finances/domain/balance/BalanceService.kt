package com.carslab.crm.modules.finances.domain.balance

import com.carslab.crm.modules.finances.domain.exception.BalanceUpdateException
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceOperationEntity
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceOperationType
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceType
import com.carslab.crm.modules.finances.infrastructure.entity.CompanyBalanceEntity
import com.carslab.crm.modules.finances.infrastructure.repository.BalanceOperationRepository
import com.carslab.crm.modules.finances.infrastructure.repository.CompanyBalanceRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class BalanceService(
    private val companyBalanceRepository: CompanyBalanceRepository,
    private val balanceOperationRepository: BalanceOperationRepository
) {

    private val logger = LoggerFactory.getLogger(BalanceService::class.java)

    fun updateBalance(
        companyId: Long,
        balanceType: BalanceType,
        amount: BigDecimal,
        operation: BalanceOperationType,
        documentId: String,
        retryCount: Int = 0
    ) {
        try {
            val balance = getOrCreateBalance(companyId)
            val previousBalance = when (balanceType) {
                BalanceType.CASH -> balance.cashBalance
                BalanceType.BANK -> balance.bankBalance
            }

            val newBalance = when (operation) {
                BalanceOperationType.ADD -> previousBalance + amount
                BalanceOperationType.SUBTRACT -> previousBalance - amount
                BalanceOperationType.CORRECTION -> amount
                else -> previousBalance + amount
            }

            when (balanceType) {
                BalanceType.CASH -> balance.cashBalance = newBalance
                BalanceType.BANK -> balance.bankBalance = newBalance
            }
            balance.lastUpdated = LocalDateTime.now()

            val savedBalance = companyBalanceRepository.save(balance)

            recordBalanceOperation(
                companyId, documentId, operation, balanceType,
                amount, previousBalance, newBalance
            )

            logger.info("Balance updated: company={}, type={}, {}→{}",
                companyId, balanceType, previousBalance, newBalance)

        } catch (e: OptimisticLockingFailureException) {
            if (retryCount < 3) {
                logger.warn("Optimistic lock failure, retrying... attempt {}", retryCount + 1)
                updateBalance(companyId, balanceType, amount, operation, documentId, retryCount + 1)
            } else {
                throw BalanceUpdateException("Failed to update balance after retries", e)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getCurrentBalances(companyId: Long): CompanyBalanceEntity {
        return getOrCreateBalance(companyId)
    }

    @Transactional(readOnly = true)
    fun reconcileBalance(companyId: Long): BalanceReconciliationResult {
        val storedBalance = getCurrentBalances(companyId)
        val calculatedBalances = calculateBalanceFromTransactions(companyId)

        val cashDifference = storedBalance.cashBalance - calculatedBalances.cash
        val bankDifference = storedBalance.bankBalance - calculatedBalances.bank

        val isReconciled = cashDifference == BigDecimal.ZERO && bankDifference == BigDecimal.ZERO

        if (!isReconciled) {
            logger.warn("Balance discrepancy for company {}: cash diff={}, bank diff={}",
                companyId, cashDifference, bankDifference)
        }

        return BalanceReconciliationResult(
            companyId = companyId,
            storedCashBalance = storedBalance.cashBalance,
            storedBankBalance = storedBalance.bankBalance,
            calculatedCashBalance = calculatedBalances.cash,
            calculatedBankBalance = calculatedBalances.bank,
            cashDifference = cashDifference,
            bankDifference = bankDifference,
            isReconciled = isReconciled
        )
    }

    private fun getOrCreateBalance(companyId: Long): CompanyBalanceEntity {
        return companyBalanceRepository.findById(companyId).orElse(
            CompanyBalanceEntity(companyId = companyId)
        )
    }

    private fun recordBalanceOperation(
        companyId: Long, documentId: String, operation: BalanceOperationType,
        balanceType: BalanceType, amount: BigDecimal,
        previousBalance: BigDecimal, newBalance: BigDecimal
    ) {
        val balanceOperation = BalanceOperationEntity(
            companyId = companyId,
            documentId = documentId,
            operationType = operation,
            balanceType = balanceType,
            amount = amount,
            previousBalance = previousBalance,
            newBalance = newBalance,
            userId = "SYSTEM",
            userName = "System Operation"
        )
        balanceOperationRepository.save(balanceOperation)
    }

    private fun calculateBalanceFromTransactions(companyId: Long): CalculatedBalances {
        val result = companyBalanceRepository.calculateBalancesFromTransactions(companyId)
        return CalculatedBalances(
            cash = result["cash"] ?: BigDecimal.ZERO,
            bank = result["bank"] ?: BigDecimal.ZERO
        )
    }
}

data class CalculatedBalances(val cash: BigDecimal, val bank: BigDecimal)

data class BalanceReconciliationResult(
    val companyId: Long,
    val storedCashBalance: BigDecimal,
    val storedBankBalance: BigDecimal,
    val calculatedCashBalance: BigDecimal,
    val calculatedBankBalance: BigDecimal,
    val cashDifference: BigDecimal,
    val bankDifference: BigDecimal,
    val isReconciled: Boolean
)