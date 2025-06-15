// src/main/kotlin/com/carslab/crm/domain/model/view/protocol/ProtocolDocumentView.kt
package com.carslab.crm.domain.model.view.protocol

import com.carslab.crm.domain.model.ProtocolId
import java.time.LocalDateTime

/**
 * Model domenowy reprezentujący dokument przypisany do protokołu.
 * Dokumenty to zgody, dodatkowe umowy, itp.
 */
data class ProtocolDocumentView(
    val storageId: String,
    val protocolId: ProtocolId,
    val originalName: String,
    val fileSize: Long,
    val contentType: String,
    val documentType: String, // "MARKETING_CONSENT", "SERVICE_CONSENT", "OTHER"
    val description: String? = null,
    val createdAt: LocalDateTime,
    val uploadedBy: String
)

/**
 * Typy dokumentów protokołu
 */
enum class ProtocolDocumentType(val displayName: String) {
    MARKETING_CONSENT("Zgoda marketingowa"),
    SERVICE_CONSENT("Zgoda na dodatkowe usługi"),
    TERMS_ACCEPTANCE("Akceptacja regulaminu"),
    PRIVACY_POLICY("Polityka prywatności"),
    DAMAGE_WAIVER("Zwolnienie z odpowiedzialności"),
    OTHER("Inny dokument");

    companion object {
        fun fromString(value: String): ProtocolDocumentType {
            return values().find { it.name == value.uppercase() } ?: OTHER
        }
    }
}