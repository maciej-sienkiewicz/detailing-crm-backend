package com.carslab.crm.modules.communication.infrastructure.mail

import com.carslab.crm.modules.communication.application.IngestService
import com.carslab.crm.modules.communication.application.RawIncomingEmail
import com.carslab.crm.modules.communication.domain.model.MailAccountStatus
import com.carslab.crm.modules.communication.domain.model.MailProviderType
import com.carslab.crm.modules.communication.domain.model.MessageDirection
import com.carslab.crm.modules.communication.domain.services.ThreadingService
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.MailAccountEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.MailAccountJpaRepository
import com.carslab.crm.modules.company_settings.domain.port.EncryptionService
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.Folder
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.UIDFolder
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeUtility
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Properties

/**
 * IMAP polling adapter. Tracks UIDVALIDITY + last seen UID per folder; a
 * UIDVALIDITY change triggers a windowed resync. First sync ingests the last
 * 30 days of INBOX and the Sent folder (replies sent from the owner's phone
 * outside the CRM must land in threads too).
 */
@Service
class ImapSyncService(
    private val accountRepository: MailAccountJpaRepository,
    private val ingestService: IngestService,
    private val encryptionService: EncryptionService
) {
    private val logger = LoggerFactory.getLogger(ImapSyncService::class.java)

    private val sentFolderNames = listOf(
        "Sent", "Sent Items", "Sent Messages", "Wysłane", "Wyslane",
        "Elementy wysłane", "[Gmail]/Sent Mail", "INBOX.Sent"
    )

    @Scheduled(fixedDelay = 300_000, initialDelay = 30_000)
    fun syncAllAccounts() {
        val accounts = accountRepository.findByStatusAndProviderType(
            MailAccountStatus.ACTIVE, MailProviderType.IMAP_SMTP
        )
        for (account in accounts) {
            try {
                syncAccount(account)
            } catch (e: Exception) {
                logger.error("IMAP sync failed for account ${account.id}", e)
            }
        }
    }

    fun syncAccount(account: MailAccountEntity) {
        val host = account.imapHost ?: return
        val password = account.encryptedPassword?.let { encryptionService.decrypt(it) } ?: return

        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", host)
            put("mail.imaps.port", (account.imapPort ?: 993).toString())
            put("mail.imaps.connectiontimeout", "15000")
            put("mail.imaps.timeout", "30000")
        }
        val session = Session.getInstance(props)
        var store: Store? = null
        try {
            store = session.getStore("imaps")
            store.connect(host, account.emailAddress, password)

            val inboxState = syncFolder(
                account, store, "INBOX", MessageDirection.INBOUND,
                account.inboxUidValidity, account.inboxLastUid
            )
            account.inboxUidValidity = inboxState?.first
            account.inboxLastUid = inboxState?.second ?: account.inboxLastUid

            findSentFolder(store)?.let { sentName ->
                val sentState = syncFolder(
                    account, store, sentName, MessageDirection.OUTBOUND,
                    account.sentUidValidity, account.sentLastUid
                )
                account.sentUidValidity = sentState?.first
                account.sentLastUid = sentState?.second ?: account.sentLastUid
            }

            account.status = MailAccountStatus.ACTIVE
            account.lastError = null
            account.lastSyncAt = LocalDateTime.now()
        } catch (e: AuthenticationFailedException) {
            account.status = MailAccountStatus.AUTH_FAILED
            account.lastError = "Logowanie do skrzynki odrzucone — połącz konto ponownie"
            logger.warn("IMAP auth failed for account ${account.id}")
        } catch (e: Exception) {
            account.lastError = (e.message ?: e.javaClass.simpleName).take(500)
            throw e
        } finally {
            accountRepository.save(account)
            runCatching { store?.close() }
        }
    }

    /** Returns (uidValidity, lastSeenUid) or null when the folder is missing. */
    private fun syncFolder(
        account: MailAccountEntity,
        store: Store,
        folderName: String,
        direction: MessageDirection,
        knownUidValidity: Long?,
        knownLastUid: Long?
    ): Pair<Long, Long>? {
        val folder = store.getFolder(folderName)
        if (!folder.exists()) return null
        folder.open(Folder.READ_ONLY)
        try {
            val uidFolder = folder as UIDFolder
            val uidValidity = uidFolder.uidValidity
            val startUid = if (knownUidValidity == uidValidity && knownLastUid != null) knownLastUid + 1 else 1L

            val messages = uidFolder.getMessagesByUID(startUid, UIDFolder.LASTUID)
            var maxUid = knownLastUid.takeIf { knownUidValidity == uidValidity } ?: 0L
            val firstSyncCutoff = LocalDateTime.now().minusDays(INITIAL_SYNC_WINDOW_DAYS)

            for (msg in messages) {
                val uid = uidFolder.getUID(msg)
                if (uid <= maxUid) continue
                maxUid = maxOf(maxUid, uid)
                val mime = msg as? MimeMessage ?: continue
                val sentAt = (mime.sentDate ?: mime.receivedDate)
                    ?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
                    ?: LocalDateTime.now()
                if (startUid == 1L && sentAt.isBefore(firstSyncCutoff)) continue

                try {
                    ingestService.ingest(account, toRawEmail(mime, direction, sentAt, uid))
                } catch (e: Exception) {
                    logger.error("Ingest failed for account ${account.id} uid $uid", e)
                }
            }
            return uidValidity to maxUid
        } finally {
            runCatching { folder.close(false) }
        }
    }

    private fun findSentFolder(store: Store): String? {
        for (name in sentFolderNames) {
            if (runCatching { store.getFolder(name).exists() }.getOrDefault(false)) return name
        }
        return null
    }

    private fun toRawEmail(
        mime: MimeMessage,
        direction: MessageDirection,
        sentAt: LocalDateTime,
        uid: Long
    ): RawIncomingEmail {
        val from = (mime.from?.firstOrNull() as? InternetAddress)
        val to = mime.getRecipients(jakarta.mail.Message.RecipientType.TO)
            ?.filterIsInstance<InternetAddress>()
            ?.map { it.address.lowercase() }
            .orEmpty()

        val bodies = extractBodies(mime)

        val headerNames = listOf("List-Unsubscribe", "List-Id", "Auto-Submitted", "Precedence")
        val headers = headerNames.mapNotNull { name ->
            mime.getHeader(name, null)?.let { name.lowercase() to it }
        }.toMap()

        return RawIncomingEmail(
            messageId = ThreadingService.normalizeMessageId(mime.messageID),
            inReplyTo = ThreadingService.normalizeMessageId(mime.getHeader("In-Reply-To", null)),
            references = ThreadingService.parseReferences(mime.getHeader("References", null)),
            fromEmail = from?.address?.lowercase() ?: "unknown@unknown",
            fromName = from?.personal?.let { runCatching { MimeUtility.decodeText(it) }.getOrDefault(it) },
            toEmails = to,
            subject = mime.subject,
            sentAt = sentAt,
            bodyHtml = bodies.second,
            bodyText = bodies.first,
            hasAttachments = bodies.third,
            providerUid = uid.toString(),
            direction = direction,
            headers = headers
        )
    }

    /** Returns (plainText, html, hasAttachments). */
    private fun extractBodies(part: Part): Triple<String?, String?, Boolean> {
        var text: String? = null
        var html: String? = null
        var attachments = false

        fun walk(p: Part) {
            when {
                p.isMimeType("text/plain") && text == null && !isAttachment(p) ->
                    text = p.content?.toString()
                p.isMimeType("text/html") && html == null && !isAttachment(p) ->
                    html = p.content?.toString()
                p.isMimeType("multipart/*") -> {
                    val mp = p.content as? Multipart ?: return
                    for (i in 0 until mp.count) walk(mp.getBodyPart(i))
                }
                isAttachment(p) -> attachments = true
            }
        }
        runCatching { walk(part) }
        return Triple(text, html, attachments)
    }

    private fun isAttachment(p: Part): Boolean =
        runCatching { Part.ATTACHMENT.equals(p.disposition, ignoreCase = true) }.getOrDefault(false)

    companion object {
        const val INITIAL_SYNC_WINDOW_DAYS = 30L
    }
}
