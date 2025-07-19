package com.carslab.crm.modules.invoice_templates.infrastructure.pdf

import com.carslab.crm.modules.invoice_templates.domain.model.LayoutSettings
import com.carslab.crm.modules.invoice_templates.domain.model.PageSize
import com.carslab.crm.modules.invoice_templates.domain.ports.PdfGenerationService
import com.microsoft.playwright.*
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.Margin
import com.microsoft.playwright.options.WaitUntilState
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PlaywrightPdfService : PdfGenerationService {
    private val logger = LoggerFactory.getLogger(PlaywrightPdfService::class.java)

    private lateinit var playwright: Playwright
    private lateinit var browser: Browser

    @PostConstruct
    fun initPlaywright() {
        try {
            logger.info("Initializing Playwright for PDF generation...")

            playwright = Playwright.create()

            browser = playwright.chromium().launch(
                BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(listOf(
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
                        "--memory-pressure-off"
                    ))
            )

            logger.info("Playwright initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize Playwright", e)
            throw RuntimeException("Playwright initialization failed: ${e.message}", e)
        }
    }

    @PreDestroy
    fun cleanup() {
        try {
            logger.info("Shutting down Playwright...")
            if (::browser.isInitialized && browser.isConnected) {
                browser.close()
            }
            if (::playwright.isInitialized) {
                playwright.close()
            }
            logger.info("Playwright shut down successfully")
        } catch (e: Exception) {
            logger.error("Error during Playwright cleanup", e)
        }
    }

    override fun generatePdf(html: String, layoutSettings: LayoutSettings): ByteArray {
        val startTime = System.currentTimeMillis()

        return try {
            logger.debug("Generating PDF with Playwright, HTML length: ${html.length}")

            // Sprawdź czy browser jest dostępny
            if (!::browser.isInitialized || !browser.isConnected) {
                logger.warn("Browser not available, reinitializing...")
                initPlaywright()
            }

            val context = browser.newContext()
            val page = context.newPage()

            try {
                // Ustaw viewport dla konsystentnego renderowania
                page.setViewportSize(1200, 800)

                // NAPRAWKA: Bezpośrednio ustaw content bez żadnych modyfikacji HTML
                page.setContent(html, Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
                    .setTimeout(30000.0)
                )

                // Poczekaj na pełne załadowanie
                page.waitForLoadState(LoadState.NETWORKIDLE)
                page.waitForTimeout(1500.0) // Dodatkowe czekanie na fonty i style

                // Konfiguracja PDF zgodna z layoutSettings
                val pdfOptions = Page.PdfOptions()
                    .setFormat(convertPageSize(layoutSettings.pageSize))
                    .setMargin(Margin()
                        .setTop("${layoutSettings.margins.top}mm")
                        .setRight("${layoutSettings.margins.right}mm")
                        .setBottom("${layoutSettings.margins.bottom}mm")
                        .setLeft("${layoutSettings.margins.left}mm")
                    )
                    .setPrintBackground(true)
                    .setPreferCSSPageSize(false)
                    .setDisplayHeaderFooter(false)

                val pdfBytes = page.pdf(pdfOptions)

                val duration = System.currentTimeMillis() - startTime
                logger.debug("PDF generated successfully with Playwright in ${duration}ms, size: ${pdfBytes.size} bytes")

                return pdfBytes

            } finally {
                try {
                    page.close()
                    context.close()
                } catch (e: Exception) {
                    logger.warn("Error closing page/context", e)
                }
            }

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Failed to generate PDF with Playwright after ${duration}ms", e)
            throw RuntimeException("PDF generation failed: ${e.message}", e)
        }
    }

    override fun validateHtmlForPdf(html: String): Boolean {
        return try {
            // Podstawowa walidacja HTML - znacznie mniej restrykcyjna niż OpenHTML
            html.isNotBlank() &&
                    (html.contains("<html", ignoreCase = true) || html.contains("<div", ignoreCase = true)) &&
                    html.length < 10_000_000 // Max 10MB HTML
        } catch (e: Exception) {
            logger.warn("HTML validation failed", e)
            false
        }
    }

    private fun convertPageSize(pageSize: PageSize): String {
        return when (pageSize) {
            PageSize.A4 -> "A4"
            PageSize.A5 -> "A5"
            PageSize.LETTER -> "Letter"
        }
    }
}