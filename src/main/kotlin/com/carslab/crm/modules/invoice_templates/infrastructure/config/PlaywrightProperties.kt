package com.carslab.crm.modules.invoice_templates.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties(PlaywrightProperties::class)
class PlaywrightConfiguration

@ConfigurationProperties(prefix = "carslab.pdf.playwright")
data class PlaywrightProperties(
    val headless: Boolean = true,
    val timeout: Long = 45000,
    val viewport: ViewportConfig = ViewportConfig(),
    val browserArgs: List<String> = listOf(
        "--no-sandbox",
        "--disable-setuid-sandbox",
        "--disable-web-security",
        "--disable-extensions",
        "--disable-plugins",
        "--disable-dev-shm-usage",
        "--disable-background-networking",
        "--disable-background-timer-throttling",
        "--disable-renderer-backgrounding",
        "--disable-backgrounding-occluded-windows",
        "--memory-pressure-off",
        "--max_old_space_size=4096"
    ),
    val pdf: PdfConfig = PdfConfig(),
    val fonts: Map<String, FontConfig> = emptyMap(),
    val cache: CacheConfig = CacheConfig(),
    val pool: PoolConfig = PoolConfig()
) {
    data class ViewportConfig(
        val width: Int = 1200,
        val height: Int = 800
    )

    data class PdfConfig(
        val printBackground: Boolean = true,
        val preferCssPageSize: Boolean = false,
        val displayHeaderFooter: Boolean = false
    )

    data class FontConfig(
        val name: String = "",
        val path: String = ""
    )

    data class CacheConfig(
        val enabled: Boolean = true,
        val maxSize: Int = 100,
        val ttlMinutes: Long = 60
    )

    data class PoolConfig(
        val maxBrowsers: Int = 3,
        val browserReuse: Boolean = true
    )

    fun getTimeoutDuration(): Duration = Duration.ofMillis(timeout)
    fun getCacheTtlDuration(): Duration = Duration.ofMinutes(cache.ttlMinutes)
}