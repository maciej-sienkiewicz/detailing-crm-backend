package com.carslab.crm.production.modules.media.domain.model

import java.util.*

@JvmInline
value class MediaId(val value: String) {
    init {
        require(value.isNotBlank()) { "Media ID cannot be blank" }
    }

    companion object {
        fun generate(): MediaId = MediaId(UUID.randomUUID().toString())
        fun of(value: String): MediaId = MediaId(value)
    }

    override fun toString(): String = value
}