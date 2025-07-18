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
            logger.debug("Input HTML length: ${html.length}")
            logger.debug("HTML preview (first 500 chars): ${html.take(500)}")

            // NAPRAWKA: Dokładne czyszczenie HTML dla XML parsera
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
            logger.error("Error generating PDF from HTML. Error: ${e.message}", e)
            throw RuntimeException("Failed to generate PDF from template: ${e.message}", e)
        }
    }

    // NAPRAWKA: Ulepszona funkcja czyszcząca HTML dla XML parsera
    private fun cleanHtmlForXmlParser(html: String): String {
        var cleaned = html

        // 1. NAPRAWKA GŁÓWNYCH PROBLEMÓW Z META TAGAMI I SELF-CLOSING TAGS
        cleaned = cleaned.replace("<meta charset=\"UTF-8\">", "<meta charset=\"UTF-8\"/>")
        cleaned = cleaned.replace("<meta charset='UTF-8'>", "<meta charset=\"UTF-8\"/>")
        cleaned = cleaned.replace(Regex("<meta([^>]*[^/])>")) { "<meta${it.groupValues[1]}/>" }
        cleaned = cleaned.replace(Regex("<br([^>]*[^/])?>")) { "<br/>" }
        cleaned = cleaned.replace(Regex("<hr([^>]*[^/])?>")) { "<hr/>" }
        cleaned = cleaned.replace(Regex("<img([^>]*[^/])>")) { "<img${it.groupValues[1]}/>" }
        cleaned = cleaned.replace(Regex("<input([^>]*[^/])>")) { "<input${it.groupValues[1]}/>" }
        cleaned = cleaned.replace(Regex("<area([^>]*[^/])>")) { "<area${it.groupValues[1]}/>" }
        cleaned = cleaned.replace(Regex("<base([^>]*[^/])>")) { "<base${it.groupValues[1]}/>" }
        cleaned = cleaned.replace(Regex("<link([^>]*[^/])>")) { "<link${it.groupValues[1]}/>" }

        // 2. USUŃ PROBLEMATYCZNE ZNAKI UNICODE
        cleaned = cleaned.replace(Regex("[\u0000-\u0008\u000B\u000C\u000E-\u001F]"), "")

        // 3. NAPRAW CSS GRADIENTS - ZAMIEŃ NA SOLID COLORS
        cleaned = cleaned.replace("linear-gradient(135deg, #667eea 0%, #764ba2 100%)", "#667eea")
        cleaned = cleaned.replace("linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)", "#f8f9fa")
        cleaned = cleaned.replace("linear-gradient(135deg, #34495e 0%, #2c3e50 100%)", "#34495e")
        cleaned = cleaned.replace("linear-gradient(135deg, #2c3e50 0%, #34495e 100%)", "#2c3e50")
        cleaned = cleaned.replace(Regex("linear-gradient\\([^)]+\\)"), "#f8f9fa")

        // 4. USUŃ PROBLEMATYCZNE CSS RGBA I BOX-SHADOW
        cleaned = cleaned.replace("rgba(0,0,0,0.1)", "rgb(230,230,230)")
        cleaned = cleaned.replace("rgba(0,0,0,0.05)", "rgb(240,240,240)")
        cleaned = cleaned.replace("rgba(255,255,255,0.1)", "rgb(255,255,255)")
        cleaned = cleaned.replace(Regex("rgba\\([^)]+\\)"), "rgb(200,200,200)")
        cleaned = cleaned.replace(Regex("box-shadow:[^;]+;"), "")

        // 5. NAPRAW PROBLEMATYCZNE CSS PROPERTIES
        cleaned = cleaned.replace(Regex("transform:[^;]+;"), "")
        cleaned = cleaned.replace(Regex("filter:[^;]+;"), "")
        cleaned = cleaned.replace(Regex("backdrop-filter:[^;]+;"), "")

        // 6. ESCAPE ZNAKI W ATTRIBUTACH
        cleaned = cleaned.replace("&(?![a-zA-Z0-9#]+;)".toRegex(), "&amp;")

        // 7. NAPRAW POTENCJALNE PROBLEMY Z CUDZYSŁOWAMI W CSS
        cleaned = cleaned.replace("font-family: 'DejaVuSans'", "font-family: DejaVuSans")
        cleaned = cleaned.replace("font-family: 'Courier New'", "font-family: 'Courier New'")

        // 8. USUŃ KOMENTARZE HTML KTÓRE MOGĄ POWODOWAĆ PROBLEMY
        cleaned = cleaned.replace(Regex("<!--[\\s\\S]*?-->"), "")

        // 9. NAPRAW DOCTYPE I XML NAMESPACE
        if (!cleaned.contains("<?xml")) {
            cleaned = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + cleaned
        }

        // 10. UPEWNIJ SIĘ ŻE XMLNS Jest POPRAWNE
        cleaned = cleaned.replace(
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
        )

        // 11. USUŃ PUSTE LINIE I NADMIAROWE SPACJE
        cleaned = cleaned.replace(Regex("\\s{2,}"), " ")
        cleaned = cleaned.replace(Regex("\\n\\s*\\n"), "\n")

        logger.debug("HTML cleaning completed. Original: ${html.length}, Cleaned: ${cleaned.length}")

        return cleaned
    }

    override fun validateHtmlForPdf(html: String): Boolean {
        return try {
            // Sprawdź podstawową strukturę HTML
            val hasHtmlTag = html.contains(Regex("<html[^>]*>"))
            val hasClosingHtmlTag = html.contains("</html>")
            val hasBodyTag = html.contains(Regex("<body[^>]*>"))
            val hasClosingBodyTag = html.contains("</body>")
            val hasHeadTag = html.contains(Regex("<head[^>]*>"))
            val hasClosingHeadTag = html.contains("</head>")

            // Sprawdź czy nie ma niepoprawnych self-closing tagów
            val hasSelfClosingMeta = !html.contains(Regex("<meta[^>]*[^/]>"))

            hasHtmlTag && hasClosingHtmlTag && hasBodyTag && hasClosingBodyTag &&
                    hasHeadTag && hasClosingHeadTag

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