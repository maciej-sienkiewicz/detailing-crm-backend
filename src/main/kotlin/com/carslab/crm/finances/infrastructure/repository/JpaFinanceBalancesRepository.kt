package com.carslab.crm.finances.infrastructure.repository

import com.carslab.crm.finances.infrastructure.entitiy.BackAccountBalanceEntity
import com.carslab.crm.finances.infrastructure.entitiy.CashBalanceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.Optional

@Repository
interface BankAccountBalanceRepository : JpaRepository<BackAccountBalanceEntity, Long> {

    /**
     * Wyciąga stan konta bankowego dla określonej firmy
     */
    @Query("SELECT b FROM BackAccountBalanceEntity b WHERE b.companyId = :companyId")
    fun findByCompanyId(@Param("companyId") companyId: Long): Optional<BackAccountBalanceEntity>

    /**
     * Upsert - wstawia nowy rekord lub aktualizuje istniejący (konto bankowe)
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO bank_account_balances (company_id, amount, last_update) 
        VALUES (:companyId, :amount, :lastUpdate)
        ON CONFLICT (company_id) DO UPDATE SET 
            amount = :amount,
            last_update = :lastUpdate
    """, nativeQuery = true)
    fun upsertBalance(
        @Param("companyId") companyId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Dodaje kwotę do aktualnego stanu konta bankowego
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO bank_account_balances (company_id, amount, last_update) 
        VALUES (:companyId, :amount, :lastUpdate)
        ON CONFLICT (company_id) DO UPDATE SET 
            amount = bank_account_balances.amount + :amount, 
            last_update = :lastUpdate
    """, nativeQuery = true)
    fun addAmountToBalance(
        @Param("companyId") companyId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Odejmuje kwotę od aktualnego stanu konta bankowego
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO bank_account_balances (company_id, amount, last_update) 
        VALUES (:companyId, -:amount, :lastUpdate)
        ON CONFLICT (company_id) DO UPDATE SET 
            amount = bank_account_balances.amount - :amount, 
            last_update = :lastUpdate
    """, nativeQuery = true)
    fun subtractAmountFromBalance(
        @Param("companyId") companyId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Nadpisuje stan konta bankowego nową wartością (zwykły UPDATE - wymaga istniejącego rekordu)
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE BackAccountBalanceEntity b 
        SET b.amount = :newAmount, 
            b.lastUpdate = :lastUpdate 
        WHERE b.companyId = :companyId
    """)
    fun updateBalance(
        @Param("companyId") companyId: Long,
        @Param("newAmount") newAmount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Pobiera aktualny stan konta bankowego
     */
    @Query(value = """
        SELECT COALESCE(amount, 0) 
        FROM bank_account_balances 
        WHERE company_id = :companyId
    """, nativeQuery = true)
    fun getCurrentBalance(@Param("companyId") companyId: Long): BigDecimal?
}

@Repository
interface CashBalancesRepository : JpaRepository<CashBalanceEntity, Long> {

    /**
     * Wyciąga stan kasy dla określonej firmy
     */
    @Query("SELECT c FROM CashBalanceEntity c WHERE c.companyId = :companyId")
    fun findByCompanyId(@Param("companyId") companyId: Long): Optional<CashBalanceEntity>

    /**
     * Upsert - wstawia nowy rekord lub aktualizuje istniejący (kasa)
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO cash_balances (company_id, amount, last_update) 
        VALUES (:companyId, :amount, :lastUpdate)
        ON CONFLICT (company_id) DO UPDATE SET 
            amount = :amount,
            last_update = :lastUpdate
    """, nativeQuery = true)
    fun upsertBalance(
        @Param("companyId") companyId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Dodaje kwotę do aktualnego stanu kasy
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO cash_balances (company_id, amount, last_update) 
        VALUES (:companyId, :amount, :lastUpdate)
        ON CONFLICT (company_id) DO UPDATE SET 
            amount = cash_balances.amount + :amount, 
            last_update = :lastUpdate
    """, nativeQuery = true)
    fun addAmountToBalance(
        @Param("companyId") companyId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Odejmuje kwotę od aktualnego stanu kasy
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO cash_balances (company_id, amount, last_update) 
        VALUES (:companyId, -:amount, :lastUpdate)
        ON CONFLICT (company_id) DO UPDATE SET 
            amount = cash_balances.amount - :amount, 
            last_update = :lastUpdate
    """, nativeQuery = true)
    fun subtractAmountFromBalance(
        @Param("companyId") companyId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Nadpisuje stan kasy nową wartością (zwykły UPDATE - wymaga istniejącego rekordu)
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE CashBalanceEntity c 
        SET c.amount = :newAmount, 
            c.lastUpdate = :lastUpdate 
        WHERE c.companyId = :companyId
    """)
    fun updateBalance(
        @Param("companyId") companyId: Long,
        @Param("newAmount") newAmount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Pobiera aktualny stan kasy
     */
    @Query(value = """
        SELECT COALESCE(amount, 0) 
        FROM cash_balances 
        WHERE company_id = :companyId
    """, nativeQuery = true)
    fun getCurrentBalance(@Param("companyId") companyId: Long): BigDecimal?
}