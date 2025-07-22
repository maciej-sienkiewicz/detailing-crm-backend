package com.carslab.crm.modules.email.api.configuration.responses

import com.carslab.crm.modules.email.domain.model.EmailConfiguration
import com.carslab.crm.modules.email.domain.model.ValidationStatus
import com.carslab.crm.modules.email.api.configuration.requests.SaveEmailConfigurationRequest
import com.fasterxml.jackson.annotation.JsonProperty

data class EmailConfigurationResponse(
    @JsonProperty("sender_email")
    val senderEmail: String,

    @JsonProperty("sender_name")
    val senderName: String,

    @JsonProperty("smtp_host")
    val smtpHost: String,

    @JsonProperty("smtp_port")
    val smtpPort: Int,

    @JsonProperty("use_ssl")
    val useSSL: Boolean,

    @JsonProperty("is_enabled")
    val isEnabled: Boolean,

    @JsonProperty("validation_status")
    val validationStatus: String,

    @JsonProperty("validation_message")
    val validationMessage: String?,

    @JsonProperty("provider_hint")
    val providerHint: String?,

    @JsonProperty("test_email_sent")
    val testEmailSent: Boolean
) {
    companion object {
        fun from(emailConfig: EmailConfiguration, testEmailSent: Boolean = false): EmailConfigurationResponse {
            return EmailConfigurationResponse(
                senderEmail = emailConfig.senderEmail,
                senderName = emailConfig.senderName,
                smtpHost = emailConfig.smtpHost,
                smtpPort = emailConfig.smtpPort,
                useSSL = emailConfig.useSSL,
                isEnabled = emailConfig.isEnabled,
                validationStatus = emailConfig.validationStatus.name,
                validationMessage = emailConfig.validationMessage,
                providerHint = emailConfig.providerHint,
                testEmailSent = testEmailSent
            )
        }

        fun error(request: SaveEmailConfigurationRequest, errorMessage: String): EmailConfigurationResponse {
            return EmailConfigurationResponse(
                senderEmail = request.senderEmail,
                senderName = request.senderName,
                smtpHost = request.smtpHost,
                smtpPort = request.smtpPort,
                useSSL = request.useSSL,
                isEnabled = request.isEnabled,
                validationStatus = ValidationStatus.INVALID_SETTINGS.name,
                validationMessage = errorMessage,
                providerHint = null,
                testEmailSent = false
            )
        }
    }
}

data class EmailSuggestionsResponse(
    @JsonProperty("email")
    val email: String,

    @JsonProperty("has_suggestion")
    val hasSuggestion: Boolean,

    @JsonProperty("suggested_smtp_host")
    val suggestedSmtpHost: String,

    @JsonProperty("suggested_smtp_port")
    val suggestedSmtpPort: Int,

    @JsonProperty("suggested_use_ssl")
    val suggestedUseSSL: Boolean,

    @JsonProperty("help_text")
    val helpText: String
)