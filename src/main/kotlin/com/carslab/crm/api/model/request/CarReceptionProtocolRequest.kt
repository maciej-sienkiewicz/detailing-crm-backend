package com.carslab.crm.api.model.request

enum class ApiDiscountType {
    PERCENTAGE,
    AMOUNT,
    FIXED_PRICE
}

enum class ServiceApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

enum class ApiReferralSource {
    REGULAR_CUSTOMER,
    RECOMMENDATION,
    SEARCH_ENGINE,
    SOCIAL_MEDIA,
    LOCAL_AD,
    OTHER
}