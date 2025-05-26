package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.infrastructure.persistence.entity.BackAccountBalanceEntity
import com.carslab.crm.infrastructure.persistence.entity.CashBalanceEntity
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
     * Wyciąga stan konta dla określonej firmy
     */
    @Query("SELECT b FROM BackAccountBalanceEntity b WHERE b.companyId = :companyId")
    fun findByCompanyId(@Param("companyId") companyId: Long): Optional<BackAccountBalanceEntity>

    /**
     * Dodaje kwotę do aktualnego stanu konta
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE BackAccountBalanceEntity b 
        SET b.amount = b.amount + :amount, 
            b.lastUpdate = :lastUpdate 
        WHERE b.companyId = :companyId
    """)
    fun addAmountToBalance(
        @Param("companyId") companyId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Odejmuje kwotę od aktualnego stanu konta
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE BackAccountBalanceEntity b 
        SET b.amount = b.amount - :amount, 
            b.lastUpdate = :lastUpdate 
        WHERE b.companyId = :companyId
    """)
    fun subtractAmountFromBalance(
        @Param("companyId") companyId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Nadpisuje stan konta nową wartością
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
}

@Repository
interface CashBalancesRepository : JpaRepository<CashBalanceEntity, Long> {

    /**
     * Wyciąga stan konta dla określonej firmy
     */
    @Query("SELECT b FROM BackAccountBalanceEntity b WHERE b.companyId = :companyId")
    fun findByCompanyId(@Param("companyId") companyId: Long): Optional<BackAccountBalanceEntity>

    /**
     * Dodaje kwotę do aktualnego stanu konta
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE BackAccountBalanceEntity b 
        SET b.amount = b.amount + :amount, 
            b.lastUpdate = :lastUpdate 
        WHERE b.companyId = :companyId
    """)
    fun addAmountToBalance(
        @Param("companyId") companyId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Odejmuje kwotę od aktualnego stanu konta
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE BackAccountBalanceEntity b 
        SET b.amount = b.amount - :amount, 
            b.lastUpdate = :lastUpdate 
        WHERE b.companyId = :companyId
    """)
    fun subtractAmountFromBalance(
        @Param("companyId") companyId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("lastUpdate") lastUpdate: String
    ): Int

    /**
     * Nadpisuje stan konta nową wartością
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
}