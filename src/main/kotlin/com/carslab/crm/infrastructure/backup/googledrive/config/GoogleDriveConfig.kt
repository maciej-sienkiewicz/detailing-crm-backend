// src/main/kotlin/com/carslab/crm/infrastructure/backup/googledrive/config/GoogleDriveConfig.kt
package com.carslab.crm.infrastructure.backup.googledrive.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "google.drive")
data class GoogleDriveConfig(
    var oauth: OAuthConfig = OAuthConfig(),
    var system: SystemAccount = SystemAccount(),
    var backup: BackupSettings = BackupSettings()
) {
    data class OAuthConfig(
        var clientId: String = "",
        var clientSecret: String = "",
        var refreshToken: String = ""
    )

    data class SystemAccount(
        var email: String = "sienkiewicz.maciej971030@gmail.com"
    )

    data class BackupSettings(
        var enabled: Boolean = true,
        var maxRetries: Int = 3,
        var timeoutSeconds: Long = 60,
        var batchSize: Int = 10
    )
}