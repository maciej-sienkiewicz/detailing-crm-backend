package com.carslab.crm.production.shared.presentation.dto

import com.fasterxml.jackson.annotation.JsonValue

enum class PriceTypeDto(@JsonValue val value: String) {
    NETTO("netto"),
    BRUTTO("brutto")
}