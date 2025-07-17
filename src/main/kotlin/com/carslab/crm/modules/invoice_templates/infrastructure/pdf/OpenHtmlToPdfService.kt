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
            // NAPRAWKA: Sczytuj i wyloguj HTML przed przetwarzaniem
            logger.debug("Input HTML length: ${html.length}")
            logger.debug("HTML preview (first 1000 chars): ${html.take(1000)}")

            // NAPRAWKA: Oczyść HTML z problematycznych znaków
            val cleanedHtml = cleanHtmlForXmlParser(html)
            logger.debug("Cleaned HTML length: ${cleanedHtml.length}")

            val outputStream = ByteArrayOutputStream()

            val builder = PdfRendererBuilder()
                .useFastMode()
                .withHtmlContent(cleanedHtml, null)
                .toStream(outputStream)
                .useDefaultPageSize(
                    getPageWidth(layoutSettings.pageSize),
                    getPageHeight(layoutSettings.pageSize),
                    BaseRendererBuilder.PageSizeUnits.MM
                )

            // Dodaj fonty jeśli dostępne
            try {
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
            logger.error("Error generating PDF from HTML. HTML content: ${html.take(2000)}", e)
            throw RuntimeException("Failed to generate PDF from template: ${e.message}", e)
        }
    }

    // NAPRAWKA: Funkcja czyszcząca HTML dla XML parsera
    private fun cleanHtmlForXmlParser(html: String): String {
        var cleaned = html

        // Usuń problematyczne znaki Unicode
        cleaned = cleaned.replace(Regex("[\u0000-\u0008\u000B\u000C\u000E-\u001F]"), "")

        // Napraw CSS gradient syntax dla XML
        cleaned = cleaned.replace("linear-gradient(135deg, #667eea 0%, #764ba2 100%)", "#667eea")
        cleaned = cleaned.replace("linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)", "#f8f9fa")
        cleaned = cleaned.replace("linear-gradient(135deg, #34495e 0%, #2c3e50 100%)", "#34495e")
        cleaned = cleaned.replace("linear-gradient(135deg, #2c3e50 0%, #34495e 100%)", "#2c3e50")

        // Usuń problematyczne CSS properties
        cleaned = cleaned.replace("rgba(0,0,0,0.1)", "rgb(230,230,230)")
        cleaned = cleaned.replace("rgba(0,0,0,0.05)", "rgb(240,240,240)")
        cleaned = cleaned.replace("rgba(255,255,255,0.1)", "rgb(255,255,255)")

        // Napraw CSS box-shadow (może powodować problemy)
        cleaned = cleaned.replace(Regex("box-shadow:[^;]+;"), "")

        // Upewnij się że wszystkie tagi są poprawnie zamknięte
        cleaned = cleaned.replace("<br>", "<br/>")
        cleaned = cleaned.replace("<hr>", "<hr/>")
        cleaned = cleaned.replace("<img([^>]*[^/])>".toRegex()) { "<img${it.groupValues[1]}/>" }

        return cleaned
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