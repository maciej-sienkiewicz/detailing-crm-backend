package com.carslab.crm.production.shared.presentation.dto

import com.fasterxml.jackson.annotation.JsonValue

/**
 * Typy rabatów dla API (warstwa prezentacji).
 *
 * Mapuje się 1:1 na domenowy DiscountType, ale używa snake_case dla JSON.
 */
enum class DiscountTypeDto(@JsonValue val value: String) {
    PERCENT("percent"),
    FIXED_AMOUNT_OFF_BRUTTO("fixed_amount_off_brutto"),
    FIXED_AMOUNT_OFF_NETTO("fixed_amount_off_netto"),
    FIXED_FINAL_BRUTTO("fixed_final_brutto"),
    FIXED_FINAL_NETTO("fixed_final_netto")
}