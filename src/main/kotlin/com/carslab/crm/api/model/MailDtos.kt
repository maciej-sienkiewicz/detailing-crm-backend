// DTO - Data Transfer Objects
package com.carslab.crm.api.model

import com.carslab.crm.domain.model.EmailAddress
import com.carslab.crm.domain.model.EmailAttachment
import com.carslab.crm.domain.model.EmailBody
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

// Żądanie logowania
data class MailLoginRequest @JsonCreator constructor(
    @JsonProperty("email") val email: String,
    @JsonProperty("password") val password: String,
    @JsonProperty("imapHost") val imapHost: String,
    @JsonProperty("imapPort") val imapPort: Int,
    @JsonProperty("imapSecure") val imapSecure: Boolean,
    @JsonProperty("smtpHost") val smtpHost: String,
    @JsonProperty("smtpPort") val smtpPort: Int,
    @JsonProperty("smtpSecure") val smtpSecure: Boolean
)

// Odpowiedź po logowaniu
data class MailLoginResponse(
    val success: Boolean,
    val token: String? = null,
    val message: String? = null
)

// Folder (etykieta) poczty
data class MailFolder(
    val name: String,
    val path: String,
    val type: String,
    val messageCount: Int = 0,
    val unreadCount: Int = 0,
    val color: String? = null
)

// Odpowiedź z listą folderów
data class MailFoldersResponse(
    val success: Boolean,
    val folders: List<MailFolder> = emptyList(),
    val message: String? = null
)

// Podsumowanie folderu
data class MailFolderSummary(
    val labelId: String,
    val name: String,
    val type: String,
    val path: String,
    val totalCount: Int,
    val unreadCount: Int,
    val color: String? = null
)

// Odpowiedź z podsumowaniem folderów
data class MailFoldersSummaryResponse(
    val success: Boolean,
    val summary: List<MailFolderSummary> = emptyList(),
    val message: String? = null
)

// Email
// Email (kontynuacja)
data class Email(
    val id: String,
    val threadId: String,
    val labelIds: List<String>,
    val snippet: String,
    val internalDate: Long,
    val subject: String,
    val from: EmailAddress,
    val to: List<EmailAddress>,
    val cc: List<EmailAddress>? = null,
    val bcc: List<EmailAddress>? = null,
    val body: EmailBody? = null,
    val attachments: List<EmailAttachment>? = null,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val isImportant: Boolean = false,
    val providerId: String? = null
)

// Odpowiedź z listą emaili
data class MailsResponse(
    val success: Boolean,
    val emails: List<Email> = emptyList(),
    val nextPageToken: String? = null,
    val resultSizeEstimate: Int = 0,
    val message: String? = null
)

// Odpowiedź z pojedynczym emailem
data class EmailResponse(
    val success: Boolean,
    val email: Email? = null,
    val message: String? = null
)

// Żądanie wysłania emaila
data class SendEmailRequest @JsonCreator constructor(
    @JsonProperty("to")
    val to: List<EmailAddress>,
    @JsonProperty("cc")
    val cc: List<EmailAddress>? = null,
    @JsonProperty("bcc")
    val bcc: List<EmailAddress>? = null,
    @JsonProperty("subject")
    val subject: String,
    @JsonProperty("body")
    val body: EmailBody,
    @JsonProperty("attachments")
    val attachments: List<AttachmentData>? = null
)

// Dane załącznika przy wysyłaniu
data class AttachmentData @JsonCreator constructor(
    @JsonProperty("filename")
    val filename: String,
    @JsonProperty("mimeType")
    val mimeType: String,
    @JsonProperty("data")
    val data: ByteArray,
    @JsonProperty("size")
    val size: Int
) {
    // Override equals dla porównywania obiektów z ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachmentData

        if (filename != other.filename) return false
        if (mimeType != other.mimeType) return false
        if (!data.contentEquals(other.data)) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + size
        return result
    }
}

// Odpowiedź po wysłaniu emaila
data class SendEmailResponse(
    val success: Boolean,
    val messageId: String? = null,
    val message: String? = null
)

// Żądanie oznaczenia emaila jako przeczytany/nieprzeczytany
data class MarkAsReadRequest @JsonCreator constructor(
    @JsonProperty("isRead")
    val isRead: Boolean
)

// Żądanie oznaczenia emaila flagą
data class ToggleFlagRequest(
    val flagName: String,
    val value: Boolean
)

// Żądanie przeniesienia emaila do innego folderu
data class MoveEmailRequest(
    val sourceFolder: String? = null,
    val destinationFolder: String
)

// Żądanie przeniesienia emaila do kosza
data class MoveToTrashRequest(
    val messageId: Int,
    val folderPath: String? = null
)

// Żądanie aktualizacji folderów emaila
data class UpdateFoldersRequest(
    val addFolders: List<String>,
    val removeFolders: List<String>
)

// Odpowiedź po wykonaniu akcji na emailu
data class MailActionResponse(
    val success: Boolean,
    val message: String? = null
)

// Żądanie utworzenia folderu
data class CreateFolderRequest(
    val name: String,
    val color: String? = null
)

// Odpowiedź po utworzeniu folderu
data class CreateFolderResponse(
    val success: Boolean,
    val folder: MailFolder? = null,
    val message: String? = null
)

// Odpowiedź z załącznikiem
data class AttachmentResponse(
    val success: Boolean,
    val data: ByteArray? = null,
    val filename: String? = null,
    val mimeType: String? = null,
    val message: String? = null
) {
    // Override equals dla porównywania obiektów z ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachmentResponse

        if (success != other.success) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (filename != other.filename) return false
        if (mimeType != other.mimeType) return false
        if (message != other.message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (filename?.hashCode() ?: 0)
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }
}