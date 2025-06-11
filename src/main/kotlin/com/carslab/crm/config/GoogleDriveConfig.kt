// src/main/kotlin/com/carslab/crm/infrastructure/backup/config/GoogleDriveConfig.kt
package com.carslab.crm.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.encrypt.TextEncryptor

@Configuration
class GoogleDriveConfig {

    @Bean
    fun textEncryptor(
        @Value("\${google.drive.encryption.password}") password: String,
        @Value("\${google.drive.encryption.salt}") salt: String
    ): TextEncryptor {
        return Encryptors.text(password, salt)
    }
}