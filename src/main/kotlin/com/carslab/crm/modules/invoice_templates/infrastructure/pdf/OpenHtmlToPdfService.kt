package com.carslab.crm.modules.invoice_templates.infrastructure.pdf

import com.carslab.crm.modules.invoice_templates.domain.model.LayoutSettings
import com.carslab.crm.modules.invoice_templates.domain.model.PageSize
import com.carslab.crm.modules.invoice_templates.domain.ports.PdfGenerationService
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.util.XRLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File

@Service
class OpenHtmlToPdfService : PdfGenerationService {
    private val logger = LoggerFactory.getLogger(OpenHtmlToPdfService::class.java)

    init {
        XRLog.setLoggingEnabled(false)
    }

    override fun generatePdf(html: String, layoutSettings: LayoutSettings): ByteArray {
        try {
            val outputStream = ByteArrayOutputStream()

            val builder = PdfRendererBuilder()
                .useFastMode()
                .withHtmlContent(html, null)
                .toStream(outputStream)
                .useDefaultPageSize(
                    getPageWidth(layoutSettings.pageSize),
                    getPageHeight(layoutSettings.pageSize),
                    BaseRendererBuilder.PageSizeUnits.MM
                )

            // Dodaj fonty jeśli dostępne
            try {
                // Sprawdź czy pliki fontów istnieją w resources
                val dejaVuSansFont = this::class.java.getResource("/fonts/DejaVuSans.ttf")
                val dejaVuSansBoldFont = this::class.java.getResource("/fonts/DejaVuSans-Bold.ttf")

                if (dejaVuSansFont != null) {
                    builder.useFont(File(dejaVuSansFont.toURI()), layoutSettings.fontFamily)
                }

                if (dejaVuSansBoldFont != null) {
                    builder.useFont(File(dejaVuSansBoldFont.toURI()), "${layoutSettings.fontFamily}-Bold")
                }
            } catch (e: Exception) {
                logger.warn("Failed to load custom fonts, using default", e)
            }

            builder.run()

            val result = outputStream.toByteArray()
            logger.debug("PDF generated successfully, size: {} bytes", result.size)

            return result

        } catch (e: Exception) {
            logger.error("Error generating PDF from HTML", e)
            throw RuntimeException("Failed to generate PDF from template", e)
        }
    }

    override fun validateHtmlForPdf(html: String): Boolean {
        return try {
            html.contains("<html") && html.contains("</html>") &&
                    html.contains("<body") && html.contains("</body>")
        } catch (e: Exception) {
            logger.warn("HTML validation failed", e)
            false
        }
    }

    private fun getPageWidth(pageSize: PageSize): Float {
        return when (pageSize) {
            PageSize.A4 -> 210f
            PageSize.A5 -> 148f
            PageSize.LETTER -> 216f
        }
    }

    private fun getPageHeight(pageSize: PageSize): Float {
        return when (pageSize) {
            PageSize.A4 -> 297f
            PageSize.A5 -> 210f
            PageSize.LETTER -> 279f
        }
    }
}