package com.carslab.crm.modules.email.domain.services

import com.carslab.crm.modules.email.api.configuration.responses.EmailSuggestionsResponse
import org.springframework.stereotype.Service

@Service
class EmailProviderService {

    private data class EmailProvider(
        val domains: List<String>,
        val smtpHost: String,
        val smtpPort: Int,
        val useSSL: Boolean,
        val helpText: String
    )

    private val providers = listOf(
        EmailProvider(
            domains = listOf("gmail.com"),
            smtpHost = "smtp.gmail.com",
            smtpPort = 587,
            useSSL = true,
            helpText = "Use Google App Password"
        ),
        EmailProvider(
            domains = listOf("outlook.com", "hotmail.com", "live.com"),
            smtpHost = "smtp-mail.outlook.com",
            smtpPort = 587,
            useSSL = true,
            helpText = "Standard password or App Password"
        ),
        EmailProvider(
            domains = listOf("yahoo.com"),
            smtpHost = "smtp.mail.yahoo.com",
            smtpPort = 587,
            useSSL = true,
            helpText = "Requires App Password"
        ),
        EmailProvider(
            domains = listOf("o2.pl"),
            smtpHost = "poczta.o2.pl",
            smtpPort = 465,
            useSSL = true,
            helpText = "Standard password"
        ),
        EmailProvider(
            domains = listOf("wp.pl"),
            smtpHost = "smtp.wp.pl",
            smtpPort = 465,
            useSSL = true,
            helpText = "Standard password"
        ),
        EmailProvider(
            domains = listOf("interia.pl"),
            smtpHost = "poczta.interia.pl",
            smtpPort = 465,
            useSSL = true,
            helpText = "Standard password"
        )
    )

    fun getSuggestions(email: String): EmailSuggestionsResponse {
        val domain = extractDomain(email)
        val provider = findProviderByDomain(domain)

        return if (provider != null) {
            EmailSuggestionsResponse(
                email = email,
                hasSuggestion = true,
                suggestedSmtpHost = provider.smtpHost,
                suggestedSmtpPort = provider.smtpPort,
                suggestedUseSSL = provider.useSSL,
                helpText = provider.helpText
            )
        } else {
            handleCustomDomain(email, domain)
        }
    }

    fun getProviderHint(email: String): String? {
        val domain = extractDomain(email)
        return findProviderByDomain(domain)?.helpText
    }

    private fun extractDomain(email: String): String {
        return email.substringAfterLast('@').lowercase()
    }

    private fun findProviderByDomain(domain: String): EmailProvider? {
        return providers.find { provider ->
            provider.domains.any { it.equals(domain, ignoreCase = true) }
        }
    }

    private fun handleCustomDomain(email: String, domain: String): EmailSuggestionsResponse {
        val isHomePlSubdomain = domain.contains(".home.pl")

        return if (isHomePlSubdomain) {
            val subdomain = domain.substringBefore(".home.pl")
            EmailSuggestionsResponse(
                email = email,
                hasSuggestion = true,
                suggestedSmtpHost = "mail.$subdomain.home.pl",
                suggestedSmtpPort = 587,
                suggestedUseSSL = true,
                helpText = "Hosting panel password"
            )
        } else {
            EmailSuggestionsResponse(
                email = email,
                hasSuggestion = false,
                suggestedSmtpHost = "",
                suggestedSmtpPort = 587,
                suggestedUseSSL = true,
                helpText = "Check SMTP settings in your hosting panel"
            )
        }
    }
}