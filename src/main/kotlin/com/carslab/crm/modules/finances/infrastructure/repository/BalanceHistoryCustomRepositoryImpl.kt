package com.carslab.crm.modules.finances.infrastructure.repository

import com.carslab.crm.modules.finances.infrastructure.entity.BalanceHistoryEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.time.LocalDateTime

/**
 * Implementacja custom repository używająca Criteria API
 * Type-safe i database-agnostic rozwiązanie dla produkcji
 */
@Repository
class BalanceHistoryCustomRepositoryImpl(
    private val entityManager: EntityManager
) : BalanceHistoryCustomRepository {

    override fun searchWithCriteria(
        companyId: Long,
        balanceType: String?,
        operationType: String?,
        userId: String?,
        documentId: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchText: String?,
        pageable: Pageable
    ): Page<BalanceHistoryEntity> {

        val cb: CriteriaBuilder = entityManager.criteriaBuilder
        val query: CriteriaQuery<BalanceHistoryEntity> = cb.createQuery(BalanceHistoryEntity::class.java)
        val root: Root<BalanceHistoryEntity> = query.from(BalanceHistoryEntity::class.java)

        // Budowanie predykatów
        val predicates = buildPredicates(cb, root, companyId, balanceType, operationType,
            userId, documentId, startDate, endDate, searchText)

        // Aplikowanie predykatów i sortowania
        query.where(cb.and(*predicates.toTypedArray()))
        query.orderBy(cb.desc(root.get<LocalDateTime>("timestamp")))

        // Wykonanie zapytania z paginacją
        val typedQuery = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.offset.toInt()
        typedQuery.maxResults = pageable.pageSize

        val resultList = typedQuery.resultList

        // Count query dla total elements
        val totalElements = getCountQuery(cb, companyId, balanceType, operationType,
            userId, documentId, startDate, endDate, searchText)

        return PageImpl(resultList, pageable, totalElements)
    }

    private fun buildPredicates(
        cb: CriteriaBuilder,
        root: Root<BalanceHistoryEntity>,
        companyId: Long,
        balanceType: String?,
        operationType: String?,
        userId: String?,
        documentId: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchText: String?
    ): List<Predicate> {

        val predicates = mutableListOf<Predicate>()

        // Company ID - zawsze wymagane (multi-tenant)
        predicates.add(cb.equal(root.get<Long>("companyId"), companyId))

        // Balance type filter
        balanceType?.let {
            predicates.add(cb.equal(root.get<String>("balanceType"), it))
        }

        // Operation type filter
        operationType?.let {
            predicates.add(cb.equal(root.get<String>("operationType"), it))
        }

        // User ID filter
        userId?.let {
            predicates.add(cb.equal(root.get<String>("userId"), it))
        }

        // Document ID filter
        documentId?.let {
            predicates.add(cb.equal(root.get<String>("documentId"), it))
        }

        // Date range filters
        startDate?.let {
            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), it))
        }

        endDate?.let {
            predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), it))
        }

        // Search text filter - case insensitive
        searchText?.takeIf { it.isNotBlank() }?.let {
            predicates.add(
                cb.like(
                    cb.lower(root.get("description")),
                    "%${it.lowercase()}%"
                )
            )
        }

        return predicates
    }

    private fun getCountQuery(
        cb: CriteriaBuilder,
        companyId: Long,
        balanceType: String?,
        operationType: String?,
        userId: String?,
        documentId: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchText: String?
    ): Long {

        val countQuery: CriteriaQuery<Long> = cb.createQuery(Long::class.java)
        val countRoot: Root<BalanceHistoryEntity> = countQuery.from(BalanceHistoryEntity::class.java)

        val predicates = buildPredicates(cb, countRoot, companyId, balanceType, operationType,
            userId, documentId, startDate, endDate, searchText)

        countQuery.select(cb.count(countRoot))
        countQuery.where(cb.and(*predicates.toTypedArray()))

        return entityManager.createQuery(countQuery).singleResult
    }
}