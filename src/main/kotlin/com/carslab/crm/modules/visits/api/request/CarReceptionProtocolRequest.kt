package com.carslab.crm.modules.visits.api.request

import com.fasterxml.jackson.annotation.JsonProperty

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
    @JsonProperty("other")
    OTHER,

    @JsonProperty("regular_customer")
    REGULAR_CUSTOMER,

    @JsonProperty("social_media")
    SOCIAL_MEDIA,

    @JsonProperty("local_ad")
    LOCAL_AD,

    @JsonProperty("search_engine")
    SEARCH_ENGINE,

    @JsonProperty("recommendation")
    RECOMMENDATION
}