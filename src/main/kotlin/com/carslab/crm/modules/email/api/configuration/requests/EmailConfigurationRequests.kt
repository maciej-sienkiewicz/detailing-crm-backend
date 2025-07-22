package com.carslab.crm.modules.email.api.configuration.requests

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*

data class SaveEmailConfigurationRequest(
    @field:NotBlank(message = "Sender email is required")
    @field:Email(message = "Sender email must be valid")
    @JsonProperty("sender_email")
    val senderEmail: String,

    @field:NotBlank(message = "Sender name is required")
    @field:Size(max = 255, message = "Sender name cannot exceed 255 characters")
    @JsonProperty("sender_name")
    val senderName: String,

    @field:NotBlank(message = "Email password is required")
    @JsonProperty("email_password")
    val emailPassword: String,

    @field:NotBlank(message = "SMTP host is required")
    @JsonProperty("smtp_host")
    val smtpHost: String,

    @field:Min(value = 1, message = "SMTP port must be between 1 and 65535")
    @field:Max(value = 65535, message = "SMTP port must be between 1 and 65535")
    @JsonProperty("smtp_port")
    val smtpPort: Int,

    @JsonProperty("use_ssl")
    val useSSL: Boolean,

    @JsonProperty("is_enabled")
    val isEnabled: Boolean = true,

    @JsonProperty("send_test_email")
    val sendTestEmail: Boolean = false
)