package com.carslab.crm.domain

import com.carslab.crm.api.model.*
import com.carslab.crm.domain.model.EmailAddress
import com.carslab.crm.domain.model.EmailAttachment
import com.carslab.crm.domain.model.EmailBody
import com.carslab.crm.domain.model.MailSession
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPStore
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.springframework.scheduling.annotation.Scheduled
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class MailService {

    // Przechowywanie aktywnych sesji - w produkcji lepiej użyć bazy danych lub Redis
    private val activeSessions = ConcurrentHashMap<String, MailSession>()

    // Mapowanie typów folderów IMAP na standardowe typy
    private val folderTypeMapping = mapOf(
        "inbox" to "inbox",
        "sent" to "sent",
        "drafts" to "draft",
        "trash" to "trash",
        "junk" to "spam",
        "spam" to "spam",
        "flagged" to "starred",
        "important" to "important"
    )

    // Logowanie do serwera pocztowego
    fun loginToMailServer(request: MailLoginRequest): MailLoginResponse {
        try {
            // Konfiguracja właściwości sesji
            val props = Properties()

            // Ustawienia IMAP
            props["mail.store.protocol"] = "imap"
            props["mail.imap.host"] = request.imapHost
            props["mail.imap.port"] = request.imapPort

            if (request.imapSecure) {
                props["mail.imap.ssl.enable"] = "true"
                props["mail.imap.ssl.trust"] = "*"
            }

            // Ustawienia SMTP
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.host"] = request.smtpHost
            props["mail.smtp.port"] = request.smtpPort

            if (request.smtpSecure) {
                props["mail.smtp.ssl.enable"] = "true"
                props["mail.smtp.ssl.trust"] = "*"
            } else {
                props["mail.smtp.starttls.enable"] = "true"
            }

            // Tworzenie sesji
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(request.email, request.password)
                }
            })

            // Próba połączenia z serwerem IMAP
            val store = session.getStore("imap")
            store.connect(request.imapHost, request.imapPort, request.email, request.password)

            // Generowanie tokenu sesji
            val token = UUID.randomUUID().toString()

            // Zapisywanie sesji
            activeSessions[token] = MailSession(
                token = token,
                email = request.email,
                password = request.password,
                imapHost = request.imapHost,
                imapPort = request.imapPort,
                imapSecure = request.imapSecure,
                smtpHost = request.smtpHost,
                smtpPort = request.smtpPort,
                smtpSecure = request.smtpSecure,
                store = store as IMAPStore,
                session = session,
                createdAt = Instant.now()
            )

            return MailLoginResponse(
                success = true,
                token = token,
                message = "Zalogowano pomyślnie"
            )

        } catch (e: AuthenticationFailedException) {
            return MailLoginResponse(
                success = false,
                message = "Błąd uwierzytelniania: Nieprawidłowe dane logowania"
            )
        } catch (e: MessagingException) {
            return MailLoginResponse(
                success = false,
                message = "Błąd połączenia: ${e.message}"
            )
        } catch (e: Exception) {
            return MailLoginResponse(
                success = false,
                message = "Wystąpił nieoczekiwany błąd: ${e.message}"
            )
        }
    }

    // Pobieranie folderów poczty
    fun getFolders(token: String): MailFoldersResponse {
        val session = getSession(token) ?: return MailFoldersResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            val store = session.store
            val rootFolder = store.defaultFolder
            val folders = mutableListOf<MailFolder>()

            // Pobieranie wszystkich folderów
            getAllFolders(rootFolder, folders)

            return MailFoldersResponse(
                success = true,
                folders = folders
            )

        } catch (e: Exception) {
            return MailFoldersResponse(
                success = false,
                message = "Błąd pobierania folderów: ${e.message}"
            )
        }
    }

    // Pobieranie podsumowania folderów
    fun getFoldersSummary(token: String): MailFoldersSummaryResponse {
        val session = getSession(token) ?: return MailFoldersSummaryResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            val store = session.store
            val rootFolder = store.defaultFolder
            val summaries = mutableListOf<MailFolderSummary>()

            // Pobieranie podsumowań dla wszystkich folderów
            getFoldersSummaryRecursive(rootFolder, summaries)

            return MailFoldersSummaryResponse(
                success = true,
                summary = summaries
            )

        } catch (e: Exception) {
            return MailFoldersSummaryResponse(
                success = false,
                message = "Błąd pobierania podsumowania folderów: ${e.message}"
            )
        }
    }

    // Pobieranie emaili z określonego folderu
    fun getEmails(token: String, folderPath: String, page: Int, pageSize: Int, pageToken: String? = null): MailsResponse {
        val session = getSession(token) ?: return MailsResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            val store = session.store
            val folder = store.getFolder(folderPath)
            folder.open(Folder.READ_ONLY)

            val totalMessages = folder.messageCount

            // Jeśli folder jest pusty
            if (totalMessages == 0) {
                folder.close(false)
                return MailsResponse(
                    success = true,
                    emails = emptyList(),
                    nextPageToken = null,
                    resultSizeEstimate = 0
                )
            }

            // Kalkulacja zakresu wiadomości dla paginacji
            val startIdx = if (pageToken != null) {
                // Używamy pageToken jako indeksu początkowego
                pageToken.toInt()
            } else {
                // Domyślnie pobieramy najnowsze wiadomości
                Math.max(1, totalMessages - ((page - 1) * pageSize))
            }

            val endIdx = Math.max(1, startIdx - pageSize + 1)

            // Pobieranie wiadomości
            val messages = if (startIdx > endIdx) {
                folder.getMessages(endIdx, startIdx)
            } else {
                emptyArray()
            }

            // Konwersja wiadomości do DTO
            val emails = messages.reversed().map { message ->
                convertMessageToEmail(message, folder.fullName)
            }

            // Generowanie tokenu dla następnej strony
            val nextPageToken = if (endIdx > 1) {
                (endIdx - 1).toString()
            } else {
                null
            }

            folder.close(false)

            return MailsResponse(
                success = true,
                emails = emails,
                nextPageToken = nextPageToken,
                resultSizeEstimate = totalMessages
            )

        } catch (e: Exception) {
            return MailsResponse(
                success = false,
                message = "Błąd pobierania wiadomości: ${e.message}"
            )
        }
    }

    // Wyszukiwanie emaili
    fun searchEmails(token: String, folderPath: String, query: String, page: Int, pageSize: Int): MailsResponse {
        val session = getSession(token) ?: return MailsResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            val store = session.store
            val folder = store.getFolder(folderPath)
            folder.open(Folder.READ_ONLY)

            // Implementacja wyszukiwania - to jest uproszczone, w rzeczywistości należy użyć
            // bardziej zaawansowanych kryteriów wyszukiwania IMAP
            val messages = folder.messages

            val matchingMessages = messages.filter { message ->
                val subject = message.subject ?: ""
                val from = (message.from?.firstOrNull() as? InternetAddress)?.address ?: ""
                val content = getMessageContent(message)

                subject.contains(query, ignoreCase = true) ||
                        from.contains(query, ignoreCase = true) ||
                        content.contains(query, ignoreCase = true)
            }

            // Paginacja wyników
            val startIdx = (page - 1) * pageSize
            val endIdx = Math.min(startIdx + pageSize, matchingMessages.size)

            val pagedMessages = if (startIdx < matchingMessages.size) {
                matchingMessages.subList(startIdx, endIdx)
            } else {
                emptyList()
            }

            // Konwersja wiadomości do DTO
            val emails = pagedMessages.map { message ->
                convertMessageToEmail(message, folder.fullName)
            }

            folder.close(false)

            return MailsResponse(
                success = true,
                emails = emails,
                nextPageToken = if (endIdx < matchingMessages.size) endIdx.toString() else null,
                resultSizeEstimate = matchingMessages.size
            )

        } catch (e: Exception) {
            return MailsResponse(
                success = false,
                message = "Błąd wyszukiwania wiadomości: ${e.message}"
            )
        }
    }

    // Pobieranie pojedynczej wiadomości
    fun getEmail(token: String, messageId: String): EmailResponse {
        val session = getSession(token) ?: return EmailResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            // W rzeczywistej implementacji trzeba podzielić messageId na folder i UID
            val parts = messageId.split(":")
            val folderPath = parts[0]
            val uid = parts[1].toLong()

            val store = session.store
            val folder = store.getFolder(folderPath) as IMAPFolder
            folder.open(Folder.READ_WRITE)

            // Pobieranie wiadomości po UID
            val message = folder.getMessageByUID(uid)

            if (message == null) {
                folder.close(false)
                return EmailResponse(
                    success = false,
                    message = "Wiadomość nie została znaleziona"
                )
            }

            // Konwersja wiadomości do DTO z pełną treścią i załącznikami
            val email = convertMessageToEmail(message, folderPath, true, true)

            // Oznaczenie wiadomości jako przeczytanej jeśli jeszcze nie jest
            if (!message.isSet(Flags.Flag.SEEN)) {
                message.setFlag(Flags.Flag.SEEN, true)
            }

            folder.close(false)

            return EmailResponse(
                success = true,
                email = email
            )

        } catch (e: Exception) {
            return EmailResponse(
                success = false,
                message = "Błąd pobierania wiadomości: ${e.message}"
            )
        }
    }

    // Pobieranie załącznika
    fun getAttachment(token: String, messageId: String, attachmentId: String): AttachmentResponse {
        val session = getSession(token) ?: return AttachmentResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            // W rzeczywistej implementacji trzeba podzielić messageId na folder i UID
            val parts = messageId.split(":")
            val folderPath = parts[0]
            val uid = parts[1].toLong()

            val store = session.store
            val folder = store.getFolder(folderPath) as IMAPFolder
            folder.open(Folder.READ_ONLY)

            // Pobieranie wiadomości po UID
            val message = folder.getMessageByUID(uid)

            if (message == null) {
                folder.close(false)
                return AttachmentResponse(
                    success = false,
                    message = "Wiadomość nie została znaleziona"
                )
            }

            // Pobieranie załącznika z wiadomości
            val attachment = getAttachmentFromMessage(message, attachmentId)

            folder.close(false)

            return if (attachment != null) {
                AttachmentResponse(
                    success = true,
                    data = attachment.data,
                    filename = attachment.filename,
                    mimeType = attachment.mimeType
                )
            } else {
                AttachmentResponse(
                    success = false,
                    message = "Załącznik nie został znaleziony"
                )
            }

        } catch (e: Exception) {
            return AttachmentResponse(
                success = false,
                message = "Błąd pobierania załącznika: ${e.message}"
            )
        }
    }

    // Wysyłanie emaila
    fun sendEmail(token: String, request: SendEmailRequest): SendEmailResponse {
        val session = getSession(token) ?: return SendEmailResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            // Konfiguracja właściwości SMTP
            val props = Properties()
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.host"] = session.smtpHost
            props["mail.smtp.port"] = session.smtpPort

            if (session.smtpSecure) {
                props["mail.smtp.ssl.enable"] = "true"
                props["mail.smtp.ssl.trust"] = "*"
            } else {
                props["mail.smtp.starttls.enable"] = "true"
            }

            // Tworzenie sesji
            val mailSession = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(session.email, session.password)
                }
            })

            // Tworzenie wiadomości
            val message = MimeMessage(mailSession)

            // Ustawienie nadawcy
            message.setFrom(InternetAddress(session.email))

            // Dodawanie odbiorców
            request.to.forEach { recipient ->
                message.addRecipient(
                    Message.RecipientType.TO,
                    InternetAddress(recipient.email, recipient.name ?: "")
                )
            }

            // Dodawanie odbiorców CC
            request.cc?.forEach { recipient ->
                message.addRecipient(
                    Message.RecipientType.CC,
                    InternetAddress(recipient.email, recipient.name ?: "")
                )
            }

            // Dodawanie odbiorców BCC
            request.bcc?.forEach { recipient ->
                message.addRecipient(
                    Message.RecipientType.BCC,
                    InternetAddress(recipient.email, recipient.name ?: "")
                )
            }

            // Ustawienie tematu
            message.subject = request.subject

            // Ustawienie treści i załączników
            if (request.attachments.isNullOrEmpty()) {
                // Tylko treść
                message.setContent(
                    request.body.html ?: request.body.plain,
                    if (request.body.html != null) "text/html; charset=UTF-8" else "text/plain; charset=UTF-8"
                )
            } else {
                // Treść z załącznikami
                val multipart = MimeMultipart()

                // Dodanie treści
                val bodyPart = MimeBodyPart()
                bodyPart.setContent(
                    request.body.html ?: request.body.plain,
                    if (request.body.html != null) "text/html; charset=UTF-8" else "text/plain; charset=UTF-8"
                )
                multipart.addBodyPart(bodyPart)

                // Dodanie załączników
                request.attachments.forEach { attachment ->
                    val attachmentPart = MimeBodyPart()
                    // Tu powinno być dodane obsługiwanie danych załącznika
                    // W rzeczywistej implementacji należy obsłużyć dane binarne
                    multipart.addBodyPart(attachmentPart)
                }

                message.setContent(multipart)
            }

            // Wysłanie wiadomości
            Transport.send(message)

            return SendEmailResponse(
                success = true,
                messageId = message.messageID,
                message = "Wiadomość została wysłana"
            )

        } catch (e: Exception) {
            return SendEmailResponse(
                success = false,
                message = "Błąd wysyłania wiadomości: ${e.message}"
            )
        }
    }

    // Zapisywanie wersji roboczej wiadomości
    fun saveDraft(token: String, request: SendEmailRequest): SendEmailResponse {
        val session = getSession(token) ?: return SendEmailResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            val store = session.store

            // Pobieranie folderu wersji roboczych
            val draftsFolder = findDraftsFolder(store)

            if (draftsFolder == null) {
                return SendEmailResponse(
                    success = false,
                    message = "Nie znaleziono folderu wersji roboczych"
                )
            }

            draftsFolder.open(Folder.READ_WRITE)

            // Tworzenie wiadomości
            val message = MimeMessage(session.session as Session)

            // Ustawienie nadawcy
            message.setFrom(InternetAddress(session.email))

            // Dodawanie odbiorców
            request.to.forEach { recipient ->
                message.addRecipient(
                    Message.RecipientType.TO,
                    InternetAddress(recipient.email, recipient.name ?: "")
                )
            }

            // Dodawanie odbiorców CC
            request.cc?.forEach { recipient ->
                message.addRecipient(
                    Message.RecipientType.CC,
                    InternetAddress(recipient.email, recipient.name ?: "")
                )
            }

            // Dodawanie odbiorców BCC
            request.bcc?.forEach { recipient ->
                message.addRecipient(
                    Message.RecipientType.BCC,
                    InternetAddress(recipient.email, recipient.name ?: "")
                )
            }

            // Ustawienie tematu
            message.subject = request.subject

            // Ustawienie treści i załączników
            if (request.attachments.isNullOrEmpty()) {
                // Tylko treść
                message.setContent(
                    request.body.html ?: request.body.plain,
                    if (request.body.html != null) "text/html; charset=UTF-8" else "text/plain; charset=UTF-8"
                )
            } else {
                // Treść z załącznikami
                val multipart = MimeMultipart()

                // Dodanie treści
                val bodyPart = MimeBodyPart()
                bodyPart.setContent(
                    request.body.html ?: request.body.plain,
                    if (request.body.html != null) "text/html; charset=UTF-8" else "text/plain; charset=UTF-8"
                )
                multipart.addBodyPart(bodyPart)

                // Dodanie załączników
                request.attachments.forEach { attachment ->
                    val attachmentPart = MimeBodyPart()
                    // Tu powinno być dodane obsługiwanie danych załącznika
                    // W rzeczywistej implementacji należy obsłużyć dane binarne
                    multipart.addBodyPart(attachmentPart)
                }

                message.setContent(multipart)
            }

            // Zapisywanie wiadomości do folderu wersji roboczych
            message.setFlag(Flags.Flag.DRAFT, true)

            // Zapisanie wiadomości jako draft
            draftsFolder.appendMessages(arrayOf(message))

            // Pobieranie UID zapisanej wiadomości
            val savedMessage = findMessageBySubjectAndDate(draftsFolder, message.subject, message.sentDate)
            val uid = if (savedMessage != null) (draftsFolder as IMAPFolder).getUID(savedMessage) else 0

            draftsFolder.close(false)

            return SendEmailResponse(
                success = true,
                messageId = "${draftsFolder.fullName}:$uid",
                message = "Wersja robocza została zapisana"
            )

        } catch (e: Exception) {
            return SendEmailResponse(
                success = false,
                message = "Błąd zapisywania wersji roboczej: ${e.message}"
            )
        }
    }

    // Oznaczanie wiadomości jako przeczytana/nieprzeczytana
    fun markAsRead(token: String, messageId: String, isRead: Boolean): MailActionResponse {
        val session = getSession(token) ?: return MailActionResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            // W rzeczywistej implementacji trzeba podzielić messageId na folder i UID
            val parts = messageId.split(":")
            val folderPath = parts[0]
            val uid = parts[1].toLong()

            val store = session.store
            val folder = store.getFolder(folderPath) as IMAPFolder
            folder.open(Folder.READ_WRITE)

            // Pobieranie wiadomości po UID
            val message = folder.getMessageByUID(uid)

            if (message == null) {
                folder.close(false)
                return MailActionResponse(
                    success = false,
                    message = "Wiadomość nie została znaleziona"
                )
            }

            // Oznaczenie wiadomości jako przeczytana/nieprzeczytana
            message.setFlag(Flags.Flag.SEEN, isRead)

            folder.close(false)

            return MailActionResponse(
                success = true,
                message = "Status odczytu zmieniony pomyślnie"
            )

        } catch (e: Exception) {
            return MailActionResponse(
                success = false,
                message = "Błąd zmiany statusu odczytu: ${e.message}"
            )
        }
    }

    // Oznaczanie wiadomości flagą (np. gwiazdka)
    fun toggleFlag(token: String, messageId: String, flagName: String, value: Boolean): MailActionResponse {
        val session = getSession(token) ?: return MailActionResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            // W rzeczywistej implementacji trzeba podzielić messageId na folder i UID
            val parts = messageId.split(":")
            val folderPath = parts[0]
            val uid = parts[1].toLong()

            val store = session.store
            val folder = store.getFolder(folderPath) as IMAPFolder
            folder.open(Folder.READ_WRITE)

            // Pobieranie wiadomości po UID
            val message = folder.getMessageByUID(uid)

            if (message == null) {
                folder.close(false)
                return MailActionResponse(
                    success = false,
                    message = "Wiadomość nie została znaleziona"
                )
            }

            // Mapowanie nazwy flagi na obiekt Flags.Flag
            val flag = when (flagName) {
                "\\Flagged" -> Flags.Flag.FLAGGED
                "\\Answered" -> Flags.Flag.ANSWERED
                "\\Deleted" -> Flags.Flag.DELETED
                "\\Draft" -> Flags.Flag.DRAFT
                "\\Seen" -> Flags.Flag.SEEN
                else -> null
            }

            if (flag != null) {
                // Zmiana flagi
                message.setFlag(flag, value)
            } else {
                // Dla niestandardowych flag
                message.setFlags(Flags(flagName), value)
            }

            folder.close(false)

            return MailActionResponse(
                success = true,
                message = "Flaga zmieniona pomyślnie"
            )

        } catch (e: Exception) {
            return MailActionResponse(
                success = false,
                message = "Błąd zmiany flagi: ${e.message}"
            )
        }
    }

    // Przenoszenie wiadomości do innego folderu
    fun moveEmail(token: String, messageId: String, destinationFolderPath: String): MailActionResponse {
        val session = getSession(token) ?: return MailActionResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            // W rzeczywistej implementacji trzeba podzielić messageId na folder i UID
            val parts = messageId.split(":")
            val sourceFolderPath = parts[0]
            val uid = parts[1].toLong()

            val store = session.store
            val sourceFolder = store.getFolder(sourceFolderPath) as IMAPFolder
            val destinationFolder = store.getFolder(destinationFolderPath)

            sourceFolder.open(Folder.READ_WRITE)
            if (!destinationFolder.exists()) {
                destinationFolder.create(Folder.HOLDS_MESSAGES)
            }
            destinationFolder.open(Folder.READ_WRITE)

            // Pobieranie wiadomości po UID
            val message = sourceFolder.getMessageByUID(uid)

            if (message == null) {
                sourceFolder.close(false)
                destinationFolder.close(false)
                return MailActionResponse(
                    success = false,
                    message = "Wiadomość nie została znaleziona"
                )
            }

            // Kopiowanie wiadomości do docelowego folderu
            sourceFolder.copyMessages(arrayOf(message), destinationFolder)

            // Oznaczenie wiadomości jako usuniętej w źródłowym folderze
            message.setFlag(Flags.Flag.DELETED, true)

            // Ekspunge - fizyczne usunięcie oznaczonych wiadomości
            sourceFolder.expunge()

            sourceFolder.close(false)
            destinationFolder.close(false)

            return MailActionResponse(
                success = true,
                message = "Wiadomość przeniesiona pomyślnie"
            )

        } catch (e: Exception) {
            return MailActionResponse(
                success = false,
                message = "Błąd przenoszenia wiadomości: ${e.message}"
            )
        }
    }

    // Aktualizacja folderów wiadomości (dodawanie/usuwanie)
    fun updateFolders(token: String, messageId: String, addFolders: List<String>, removeFolders: List<String>): MailActionResponse {
        val session = getSession(token) ?: return MailActionResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            // W rzeczywistej implementacji trzeba podzielić messageId na folder i UID
            val parts = messageId.split(":")
            val currentFolderPath = parts[0]
            val uid = parts[1].toLong()

            val store = session.store
            val currentFolder = store.getFolder(currentFolderPath) as IMAPFolder
            currentFolder.open(Folder.READ_WRITE)

            // Pobieranie wiadomości po UID
            val message = currentFolder.getMessageByUID(uid)

            if (message == null) {
                currentFolder.close(false)
                return MailActionResponse(
                    success = false,
                    message = "Wiadomość nie została znaleziona"
                )
            }

            // Dodawanie do nowych folderów
            for (folderPath in addFolders) {
                if (folderPath != currentFolderPath) {
                    val targetFolder = store.getFolder(folderPath)
                    if (!targetFolder.exists()) {
                        targetFolder.create(Folder.HOLDS_MESSAGES)
                    }
                    targetFolder.open(Folder.READ_WRITE)
                    currentFolder.copyMessages(arrayOf(message), targetFolder)
                    targetFolder.close(false)
                }
            }

            // Usuwanie z folderów
            for (folderPath in removeFolders) {
                if (folderPath != currentFolderPath) {
                    val targetFolder = store.getFolder(folderPath) as IMAPFolder
                    targetFolder.open(Folder.READ_WRITE)

                    // Wyszukiwanie wiadomości o tym samym UID w innych folderach jest trudne
                    // W rzeczywistej implementacji potrzebny byłby bardziej zaawansowany mechanizm
                    // Tu jako przykład, próbujemy znaleźć wiadomość o tym samym temacie i dacie
                    val matchingMessages = findMessagesBySubjectAndDate(
                        targetFolder,
                        message.subject,
                        message.sentDate
                    )

                    for (matchingMessage in matchingMessages) {
                        matchingMessage.setFlag(Flags.Flag.DELETED, true)
                    }

                    targetFolder.expunge()
                    targetFolder.close(false)
                }
            }

            currentFolder.close(false)

            return MailActionResponse(
                success = true,
                message = "Foldery zaktualizowane pomyślnie"
            )

        } catch (e: Exception) {
            return MailActionResponse(
                success = false,
                message = "Błąd aktualizacji folderów: ${e.message}"
            )
        }
    }

    // Trwałe usuwanie wiadomości
    fun permanentlyDelete(token: String, messageId: String): MailActionResponse {
        val session = getSession(token) ?: return MailActionResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            // W rzeczywistej implementacji trzeba podzielić messageId na folder i UID
            val parts = messageId.split(":")
            val folderPath = parts[0]
            val uid = parts[1].toLong()

            val store = session.store
            val folder = store.getFolder(folderPath) as IMAPFolder
            folder.open(Folder.READ_WRITE)

            // Pobieranie wiadomości po UID
            val message = folder.getMessageByUID(uid)

            if (message == null) {
                folder.close(false)
                return MailActionResponse(
                    success = false,
                    message = "Wiadomość nie została znaleziona"
                )
            }

            // Oznaczenie wiadomości jako usuniętej
            message.setFlag(Flags.Flag.DELETED, true)

            // Ekspunge - fizyczne usunięcie oznaczonych wiadomości
            folder.expunge()

            folder.close(false)

            return MailActionResponse(
                success = true,
                message = "Wiadomość została trwale usunięta"
            )

        } catch (e: Exception) {
            return MailActionResponse(
                success = false,
                message = "Błąd usuwania wiadomości: ${e.message}"
            )
        }
    }

    // Tworzenie nowego folderu
    fun createFolder(token: String, folderName: String, color: String?): CreateFolderResponse {
        val session = getSession(token) ?: return CreateFolderResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            val store = session.store
            val folder = store.getFolder(folderName)

            if (folder.exists()) {
                return CreateFolderResponse(
                    success = false,
                    message = "Folder o podanej nazwie już istnieje"
                )
            }

            // Tworzenie folderu
            val created = folder.create(Folder.HOLDS_MESSAGES)

            if (!created) {
                return CreateFolderResponse(
                    success = false,
                    message = "Nie udało się utworzyć folderu"
                )
            }

            return CreateFolderResponse(
                success = true,
                folder = MailFolder(
                    name = folderName,
                    path = folder.fullName,
                    type = "custom",
                    messageCount = 0,
                    unreadCount = 0,
                    color = color
                ),
                message = "Folder został utworzony"
            )

        } catch (e: Exception) {
            return CreateFolderResponse(
                success = false,
                message = "Błąd tworzenia folderu: ${e.message}"
            )
        }
    }

    // Usuwanie folderu
    fun deleteFolder(token: String, folderPath: String): MailActionResponse {
        val session = getSession(token) ?: return MailActionResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            val store = session.store
            val folder = store.getFolder(folderPath)

            if (!folder.exists()) {
                return MailActionResponse(
                    success = false,
                    message = "Folder nie istnieje"
                )
            }

            if (!folder.delete(true)) {
                return MailActionResponse(
                    success = false,
                    message = "Nie udało się usunąć folderu"
                )
            }

            return MailActionResponse(
                success = true,
                message = "Folder został usunięty"
            )

        } catch (e: Exception) {
            return MailActionResponse(
                success = false,
                message = "Błąd usuwania folderu: ${e.message}"
            )
        }
    }

    // Wylogowywanie (zamykanie sesji)
    fun logout(token: String): MailActionResponse {
        val session = activeSessions[token] ?: return MailActionResponse(
            success = false,
            message = "Nieprawidłowy token sesji"
        )

        try {
            // Zamykanie połączenia IMAP
            session.store.close()

            // Usuwanie sesji
            activeSessions.remove(token)

            return MailActionResponse(
                success = true,
                message = "Wylogowano pomyślnie"
            )

        } catch (e: Exception) {
            return MailActionResponse(
                success = false,
                message = "Błąd wylogowywania: ${e.message}"
            )
        }
    }

    // Planowane zadanie czyszczenia nieużywanych sesji (co 30 minut)
    @Scheduled(fixedRate = 30 * 60 * 1000)
    fun cleanupExpiredSessions() {
        val expirationTime = Instant.now().minus(2, ChronoUnit.HOURS)

        val expiredSessionTokens = activeSessions.entries
            .filter { it.value.createdAt.isBefore(expirationTime) }
            .map { it.key }

        for (token in expiredSessionTokens) {
            try {
                logout(token)
            } catch (e: Exception) {
                // Ignorujemy błędy podczas czyszczenia sesji
            }
        }
    }

    // Metody pomocnicze

    // Pobieranie aktywnej sesji
    private fun getSession(token: String): MailSession? {
        return activeSessions[token]
    }

    // Pobieranie wszystkich folderów rekurencyjnie
    private fun getAllFolders(rootFolder: Folder, foldersList: MutableList<MailFolder>) {
        val subFolders = rootFolder.list()

        for (folder in subFolders) {
            if ((folder.type and Folder.HOLDS_MESSAGES) != 0) {
                folder.open(Folder.READ_ONLY)

                // Mapowanie typu folderu
                val folderType = mapFolderType(folder.name.lowercase())

                foldersList.add(
                    MailFolder(
                        name = folder.name,
                        path = folder.fullName,
                        type = folderType,
                        messageCount = folder.messageCount,
                        unreadCount = folder.unreadMessageCount
                    )
                )

                folder.close(false)
            }

            // Rekurencyjne przetwarzanie podfolderów
            if ((folder.type and Folder.HOLDS_FOLDERS) != 0) {
                getAllFolders(folder, foldersList)
            }
        }
    }

    // Pobieranie podsumowania folderów rekurencyjnie
    private fun getFoldersSummaryRecursive(rootFolder: Folder, summaries: MutableList<MailFolderSummary>) {
        val subFolders = rootFolder.list()

        for (folder in subFolders) {
            if ((folder.type and Folder.HOLDS_MESSAGES) != 0) {
                folder.open(Folder.READ_ONLY)

                // Mapowanie typu folderu
                val folderType = mapFolderType(folder.name.lowercase())

                summaries.add(
                    MailFolderSummary(
                        labelId = folder.fullName,
                        name = folder.name,
                        type = folderType,
                        path = folder.fullName,
                        totalCount = folder.messageCount,
                        unreadCount = folder.unreadMessageCount
                    )
                )

                folder.close(false)
            }

            // Rekurencyjne przetwarzanie podfolderów
            if ((folder.type and Folder.HOLDS_FOLDERS) != 0) {
                getFoldersSummaryRecursive(folder, summaries)
            }
        }
    }

    // Konwersja wiadomości do obiektu Email
    private fun convertMessageToEmail(
        message: Message,
        folderPath: String,
        fetchBody: Boolean = false,
        fetchAttachments: Boolean = false
    ): Email {
        // Pobieranie podstawowych informacji
        val messageId = "${folderPath}:${(message.folder as IMAPFolder).getUID(message)}"
        val subject = message.subject ?: "(brak tematu)"
        val sentDate = message.sentDate ?: Date()
        val internalDate = sentDate.time

        // Flagi
        val isRead = message.isSet(Flags.Flag.SEEN)
        val isStarred = message.isSet(Flags.Flag.FLAGGED)
        val isImportant = false // IMAP nie ma standardowej flagi dla "ważne"

        // Adresy
        val from = parseEmailAddress(message.from?.firstOrNull())
        val to = parseEmailAddresses(message.getRecipients(Message.RecipientType.TO))
        val cc = parseEmailAddresses(message.getRecipients(Message.RecipientType.CC))
        val bcc = parseEmailAddresses(message.getRecipients(Message.RecipientType.BCC))

        // Treść i załączniki
        val (body, attachments) = if (fetchBody) {
            extractBodyAndAttachments(message, fetchAttachments)
        } else {
            Pair(
                EmailBody(plain = getMessageSnippet(message), html = null),
                emptyList()
            )
        }

        // Lista folderów
        val labelIds = listOf(folderPath)

        // Tworzenie obiektu Email
        return Email(
            id = messageId,
            threadId = messageId, // W przypadku IMAP nie ma koncepcji wątków
            labelIds = labelIds,
            snippet = body.plain?.take(100) ?: "",
            internalDate = internalDate,
            subject = subject,
            from = from,
            to = to,
            cc = cc,
            bcc = bcc,
            body = body,
            attachments = if (attachments.isNotEmpty()) attachments else null,
            isRead = isRead,
            isStarred = isStarred,
            isImportant = isImportant,
            providerId = "imap"
        )
    }

    // Ekstrakcja treści i załączników z wiadomości
    private fun extractBodyAndAttachments(
        message: Message,
        fetchAttachments: Boolean
    ): Pair<EmailBody, List<EmailAttachment>> {
        var textPlain: String? = null
        var textHtml: String? = null
        val attachments = mutableListOf<EmailAttachment>()

        try {
            // Obsługa treści wieloczęściowej
            if (message.contentType?.startsWith("multipart/") == true) {
                val multipart = message.content as? Multipart

                if (multipart != null) {
                    for (i in 0 until multipart.count) {
                        val part = multipart.getBodyPart(i)
                        processMessagePart(part, attachments, fetchAttachments)?.let { (plain, html) ->
                            if (plain != null) textPlain = plain
                            if (html != null) textHtml = html
                        }
                    }
                }
            } else {
                // Treść jednorodna
                when (message.contentType?.lowercase()) {
                    null, "", "text/plain" -> textPlain = message.content.toString()
                    "text/html" -> textHtml = message.content.toString()
                    else -> {
                        if (fetchAttachments && Part.ATTACHMENT.equals(message.disposition, ignoreCase = true)) {
                            attachments.add(
                                EmailAttachment(
                                    id = "${message.folder.fullName}:${(message.folder as IMAPFolder).getUID(message)}:0",
                                    filename = message.fileName ?: "attachment",
                                    mimeType = message.contentType ?: "application/octet-stream",
                                    size = message.size
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // W przypadku błędu próbujemy pobrać chociaż fragment treści
            textPlain = getMessageSnippet(message)
        }

        return Pair(
            EmailBody(
                plain = textPlain ?: "",
                html = textHtml
            ),
            attachments
        )
    }

    // Przetwarzanie części wiadomości
    private fun processMessagePart(
        part: Part,
        attachments: MutableList<EmailAttachment>,
        fetchAttachments: Boolean
    ): Pair<String?, String?>? {
        var textPlain: String? = null
        var textHtml: String? = null

        val contentType = part.contentType?.lowercase() ?: ""

        when {
            contentType.startsWith("multipart/") -> {
                val multipart = part.content as? Multipart
                if (multipart != null) {
                    for (i in 0 until multipart.count) {
                        val subPart = multipart.getBodyPart(i)
                        processMessagePart(subPart, attachments, fetchAttachments)?.let { (plain, html) ->
                            if (plain != null) textPlain = plain
                            if (html != null) textHtml = html
                        }
                    }
                }
            }
            contentType.startsWith("text/plain") -> {
                textPlain = part.content.toString()
            }
            contentType.startsWith("text/html") -> {
                textHtml = part.content.toString()
            }
            else -> {
                if (fetchAttachments && Part.ATTACHMENT.equals(part.disposition, ignoreCase = true)) {
                    val bodyPart = part as? BodyPart
                    if (bodyPart != null) {
                        // W rzeczywistej implementacji należy wygenerować unikalny ID dla załącznika
                        attachments.add(
                            EmailAttachment(
                                id = "attachment_${attachments.size}",
                                filename = part.fileName ?: "attachment",
                                mimeType = part.contentType ?: "application/octet-stream",
                                size = part.size
                            )
                        )
                    }
                }
            }
        }

        return if (textPlain != null || textHtml != null) {
            Pair(textPlain, textHtml)
        } else {
            null
        }
    }

    // Pobieranie załącznika z wiadomości
    private fun getAttachmentFromMessage(message: Message, attachmentId: String): EmailAttachment? {
        // Implementacja pobierania załącznika na podstawie ID
        // W rzeczywistej implementacji trzeba bardziej szczegółowo obsłużyć wyszukiwanie
        // załącznika i jego danych binarnych
        return null
    }

    // Pobieranie fragmentu treści wiadomości
    private fun getMessageSnippet(message: Message): String {
        try {
            // Próba pobrania treści
            return when (val content = message.content) {
                is String -> content.take(100)
                is Multipart -> {
                    // Szukanie części tekstowej
                    for (i in 0 until content.count) {
                        val part = content.getBodyPart(i)
                        if (part.contentType?.startsWith("text/plain") == true) {
                            return part.content.toString().take(100)
                        }
                    }
                    "(brak czytelnej treści)"
                }
                else -> "(brak czytelnej treści)"
            }
        } catch (e: Exception) {
            return "(błąd odczytu treści)"
        }
    }

    // Znajdowanie wiadomości w folderze po temacie i dacie
    private fun findMessageBySubjectAndDate(folder: Folder, subject: String?, date: Date?): Message? {
        if (subject == null || date == null) return null

        val messages = folder.messages

        return messages.firstOrNull { message ->
            message.subject == subject && message.sentDate?.time == date.time
        }
    }

    // Znajdowanie wiadomości w folderze po temacie i dacie
    private fun findMessagesBySubjectAndDate(folder: Folder, subject: String?, date: Date?): List<Message> {
        if (subject == null || date == null) return emptyList()

        val messages = folder.messages

        return messages.filter { message ->
            message.subject == subject && message.sentDate?.time == date.time
        }
    }

    // Znajdowanie folderu wersji roboczych
    private fun findDraftsFolder(store: Store): Folder? {
        // Próba znalezienia standardowego folderu wersji roboczych
        val commonDraftsFolderNames = listOf("Drafts", "Draft", "Wersje robocze", "Kopie robocze")

        for (name in commonDraftsFolderNames) {
            val folder = store.getFolder(name)
            if (folder.exists()) {
                return folder
            }
        }

        // Jeśli nie znaleziono, szukamy w całej strukturze folderów
        return findFolderByType(store.defaultFolder, "drafts")
    }

    // Rekurencyjne wyszukiwanie folderu po typie
    private fun findFolderByType(rootFolder: Folder, type: String): Folder? {
        val subFolders = rootFolder.list()

        for (folder in subFolders) {
            // Sprawdzenie nazwy folderu
            if (mapFolderType(folder.name.lowercase()) == type) {
                return folder
            }

            // Rekurencyjne wyszukiwanie w podfolderach
            if ((folder.type and Folder.HOLDS_FOLDERS) != 0) {
                val result = findFolderByType(folder, type)
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    // Mapowanie nazwy folderu na typ
    private fun mapFolderType(folderName: String): String {
        return folderTypeMapping[folderName.lowercase()] ?: "custom"
    }

    // Parsowanie adresów email
    private fun parseEmailAddress(address: Address?): EmailAddress {
        if (address == null) {
            return EmailAddress(email = "", name = null)
        }

        return when (address) {
            is InternetAddress -> EmailAddress(
                email = address.address,
                name = address.personal
            )
            else -> EmailAddress(
                email = address.toString(),
                name = null
            )
        }
    }

    // Parsowanie listy adresów email
    private fun parseEmailAddresses(addresses: Array<Address>?): List<EmailAddress> {
        if (addresses == null) {
            return emptyList()
        }

        return addresses.map { parseEmailAddress(it) }
    }

    // Pobieranie treści wiadomości jako tekst
    private fun getMessageContent(message: Message): String {
        try {
            return when (val content = message.content) {
                is String -> content
                is Multipart -> {
                    val sb = StringBuilder()
                    for (i in 0 until content.count) {
                        val part = content.getBodyPart(i)
                        if (part.contentType?.startsWith("text/") == true) {
                            sb.append(part.content.toString())
                        }
                    }
                    sb.toString()
                }
                else -> ""
            }
        } catch (e: Exception) {
            return ""
        }
    }
}