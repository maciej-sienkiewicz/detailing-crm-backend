package com.carslab.crm.domain

import com.carslab.crm.api.model.CashTransactionFilterDTO
import com.carslab.crm.api.model.request.CreateCashTransactionRequest
import com.carslab.crm.api.model.request.UpdateCashTransactionRequest
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.UserId
import com.carslab.crm.domain.model.view.finance.CashStatistics
import com.carslab.crm.domain.model.view.finance.CashTransaction
import com.carslab.crm.domain.model.view.finance.TransactionId
import com.carslab.crm.domain.model.view.finance.TransactionType
import com.carslab.crm.domain.port.CashRepository
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class CashService(
    private val cashRepository: CashRepository
) {
    private val logger = LoggerFactory.getLogger(CashService::class.java)

    /**
     * Tworzy nową transakcję gotówkową.
     */
    @Transactional
    fun createCashTransaction(request: CreateCashTransactionRequest): CashTransaction {
        logger.info("Creating new cash transaction: {}", request.description)

        // Walidacja
        validateCashTransactionRequest(request)

        // Tworzenie transakcji
        val transaction = CashTransaction(
            id = TransactionId.generate(),
            type = TransactionType.valueOf(request.type),
            description = request.description,
            date = request.date,
            amount = request.amount,
            visitId = request.visitId,
            createdBy = UserId("0"),
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        // Zapisanie transakcji w repozytorium
        val savedTransaction = cashRepository.save(transaction)
        logger.info("Created cash transaction with ID: {}", savedTransaction.id.value)

        return savedTransaction
    }

    /**
     * Aktualizuje istniejącą transakcję gotówkową.
     */
    @Transactional
    fun updateCashTransaction(id: String, request: UpdateCashTransactionRequest): CashTransaction {
        logger.info("Updating cash transaction with ID: {}", id)

        // Sprawdzenie czy transakcja istnieje
        val existingTransaction = cashRepository.findById(TransactionId(id))
            ?: throw ResourceNotFoundException("CashTransaction", id)

        // Walidacja
        validateCashTransactionRequest(request)

        // Aktualizacja transakcji
        val updatedTransaction = CashTransaction(
            id = existingTransaction.id,
            type = TransactionType.valueOf(request.type),
            description = request.description,
            date = request.date,
            amount = request.amount,
            visitId = request.visitId,
            createdBy = existingTransaction.createdBy, // Zachowujemy oryginalnego twórcę
            audit = Audit(
                createdAt = existingTransaction.audit.createdAt,
                updatedAt = LocalDateTime.now()
            )
        )

        // Zapisanie zaktualizowanej transakcji
        val savedTransaction = cashRepository.save(updatedTransaction)
        logger.info("Updated cash transaction with ID: {}", savedTransaction.id.value)

        return savedTransaction
    }

    /**
     * Pobiera transakcję po ID.
     */
    fun getCashTransactionById(id: String): CashTransaction {
        logger.debug("Getting cash transaction by ID: {}", id)
        return cashRepository.findById(TransactionId(id))
            ?: throw ResourceNotFoundException("CashTransaction", id)
    }

    /**
     * Pobiera wszystkie transakcje z opcjonalnym filtrowaniem i paginacją.
     */
    fun getAllCashTransactions(
        filter: CashTransactionFilterDTO? = null,
        page: Int = 0,
        size: Int = 20
    ): PaginatedResponse<CashTransaction> {
        logger.debug("Getting all cash transactions with filter: {}, page: {}, size: {}", filter, page, size)

        val (transactions, totalCount) = cashRepository.findAll(filter, page, size)

        val totalPages = if (totalCount % size == 0L) totalCount / size else totalCount / size + 1

        return PaginatedResponse(
            data = transactions,
            page = page,
            size = size,
            totalItems = totalCount,
            totalPages = totalPages
        )
    }

    /**
     * Usuwa transakcję po ID.
     */
    @Transactional
    fun deleteCashTransaction(id: String): Boolean {
        logger.info("Deleting cash transaction with ID: {}", id)

        // Sprawdzenie czy transakcja istnieje
        if (cashRepository.findById(TransactionId(id)) == null) {
            throw ResourceNotFoundException("CashTransaction", id)
        }

        // Usunięcie transakcji
        return cashRepository.deleteById(TransactionId(id))
    }

    /**
     * Pobiera aktualny stan kasy.
     */
    fun getCurrentBalance(): BigDecimal {
        logger.debug("Getting current cash balance")
        return cashRepository.getCurrentBalance()
    }

    /**
     * Pobiera statystyki gotówkowe za bieżący miesiąc.
     */
    fun getCurrentMonthStatistics(): CashStatistics {
        logger.debug("Getting cash statistics for current month")
        return cashRepository.getCurrentMonthStatistics()
    }

    /**
     * Pobiera statystyki gotówkowe za podany okres.
     */
    fun getStatisticsForPeriod(startDate: LocalDate, endDate: LocalDate): CashStatistics {
        logger.debug("Getting cash statistics for period: {} to {}", startDate, endDate)
        return cashRepository.getStatisticsForPeriod(startDate, endDate)
    }

    /**
     * Pobiera statystyki gotówkowe za podany miesiąc i rok.
     */
    fun getStatisticsForMonth(year: Int, month: Int): CashStatistics {
        logger.debug("Getting cash statistics for month: {}/{}", month, year)
        val yearMonth = YearMonth.of(year, month)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        return cashRepository.getStatisticsForPeriod(startDate, endDate)
    }

    /**
     * Pobiera transakcje dla powiązanej wizyty.
     */
    fun getTransactionsByVisitId(visitId: String): List<CashTransaction> {
        logger.debug("Getting cash transactions for visit ID: {}", visitId)
        return cashRepository.findByVisitId(visitId)
    }

    /**
     * Walidacja danych transakcji.
     */
    private fun validateCashTransactionRequest(request: Any) {
        when (request) {
            is CreateCashTransactionRequest -> {
                if (request.amount <= BigDecimal.ZERO) {
                    throw ValidationException("Amount must be greater than zero")
                }

                try {
                    TransactionType.valueOf(request.type)
                } catch (e: IllegalArgumentException) {
                    throw ValidationException("Invalid transaction type: ${request.type}")
                }
            }
            is UpdateCashTransactionRequest -> {
                if (request.amount <= BigDecimal.ZERO) {
                    throw ValidationException("Amount must be greater than zero")
                }

                try {
                    TransactionType.valueOf(request.type)
                } catch (e: IllegalArgumentException) {
                    throw ValidationException("Invalid transaction type: ${request.type}")
                }
            }
        }
    }
}