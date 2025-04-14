// Model classes
package com.carslab.crm.domain.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.sun.mail.imap.IMAPStore
import jakarta.activation.DataSource
import jakarta.mail.Session
import java.time.Instant

// Klasa przechowująca aktywną sesję IMAP
data class MailSession(
    val token: String,
    val email: String,
    val password: String,
    val imapHost: String,
    val imapPort: Int,
    val imapSecure: Boolean,
    val smtpHost: String,
    val smtpPort: Int,
    val smtpSecure: Boolean,
    val store: IMAPStore,
    val session: Session,
    val createdAt: Instant
)

// Adres email
data class EmailAddress @JsonCreator constructor(
    @JsonProperty("email")
    val email: String,
    @JsonProperty("name")
    val name: String? = null
)

// Treść emaila
data class EmailBody(
    val plain: String? = null,
    val html: String? = null
)

// Załącznik emaila
data class EmailAttachment(
    val id: String,
    val filename: String,
    val mimeType: String,
    val size: Int,
    val data: ByteArray? = null
) {
    // Override equals dla porównywania obiektów z ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmailAttachment

        if (id != other.id) return false
        if (filename != other.filename) return false
        if (mimeType != other.mimeType) return false
        if (size != other.size) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + size
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}

// Klasa obsługująca binarny źródło danych dla załączników
class ByteArrayDataSource(
    private val data: ByteArray,
    private val type: String
) : DataSource {
    override fun getContentType(): String = type
    override fun getInputStream(): java.io.InputStream = java.io.ByteArrayInputStream(data)
    override fun getName(): String = "ByteArrayDataSource"
    override fun getOutputStream(): java.io.OutputStream = throw UnsupportedOperationException("Not supported")
}