package com.carslab.crm.production.modules.invoice_templates.infrastructure.service

import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.production.modules.invoice_templates.domain.service.PdfGenerationService
import com.microsoft.playwright.*
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.Margin
import com.microsoft.playwright.options.WaitUntilState
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PlaywrightPdfServiceImpl : PdfGenerationService {
    private val logger = LoggerFactory.getLogger(PlaywrightPdfServiceImpl::class.java)

    private lateinit var playwright: Playwright
    private lateinit var browser: Browser

    @PostConstruct
    fun initPlaywright() {
        try {
            logger.info("Initializing Playwright for PDF generation")

            playwright = Playwright.create()
            browser = playwright.chromium().launch(
                BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(listOf(
                        "--no-sandbox",
                        "--disable-setuid-sandbox",
                        "--disable-web-security",
                        "--disable-dev-shm-usage"
                    ))
            )

            logger.info("Playwright initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize Playwright", e)
            throw RuntimeException("Playwright initialization failed", e)
        }
    }

    @PreDestroy
    fun cleanup() {
        try {
            logger.info("Shutting down Playwright")
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

    override fun generatePreview(template: InvoiceTemplate): ByteArray {
        logger.debug("Generating preview for template: {}", template.id.value)

        val mockHtml = generateMockHtml(template)
        return generatePdfFromHtml(mockHtml)
    }

    override fun generateInvoice(template: InvoiceTemplate, documentId: String, companyId: Long): ByteArray {
        logger.debug("Generating invoice for document: {} using template: {}", documentId, template.id.value)

        val html = renderTemplateWithData(template, documentId, companyId)
        return generatePdfFromHtml(html)
    }

    private fun generatePdfFromHtml(html: String): ByteArray {
        val startTime = System.currentTimeMillis()

        return try {
            if (!::browser.isInitialized || !browser.isConnected) {
                logger.warn("Browser not available, reinitializing")
                initPlaywright()
            }

            val context = browser.newContext()
            val page = context.newPage()

            try {
                page.setViewportSize(1200, 800)
                page.setContent(html, Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
                    .setTimeout(30000.0)
                )

                page.waitForLoadState(LoadState.NETWORKIDLE)
                page.waitForTimeout(1000.0)

                val pdfOptions = Page.PdfOptions()
                    .setFormat("A4")
                    .setMargin(Margin()
                        .setTop("15mm")
                        .setRight("15mm")
                        .setBottom("15mm")
                        .setLeft("15mm")
                    )
                    .setPrintBackground(true)
                    .setPreferCSSPageSize(false)

                val pdfBytes = page.pdf(pdfOptions)
                val duration = System.currentTimeMillis() - startTime

                logger.debug("PDF generated in {}ms, size: {} bytes", duration, pdfBytes.size)
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
            logger.error("Failed to generate PDF after {}ms", duration, e)
            throw RuntimeException("PDF generation failed", e)
        }
    }

    private fun generateMockHtml(template: InvoiceTemplate): String {
        return template.htmlContent
            .replace("{{invoice.number}}", "PREVIEW/2024/001")
            .replace("{{sellerName}}", "Przykładowa Firma Sp. z o.o.")
            .replace("{{sellerAddress}}", "ul. Przykładowa 1\\n00-001 Warszawa")
            .replace("{{sellerTaxId}}", "1234567890")
            .replace("{{buyerName}}", "Klient Testowy")
            .replace("{{buyerAddress}}", "ul. Kliencka 123\\n00-002 Kraków")
            .replace("{{buyerTaxId}}", "0987654321")
            .replace("{{issuedDate}}", "15.08.2024")
            .replace("{{dueDate}}", "29.08.2024")
            .replace("{{paymentMethod}}", "Przelew bankowy")
            .replace("{{totalNetFormatted}}", "1000.00 zł")
            .replace("{{totalGrossFormatted}}", "1230.00 zł")
            .replace("{{generatedAt}}", "15.08.2024 10:30")
            .replace("{{logoHtml}}", "<div style=\"width: 100px; height: 50px; background: #f0f0f0; display: flex; align-items: center; justify-content: center;\">LOGO</div>")
            .replace("{{itemsHtml}}", """
                <tr>
                    <td>1</td>
                    <td>Usługa detailingowa Premium</td>
                    <td>1</td>
                    <td class="numeric">800.00 zł</td>
                    <td>23%</td>
                    <td class="numeric">800.00 zł</td>
                    <td class="numeric">984.00 zł</td>
                </tr>
                <tr>
                    <td>2</td>
                    <td>Wosk ochronny</td>
                    <td>1</td>
                    <td class="numeric">200.00 zł</td>
                    <td>23%</td>
                    <td class="numeric">200.00 zł</td>
                    <td class="numeric">246.00 zł</td>
                </tr>
            """.trimIndent())
    }

    private fun renderTemplateWithData(template: InvoiceTemplate, documentId: String, companyId: Long): String {
        logger.debug("Rendering template with real data - placeholder implementation")
        return generateMockHtml(template)
    }
}