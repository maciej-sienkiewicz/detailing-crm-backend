package com.carslab.crm.api.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDate
import java.time.LocalDateTime

data class ContactAttemptRequest(
    @JsonProperty("id")
    var id: String? = null,

    @JsonProperty("client_id")
    val clientId: String,

    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    val date: LocalDateTime,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("result")
    val result: String
)

class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDateTime {
        val dateString = p.valueAsString
        return try {
            LocalDate.parse(dateString).atStartOfDay()
        } catch (e: Exception) {
            LocalDateTime.parse(dateString)
        }
    }
}