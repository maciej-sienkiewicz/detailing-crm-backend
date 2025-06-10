package com.carslab.crm.modules.finances.domain.balance

import com.carslab.crm.infrastructure.auth.UserService
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.modules.finances.api.BalanceOverrideRequest
import com.carslab.crm.modules.finances.api.BalanceOverrideResult
import com.carslab.crm.modules.finances.domain.exception.InsufficientFundsException
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceOperationEntity
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceOperationType
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceType
import com.carslab.crm.modules.finances.infrastructure.entity.CompanyBalanceEntity
import com.carslab.crm.modules.finances.infrastructure.entity.OverrideReason
import com.carslab.crm.modules.finances.infrastructure.repository.BalanceOperationRepository
import com.carslab.crm.modules.finances.infrastructure.repository.CompanyBalanceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class BalanceOverrideService(
    private val companyBalanceRepository: CompanyBalanceRepository,
    private val balanceOperationRepository: BalanceOperationRepository,
    private val userService: UserService
) {

    private val logger = LoggerFactory.getLogger(BalanceOverrideService::class.java)

    /**
     * 🔥 GŁÓWNA FUNKCJA: Nadpisanie salda z pełnym audytem
     */
    fun overrideBalance(request: BalanceOverrideRequest): BalanceOverrideResult {
        logger.info("Balance override requested: company=${request.companyId}, type=${request.balanceType}, " +
                "newBalance=${request.newBalance}, reason=${request.reason}")

        // 2. Walidacja danych
        validateOverrideRequest(request)

        // 4. Wykonaj nadpisanie
        return executeBalanceOverride(request)
    }

    /**
     * 💰 PRZENIESIENIE GOTÓWKI DO SEJFU
     */
    fun moveCashToSafe(
        companyId: Long,
        amount: BigDecimal,
        userId: String,
        description: String? = null
    ): BalanceOverrideResult {
        logger.info("💰 Moving cash to safe: company=$companyId, amount=$amount")

        val currentBalance = getCurrentCashBalance(companyId)

        if (currentBalance < amount) {
            throw InsufficientFundsException("Insufficient cash balance: $currentBalance, required: $amount")
        }

        val newBalance = currentBalance - amount

        return overrideBalance(
            BalanceOverrideRequest(
                companyId = companyId,
                balanceType = BalanceType.CASH,
                newBalance = newBalance,
                reason = OverrideReason.CASH_TO_SAFE,
                userId = userId,
                description = description ?: "Przeniesienie $amount PLN do sejfu",
                isPreApproved = true
            )
        )
    }

    /**
     * 💰 POBRANIE GOTÓWKI Z SEJFU
     */
    fun moveCashFromSafe(
        companyId: Long,
        amount: BigDecimal,
        userId: String,
        description: String? = null
    ): BalanceOverrideResult {
        logger.info("💰 Moving cash from safe: company=$companyId, amount=$amount")

        val currentBalance = getCurrentCashBalance(companyId)
        val newBalance = currentBalance + amount

        return overrideBalance(BalanceOverrideRequest(
            companyId = companyId,
            balanceType = BalanceType.CASH,
            newBalance = newBalance,
            reason = OverrideReason.CASH_FROM_SAFE,
            userId = userId,
            description = description ?: "Pobranie $amount PLN z sejfu",
            isPreApproved = true
        ))
    }

    /**
     * 🏦 UZGODNIENIE Z WYCIĄGIEM BANKOWYM
     */
    fun reconcileWithBankStatement(
        companyId: Long,
        bankStatementBalance: BigDecimal,
        userId: String,
        description: String
    ): BalanceOverrideResult {
        logger.info("🏦 Bank statement reconciliation: company=$companyId, statementBalance=$bankStatementBalance")

        return overrideBalance(BalanceOverrideRequest(
            companyId = companyId,
            balanceType = BalanceType.BANK,
            newBalance = bankStatementBalance,
            reason = OverrideReason.BANK_STATEMENT_RECONCILIATION,
            userId = userId,
            description = "Uzgodnienie z wyciągiem bankowym: $description",
            isPreApproved = false // Wymaga zatwierdzenia
        ))
    }

    /**
     * 📊 INWENTARYZACJA KASY
     */
    fun performCashInventory(
        companyId: Long,
        countedAmount: BigDecimal,
        userId: String,
        inventoryNotes: String
    ): BalanceOverrideResult {
        logger.info("📊 Cash inventory: company=$companyId, counted=$countedAmount")

        val currentBalance = getCurrentCashBalance(companyId)
        val difference = countedAmount - currentBalance

        val description = if (difference == BigDecimal.ZERO) {
            "Inwentaryzacja kasy - stan zgodny: $countedAmount PLN"
        } else {
            "Inwentaryzacja kasy - różnica: ${difference.abs()} PLN (${if (difference > BigDecimal.ZERO) "nadwyżka" else "niedobór"}). Uwagi: $inventoryNotes"
        }

        return overrideBalance(BalanceOverrideRequest(
            companyId = companyId,
            balanceType = BalanceType.CASH,
            newBalance = countedAmount,
            reason = OverrideReason.INVENTORY_COUNT,
            userId = userId,
            description = description,
            isPreApproved = false // Duże różnice wymagają zatwierdzenia
        ))
    }

    // ============ PRIVATE METHODS ============

    private fun executeBalanceOverride(request: BalanceOverrideRequest): BalanceOverrideResult {
        try {
            val balance = getOrCreateBalance(request.companyId)
            val user = userService.getUserById(request.userId.toLong())

            val previousBalance = when (request.balanceType) {
                BalanceType.CASH -> balance.cashBalance
                BalanceType.BANK -> balance.bankBalance
            }

            // Aktualizuj saldo
            when (request.balanceType) {
                BalanceType.CASH -> balance.cashBalance = request.newBalance
                BalanceType.BANK -> balance.bankBalance = request.newBalance
            }
            balance.lastUpdated = LocalDateTime.now()

            // Zapisz z optimistic locking
            val savedBalance = companyBalanceRepository.save(balance)

            // Zapisz operację do audytu
            val operation = BalanceOperationEntity(
                companyId = request.companyId,
                documentId = null, // Manualna operacja
                operationType = BalanceOperationType.MANUAL_OVERRIDE,
                balanceType = request.balanceType,
                amount = request.newBalance - previousBalance,
                previousBalance = previousBalance,
                newBalance = request.newBalance,
                overrideReason = request.reason,
                userId = request.userId,
                userName = user.firstName + " " + user.lastName,
                description = request.description,
                approvedBy = request.approvedBy,
                approvalDate = if (request.approvedBy != null) LocalDateTime.now() else null,
                isApproved = true,
                ipAddress = request.ipAddress,
                userAgent = request.userAgent
            )

            balanceOperationRepository.save(operation)

            logger.info("✅ Balance override successful: company=${request.companyId}, " +
                    "${request.balanceType}: $previousBalance → ${request.newBalance}")

            return BalanceOverrideResult(
                success = true,
                operationId = operation.id,
                previousBalance = previousBalance,
                newBalance = request.newBalance,
                difference = request.newBalance - previousBalance,
                message = "Saldo zostało pomyślnie zaktualizowane"
            )

        } catch (e: Exception) {
            logger.error("❌ Balance override failed: company=${request.companyId}", e)

            return BalanceOverrideResult(
                success = false,
                operationId = null,
                previousBalance = null,
                newBalance = request.newBalance,
                difference = null,
                message = "Błąd podczas aktualizacji salda: ${e.message}",
                error = e
            )
        }
    }

    private fun validateOverrideRequest(request: BalanceOverrideRequest) {
        if (request.newBalance < BigDecimal.ZERO) {
            throw ValidationException("New balance cannot be negative")
        }

        if (request.newBalance > BigDecimal("1000000")) {
            throw ValidationException("New balance exceeds maximum allowed amount")
        }

        if (request.description.isNullOrBlank() && request.reason == OverrideReason.OTHER) {
            throw ValidationException("Description is required for 'Other' reason")
        }
    }

    private fun getCurrentCashBalance(companyId: Long): BigDecimal {
        return companyBalanceRepository.findById(companyId)
            .map { it.cashBalance }
            .orElse(BigDecimal.ZERO)
    }

    private fun getOrCreateBalance(companyId: Long): CompanyBalanceEntity {
        return companyBalanceRepository.findById(companyId).orElse(
            CompanyBalanceEntity(companyId = companyId)
        )
    }
}