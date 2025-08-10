package com.carslab.crm.modules.finances.api.responses

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Model odpowiedzi dla historii zmian sald
 */
data class BalanceHistoryResponse(
    @JsonProperty("operation_id")
    val operationId: Long,

    @JsonProperty("balance_type")
    val balanceType: String,

    @JsonProperty("balance_before")
    val balanceBefore: BigDecimal,

    @JsonProperty("balance_after")
    val balanceAfter: BigDecimal,

    @JsonProperty("amount_changed")
    val amountChanged: BigDecimal,

    @JsonProperty("operation_type")
    val operationType: String,

    @JsonProperty("operation_description")
    val operationDescription: String,

    @JsonProperty("document_id")
    val documentId: String?,

    @JsonProperty("user_id")
    val userId: String,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime,

    @JsonProperty("ip_address")
    val ipAddress: String?,

    @JsonProperty("related_operation_id")
    val relatedOperationId: Long?
)

/**
 * Strona z historią operacji
 */
data class BalanceHistoryPageResponse(
    val content: List<BalanceHistoryResponse>,

    @JsonProperty("page_number")
    val pageNumber: Int,

    @JsonProperty("page_size")
    val pageSize: Int,

    @JsonProperty("total_elements")
    val totalElements: Long,

    @JsonProperty("total_pages")
    val totalPages: Int,

    @JsonProperty("first")
    val isFirst: Boolean,

    @JsonProperty("last")
    val isLast: Boolean,

    @JsonProperty("has_next")
    val hasNext: Boolean,

    @JsonProperty("has_previous")
    val hasPrevious: Boolean
)

/**
 * Podsumowanie statystyk sald
 */
data class BalanceStatisticsResponse(
    @JsonProperty("period_start")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val periodStart: LocalDateTime,

    @JsonProperty("period_end")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val periodEnd: LocalDateTime,

    @JsonProperty("balance_type")
    val balanceType: String,

    @JsonProperty("total_operations")
    val totalOperations: Int,

    @JsonProperty("total_amount_changed")
    val totalAmountChanged: BigDecimal,

    @JsonProperty("positive_changes_count")
    val positiveChangesCount: Int,

    @JsonProperty("negative_changes_count")
    val negativeChangesCount: Int,

    @JsonProperty("start_balance")
    val startBalance: BigDecimal,

    @JsonProperty("end_balance")
    val endBalance: BigDecimal,

    @JsonProperty("net_change")
    val netChange: BigDecimal,

    @JsonProperty("average_operation_size")
    val averageOperationSize: BigDecimal
)

/**
 * Odpowiedź z ostatnią operacją
 */
data class LastOperationResponse(
    @JsonProperty("has_operations")
    val hasOperations: Boolean,

    @JsonProperty("last_operation")
    val lastOperation: BalanceHistoryResponse?
)

/**
 * Filtr wyszukiwania historii
 */
data class BalanceHistorySearchRequest(
    @JsonProperty("balance_type")
    val balanceType: String?,

    @JsonProperty("operation_type")
    val operationType: String?,

    @JsonProperty("user_id")
    val userId: String?,

    @JsonProperty("document_id")
    val documentId: String?,

    @JsonProperty("start_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val startDate: LocalDateTime?,

    @JsonProperty("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val endDate: LocalDateTime?,

    @JsonProperty("search_text")
    val searchText: String?,

    val page: Int = 0,
    val size: Int = 20
)