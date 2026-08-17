package com.carslab.crm.modules.communication.infrastructure.mail

import com.carslab.crm.modules.communication.domain.model.MailAuthType
import com.carslab.crm.modules.communication.domain.model.MailProviderType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

data class MailProviderDetection(
    val providerType: MailProviderType,
    val authType: MailAuthType,
    val imapHost: String? = null,
    val imapPort: Int? = null,
    val smtpHost: String? = null,
    val smtpPort: Int? = null,
    val requiresAppPassword: Boolean = false,
    val guideUrl: String? = null
)

/**
 * Thunderbird-style provider detection: known-domain table -> Mozilla ISPDB ->
 * autoconfig.{domain} -> imap.{domain} guess. The user only ever types an
 * e-mail address; OAuth providers are recognized up front so the UI can route
 * to the consent flow instead of asking for a password.
 */
@Service
class MailAutodiscoverService {
    private val logger = LoggerFactory.getLogger(MailAutodiscoverService::class.java)

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(4))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val googleDomains = setOf("gmail.com", "googlemail.com")
    private val microsoftDomains = setOf("outlook.com", "hotmail.com", "live.com", "msn.com", "outlook.pl")

    private val knownImapProviders = mapOf(
        "wp.pl" to MailProviderDetection(
            MailProviderType.IMAP_SMTP, MailAuthType.PASSWORD,
            "imap.wp.pl", 993, "smtp.wp.pl", 465
        ),
        "o2.pl" to MailProviderDetection(
            MailProviderType.IMAP_SMTP, MailAuthType.PASSWORD,
            "poczta.o2.pl", 993, "poczta.o2.pl", 465
        ),
        "onet.pl" to MailProviderDetection(
            MailProviderType.IMAP_SMTP, MailAuthType.PASSWORD,
            "imap.poczta.onet.pl", 993, "smtp.poczta.onet.pl", 465
        ),
        "op.pl" to MailProviderDetection(
            MailProviderType.IMAP_SMTP, MailAuthType.PASSWORD,
            "imap.poczta.onet.pl", 993, "smtp.poczta.onet.pl", 465
        ),
        "interia.pl" to MailProviderDetection(
            MailProviderType.IMAP_SMTP, MailAuthType.PASSWORD,
            "poczta.interia.pl", 993, "poczta.interia.pl", 465
        ),
        "icloud.com" to MailProviderDetection(
            MailProviderType.IMAP_SMTP, MailAuthType.APP_PASSWORD,
            "imap.mail.me.com", 993, "smtp.mail.me.com", 587,
            requiresAppPassword = true,
            guideUrl = "https://support.apple.com/pl-pl/102654"
        )
    )

    fun detect(email: String): MailProviderDetection {
        val domain = email.substringAfterLast('@').lowercase().trim()

        if (domain in googleDomains) {
            return MailProviderDetection(MailProviderType.GOOGLE_API, MailAuthType.OAUTH2)
        }
        if (domain in microsoftDomains) {
            return MailProviderDetection(MailProviderType.MS_GRAPH, MailAuthType.OAUTH2)
        }
        knownImapProviders[domain]?.let { return it }

        lookupIspdb(domain)?.let { return it }
        lookupAutoconfig(domain)?.let { return it }

        // Last-resort guess; the UI keeps a manual-configuration escape hatch.
        return MailProviderDetection(
            MailProviderType.IMAP_SMTP, MailAuthType.PASSWORD,
            "imap.$domain", 993, "smtp.$domain", 587
        )
    }

    private fun lookupIspdb(domain: String): MailProviderDetection? =
        fetchAutoconfigXml("https://autoconfig.thunderbird.net/v1.1/$domain")

    private fun lookupAutoconfig(domain: String): MailProviderDetection? =
        fetchAutoconfigXml("https://autoconfig.$domain/mail/config-v1.1.xml")

    private fun fetchAutoconfigXml(url: String): MailProviderDetection? {
        return try {
            val request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) return null
            parseAutoconfig(response.body())
        } catch (e: Exception) {
            logger.debug("Autoconfig lookup failed for $url: ${e.message}")
            null
        }
    }

    /** Minimal parser for the Thunderbird autoconfig XML format. */
    internal fun parseAutoconfig(xml: String): MailProviderDetection? {
        fun server(type: String): Pair<String, Int>? {
            val block = Regex(
                "<(incoming|outgoing)Server[^>]*type=\"$type\"[^>]*>(.*?)</\\1Server>",
                RegexOption.DOT_MATCHES_ALL
            ).find(xml)?.groupValues?.get(2) ?: return null
            val host = Regex("<hostname>([^<]+)</hostname>").find(block)?.groupValues?.get(1) ?: return null
            val port = Regex("<port>(\\d+)</port>").find(block)?.groupValues?.get(1)?.toIntOrNull() ?: return null
            return host.trim() to port
        }

        val imap = server("imap") ?: return null
        val smtp = server("smtp")
        return MailProviderDetection(
            providerType = MailProviderType.IMAP_SMTP,
            authType = MailAuthType.PASSWORD,
            imapHost = imap.first,
            imapPort = imap.second,
            smtpHost = smtp?.first,
            smtpPort = smtp?.second
        )
    }
}
