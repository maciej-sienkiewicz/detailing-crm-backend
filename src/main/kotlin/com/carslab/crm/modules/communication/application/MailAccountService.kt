package com.carslab.crm.modules.communication.application

import com.carslab.crm.modules.communication.domain.model.MailAccountStatus
import com.carslab.crm.modules.communication.domain.model.MailProviderType
import com.carslab.crm.modules.communication.infrastructure.mail.ImapSyncService
import com.carslab.crm.modules.communication.infrastructure.mail.MailAutodiscoverService
import com.carslab.crm.modules.communication.infrastructure.mail.MailProviderDetection
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.MailAccountEntity
import com.carslab.crm.modules.communication.infrastructure.persistence.repository.MailAccountJpaRepository
import com.carslab.crm.modules.company_settings.domain.port.EncryptionService
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.Session
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.Properties

class MailAccountConnectionException(message: String) : RuntimeException(message)

@Service
class MailAccountService(
    private val accountRepository: MailAccountJpaRepository,
    private val autodiscoverService: MailAutodiscoverService,
    private val imapSyncService: ImapSyncService,
    private val encryptionService: EncryptionService
) {
    private val logger = LoggerFactory.getLogger(MailAccountService::class.java)

    fun detect(email: String): MailProviderDetection = autodiscoverService.detect(email)

    fun connect(
        companyId: Long,
        email: String,
        password: String,
        imapHost: String?,
        imapPort: Int?,
        smtpHost: String?,
        smtpPort: Int?
    ): MailAccountEntity {
        val normalizedEmail = email.trim().lowercase()
        val detection = autodiscoverService.detect(normalizedEmail)
        if (detection.providerType != MailProviderType.IMAP_SMTP && imapHost == null) {
            throw MailAccountConnectionException(
                "To konto obsługujemy przez logowanie u dostawcy (OAuth) — użyj przycisku logowania zamiast hasła"
            )
        }

        val resolvedImapHost = imapHost ?: detection.imapHost
            ?: throw MailAccountConnectionException("Nie udało się wykryć serwera poczty — użyj konfiguracji ręcznej")
        val resolvedImapPort = imapPort ?: detection.imapPort ?: 993
        val resolvedSmtpHost = smtpHost ?: detection.smtpHost ?: resolvedImapHost.replaceFirst("imap", "smtp")
        val resolvedSmtpPort = smtpPort ?: detection.smtpPort ?: 587

        testImapLogin(resolvedImapHost, resolvedImapPort, normalizedEmail, password)

        val existing = accountRepository.findByCompanyIdAndEmailAddress(companyId, normalizedEmail)
        val account = existing?.apply {
            encryptedPassword = encryptionService.encrypt(password)
            this.imapHost = resolvedImapHost
            this.imapPort = resolvedImapPort
            this.smtpHost = resolvedSmtpHost
            this.smtpPort = resolvedSmtpPort
            status = MailAccountStatus.ACTIVE
            lastError = null
        } ?: MailAccountEntity(
            companyId = companyId,
            emailAddress = normalizedEmail,
            providerType = MailProviderType.IMAP_SMTP,
            authType = detection.authType,
            encryptedPassword = encryptionService.encrypt(password),
            imapHost = resolvedImapHost,
            imapPort = resolvedImapPort,
            smtpHost = resolvedSmtpHost,
            smtpPort = resolvedSmtpPort
        )

        val saved = accountRepository.save(account)
        triggerInitialSync(saved)
        return saved
    }

    fun list(companyId: Long): List<MailAccountEntity> = accountRepository.findByCompanyId(companyId)

    fun delete(companyId: Long, accountId: Long) {
        val account = accountRepository.findByIdAndCompanyId(accountId, companyId)
            ?: throw MailAccountConnectionException("Nie znaleziono konta")
        account.status = MailAccountStatus.DISABLED
        account.encryptedPassword = null
        accountRepository.save(account)
    }

    fun requestSync(companyId: Long, accountId: Long) {
        val account = accountRepository.findByIdAndCompanyId(accountId, companyId)
            ?: throw MailAccountConnectionException("Nie znaleziono konta")
        triggerInitialSync(account)
    }

    @Async
    fun triggerInitialSync(account: MailAccountEntity) {
        try {
            imapSyncService.syncAccount(account)
        } catch (e: Exception) {
            logger.error("Initial sync failed for account ${account.id}", e)
        }
    }

    private fun testImapLogin(host: String, port: Int, email: String, password: String) {
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", host)
            put("mail.imaps.port", port.toString())
            put("mail.imaps.connectiontimeout", "10000")
            put("mail.imaps.timeout", "15000")
        }
        val session = Session.getInstance(props)
        try {
            val store = session.getStore("imaps")
            store.connect(host, email, password)
            store.close()
        } catch (e: AuthenticationFailedException) {
            val serverMessage = e.message.orEmpty().lowercase()
            if ("application-specific password" in serverMessage || "app password" in serverMessage) {
                throw MailAccountConnectionException(
                    "Twoja skrzynka wymaga tzw. hasła aplikacji — wygeneruj je w panelu dostawcy i wklej zamiast zwykłego hasła"
                )
            }
            throw MailAccountConnectionException("Serwer odrzucił e-mail lub hasło — sprawdź dane i spróbuj ponownie")
        } catch (e: Exception) {
            throw MailAccountConnectionException(
                "Nie udało się połączyć z serwerem $host — sprawdź adres lub użyj konfiguracji ręcznej"
            )
        }
    }
}
