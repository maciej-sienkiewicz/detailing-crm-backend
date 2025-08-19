package com.carslab.crm.production.modules.visits.infrastructure.mapper

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.modules.visits.api.request.ApiDiscountType
import com.carslab.crm.modules.visits.api.request.ApiReferralSource
import com.carslab.crm.modules.visits.api.request.ServiceApprovalStatus as ApiServiceApprovalStatus
import com.carslab.crm.production.modules.visits.domain.models.enums.*

object EnumMappers {

    /**
     * Mapper dla VisitStatus
     */
    fun mapToVisitStatus(status: String?): VisitStatus {
        return when (status?.uppercase()) {
            "SCHEDULED" -> VisitStatus.SCHEDULED
            "PENDING_APPROVAL" -> VisitStatus.PENDING_APPROVAL
            "IN_PROGRESS" -> VisitStatus.IN_PROGRESS
            "READY_FOR_PICKUP" -> VisitStatus.READY_FOR_PICKUP
            "COMPLETED" -> VisitStatus.COMPLETED
            "CANCELLED" -> VisitStatus.CANCELLED
            null -> VisitStatus.SCHEDULED
            else -> throw IllegalArgumentException("Invalid visit status: $status")
        }
    }

    fun mapToApiProtocolStatus(status: VisitStatus): ApiProtocolStatus {
        return when (status) {
            VisitStatus.SCHEDULED -> ApiProtocolStatus.SCHEDULED
            VisitStatus.PENDING_APPROVAL -> ApiProtocolStatus.PENDING_APPROVAL
            VisitStatus.IN_PROGRESS -> ApiProtocolStatus.IN_PROGRESS
            VisitStatus.READY_FOR_PICKUP -> ApiProtocolStatus.READY_FOR_PICKUP
            VisitStatus.COMPLETED -> ApiProtocolStatus.COMPLETED
            VisitStatus.CANCELLED -> ApiProtocolStatus.CANCELLED
        }
    }

    fun mapFromApiProtocolStatus(status: ApiProtocolStatus): VisitStatus {
        return when (status) {
            ApiProtocolStatus.SCHEDULED -> VisitStatus.SCHEDULED
            ApiProtocolStatus.PENDING_APPROVAL -> VisitStatus.PENDING_APPROVAL
            ApiProtocolStatus.IN_PROGRESS -> VisitStatus.IN_PROGRESS
            ApiProtocolStatus.READY_FOR_PICKUP -> VisitStatus.READY_FOR_PICKUP
            ApiProtocolStatus.COMPLETED -> VisitStatus.COMPLETED
            ApiProtocolStatus.CANCELLED -> VisitStatus.CANCELLED
        }
    }

    /**
     * Mapper dla ReferralSource
     */
    fun mapToReferralSource(source: String?): ReferralSource? {
        return when (source?.uppercase()) {
            "OTHER" -> ReferralSource.OTHER
            "REGULAR_CUSTOMER" -> ReferralSource.REGULAR_CUSTOMER
            "SOCIAL_MEDIA" -> ReferralSource.SOCIAL_MEDIA
            "LOCAL_AD" -> ReferralSource.LOCAL_AD
            "SEARCH_ENGINE" -> ReferralSource.SEARCH_ENGINE
            "RECOMMENDATION" -> ReferralSource.RECOMMENDATION
            null, "" -> null
            else -> ReferralSource.OTHER
        }
    }

    fun mapToApiReferralSource(source: ReferralSource): ApiReferralSource {
        return when (source) {
            ReferralSource.OTHER -> ApiReferralSource.OTHER
            ReferralSource.REGULAR_CUSTOMER -> ApiReferralSource.REGULAR_CUSTOMER
            ReferralSource.SOCIAL_MEDIA -> ApiReferralSource.SOCIAL_MEDIA
            ReferralSource.LOCAL_AD -> ApiReferralSource.LOCAL_AD
            ReferralSource.SEARCH_ENGINE -> ApiReferralSource.SEARCH_ENGINE
            ReferralSource.RECOMMENDATION -> ApiReferralSource.RECOMMENDATION
        }
    }

    fun mapFromApiReferralSource(source: ApiReferralSource): ReferralSource {
        return when (source) {
            ApiReferralSource.OTHER -> ReferralSource.OTHER
            ApiReferralSource.REGULAR_CUSTOMER -> ReferralSource.REGULAR_CUSTOMER
            ApiReferralSource.SOCIAL_MEDIA -> ReferralSource.SOCIAL_MEDIA
            ApiReferralSource.LOCAL_AD -> ReferralSource.LOCAL_AD
            ApiReferralSource.SEARCH_ENGINE -> ReferralSource.SEARCH_ENGINE
            ApiReferralSource.RECOMMENDATION -> ReferralSource.RECOMMENDATION
        }
    }

    /**
     * Mapper dla DiscountType
     */
    fun mapToDiscountType(type: String?): DiscountType? {
        return when (type?.uppercase()) {
            "PERCENTAGE" -> DiscountType.PERCENTAGE
            "AMOUNT" -> DiscountType.AMOUNT
            "FIXED_PRICE" -> DiscountType.FIXED_PRICE
            null, "" -> null
            else -> throw IllegalArgumentException("Invalid discount type: $type")
        }
    }

    fun mapToApiDiscountType(type: DiscountType): ApiDiscountType {
        return when (type) {
            DiscountType.PERCENTAGE -> ApiDiscountType.PERCENTAGE
            DiscountType.AMOUNT -> ApiDiscountType.AMOUNT
            DiscountType.FIXED_PRICE -> ApiDiscountType.FIXED_PRICE
        }
    }

    fun mapFromApiDiscountType(type: ApiDiscountType): DiscountType {
        return when (type) {
            ApiDiscountType.PERCENTAGE -> DiscountType.PERCENTAGE
            ApiDiscountType.AMOUNT -> DiscountType.AMOUNT
            ApiDiscountType.FIXED_PRICE -> DiscountType.FIXED_PRICE
        }
    }

    /**
     * Mapper dla ServiceApprovalStatus
     */
    fun mapToServiceApprovalStatus(status: String?): ServiceApprovalStatus {
        return when (status?.uppercase()) {
            "PENDING" -> ServiceApprovalStatus.PENDING
            "APPROVED" -> ServiceApprovalStatus.APPROVED
            "REJECTED" -> ServiceApprovalStatus.REJECTED
            null -> ServiceApprovalStatus.PENDING
            else -> throw IllegalArgumentException("Invalid service approval status: $status")
        }
    }

    fun mapToApiServiceApprovalStatus(status: ServiceApprovalStatus): ApiServiceApprovalStatus {
        return when (status) {
            ServiceApprovalStatus.PENDING -> ApiServiceApprovalStatus.PENDING
            ServiceApprovalStatus.APPROVED -> ApiServiceApprovalStatus.APPROVED
            ServiceApprovalStatus.REJECTED -> ApiServiceApprovalStatus.REJECTED
        }
    }

    fun mapFromApiServiceApprovalStatus(status: ApiServiceApprovalStatus): ServiceApprovalStatus {
        return when (status) {
            ApiServiceApprovalStatus.PENDING -> ServiceApprovalStatus.PENDING
            ApiServiceApprovalStatus.APPROVED -> ServiceApprovalStatus.APPROVED
            ApiServiceApprovalStatus.REJECTED -> ServiceApprovalStatus.REJECTED
        }
    }

    /**
     * Mapper dla CommentType
     */
    fun mapToCommentType(type: String?): CommentType {
        return when (type?.uppercase()) {
            "INTERNAL" -> CommentType.INTERNAL
            "CUSTOMER" -> CommentType.CUSTOMER
            "SYSTEM" -> CommentType.SYSTEM
            null -> CommentType.INTERNAL
            else -> throw IllegalArgumentException("Invalid comment type: $type")
        }
    }

    /**
     * Mapper dla DocumentType
     */
    fun mapToDocumentType(type: String?): DocumentType {
        return when (type?.uppercase()) {
            "ACCEPTANCE_PROTOCOL" -> DocumentType.ACCEPTANCE_PROTOCOL
            "TERMS_ACCEPTANCE" -> DocumentType.TERMS_ACCEPTANCE
            "MARKETING_CONSENT" -> DocumentType.MARKETING_CONSENT
            "SERVICE_CONSENT" -> DocumentType.SERVICE_CONSENT
            "INVOICE" -> DocumentType.INVOICE
            "RECEIPT" -> DocumentType.RECEIPT
            "OTHER" -> DocumentType.OTHER
            null -> DocumentType.OTHER
            else -> DocumentType.OTHER
        }
    }
}