package com.carslab.crm.modules.communication.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class LeadCardDto(
    val id: Long,
    val stage: String,
    val vehicleBrand: String?,
    val vehicleModel: String?,
    val vehicleYear: Int?,
    val services: List<String>,
    val customerName: String?,
    val contactEmail: String,
    val quoteTotal: Long?,
    val lastMessageAt: String,
    val lastDirection: String,
    val unread: Boolean,
    val returningCustomer: Boolean,
    val createdAt: String
)

data class LeadListResponse(val leads: List<LeadCardDto>)

data class FactDto(
    val id: Long,
    val key: String,
    val value: String,
    val verified: Boolean,
    val sourceMessageId: Long?
)

data class TimelineItemDto(
    val type: String,
    val id: Long? = null,
    // MESSAGE
    val direction: String? = null,
    val sentAt: String? = null,
    val fromName: String? = null,
    val bodyText: String? = null,
    // NOTE
    val text: String? = null,
    val createdAt: String? = null,
    val author: String? = null,
    // EVENT
    val at: String? = null,
    // QUOTE
    val items: List<QuoteItemDto>? = null,
    val total: Long? = null
)

data class QuoteItemDto(val name: String, val price: Long)

data class LeadDetailDto(
    val id: Long,
    val stage: String,
    val vehicleBrand: String?,
    val vehicleModel: String?,
    val vehicleYear: Int?,
    val services: List<String>,
    val customerName: String?,
    val contactEmail: String,
    val phone: String?,
    val quoteTotal: Long?,
    val lastMessageAt: String,
    val lastDirection: String,
    val unread: Boolean,
    val returningCustomer: Boolean,
    val createdAt: String,
    val facts: List<FactDto>,
    val timeline: List<TimelineItemDto>
)

data class ReplyQuoteRequest(@field:NotEmpty val items: List<QuoteItemDto>)

data class ReplyRequest(
    @field:NotBlank val bodyText: String,
    val quote: ReplyQuoteRequest? = null,
    val nextStage: String? = null
)

data class ReplyResponse(val messageId: Long)

data class StageChangeRequest(@field:NotBlank val stage: String, val reason: String? = null)

data class AddNoteRequest(@field:NotBlank val text: String)

data class UpdateFactRequest(val factId: Long, @field:NotBlank val value: String)

data class ReviewItemDto(
    val threadId: Long,
    val fromEmail: String,
    val fromName: String?,
    val subject: String?,
    val snippet: String,
    val receivedAt: String
)

data class ReviewListResponse(val items: List<ReviewItemDto>)

data class ReviewDecisionRequest(@field:NotBlank val decision: String)

data class DetectProviderRequest(@field:NotBlank @field:Email val email: String)

data class DetectProviderResponse(
    val providerType: String,
    val authType: String,
    val imapHost: String?,
    val imapPort: Int?,
    val smtpHost: String?,
    val smtpPort: Int?,
    val requiresAppPassword: Boolean,
    val guideUrl: String?
)

data class ConnectMailAccountRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val password: String,
    val imapHost: String? = null,
    val imapPort: Int? = null,
    val smtpHost: String? = null,
    val smtpPort: Int? = null
)

data class MailAccountDto(
    val id: Long,
    val emailAddress: String,
    val providerType: String,
    val status: String,
    val lastSyncAt: String?
)
