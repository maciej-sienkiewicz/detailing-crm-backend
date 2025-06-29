package com.carslab.crm.domain.visits.protocols

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.modules.visits.domain.CarReceptionService
import com.carslab.crm.modules.visits.domain.ports.CarReceptionRepository
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

data class SignatureData(
    val signatureImageBytes: ByteArray,
    val signatureFieldName: String = "signature" // nazwa pola podpisu w PDF
)

@Service
class PdfService(
    private val carReceptionService: CarReceptionService,
) {
    
    fun generatePdf(protocolId: Long, signatureData: SignatureData? = null): ByteArray {
        val protocol: CarReceptionProtocol = carReceptionService.getProtocolById(ProtocolId(protocolId.toString()))
            ?: throw IllegalStateException("Nie znaleziono protokołu o ID: $protocolId")

        val formData = getFormDataForProtokol(protocol)
        val resource = ClassPathResource("static/z.pdf")

        ByteArrayOutputStream().use { outputStream ->
            PDDocument.load(resource.inputStream).use { document ->
                val acroForm: PDAcroForm? = document.documentCatalog.acroForm

                if (acroForm == null) {
                    throw IllegalStateException("Brak formularza w podanym pliku PDF.")
                }

                // Wypełnianie standardowych pól tekstowych
                fillTextFields(acroForm, formData)

                // Wypełnianie pól checkbox/button
                fillCheckboxFields(acroForm, protocol)

                // Dodanie podpisu jeśli został przekazany
                signatureData?.let { signature ->
                    addSignatureToDocument(document, acroForm, signature)
                }

                // Spłaszczenie formularza (opcjonalne - uniemożliwia dalszą edycję)
                if (signatureData != null) {
                    document.documentCatalog.acroForm?.flatten()
                }

                document.save(outputStream)
            }

            return outputStream.toByteArray()
        }
    }

    fun sign(document: ByteArray, signatureData: ByteArray): ByteArray {

        ByteArrayOutputStream().use { outputStream ->
            PDDocument.load(document.inputStream()).use { document ->
                val acroForm: PDAcroForm? = document.documentCatalog.acroForm

                if (acroForm == null) {
                    throw IllegalStateException("Brak formularza w podanym pliku PDF.")
                }

                addSignatureToDocument(document, acroForm, signatureData)

                document.documentCatalog.acroForm?.flatten()

                document.save(outputStream)
            }

            return outputStream.toByteArray()
        }
    }

    private fun addSignatureToDocument(
        document: PDDocument,
        acroForm: PDAcroForm,
        signatureData: ByteArray
    ) {
        try {
            // Próbujemy znaleźć pole podpisu w formularzu
            val signatureField = acroForm.getField("Signature_1") as? PDSignatureField

            if (signatureField != null) {
                // Jeśli mamy dedykowane pole podpisu, używamy jego współrzędnych
                addSignatureToField(document, signatureField, signatureData)
            } else {
                // Fallback: dodajemy podpis na stałej pozycji na ostatniej stronie
                addSignatureToFixedPosition(document, signatureData)
            }
        } catch (e: Exception) {
            println("Błąd podczas dodawania podpisu: ${e.message}")
            // Fallback na stałą pozycję
            addSignatureToFixedPosition(document, signatureData)
        }
    }

    private fun fillTextFields(acroForm: PDAcroForm, formData: Map<String, String>) {
        formData.forEach { (fieldName, value) ->
            val field = acroForm.getField(fieldName) as? PDTextField
            if (field != null) {
                field.setValue(value.transliterate())
            } else {
                println("Nie znaleziono pola tekstowego: $fieldName")
            }
        }
    }

    private fun fillCheckboxFields(acroForm: PDAcroForm, protocol: CarReceptionProtocol) {

    }

    private fun addSignatureToDocument(
        document: PDDocument,
        acroForm: PDAcroForm,
        signatureData: SignatureData
    ) {
        try {
            // Próbujemy znaleźć pole podpisu w formularzu
            val signatureField = acroForm.getField("signature") as? PDSignatureField

            if (signatureField != null) {
                // Jeśli mamy dedykowane pole podpisu, używamy jego współrzędnych
                addSignatureToField(document, signatureField, signatureData.signatureImageBytes)
            } else {
                // Fallback: dodajemy podpis na stałej pozycji na ostatniej stronie
                addSignatureToFixedPosition(document, signatureData.signatureImageBytes)
            }
        } catch (e: Exception) {
            println("Błąd podczas dodawania podpisu: ${e.message}")
            // Fallback na stałą pozycję
            addSignatureToFixedPosition(document, signatureData.signatureImageBytes)
        }
    }

    private fun addSignatureToField(
        document: PDDocument,
        signatureField: PDSignatureField,
        signatureImageBytes: ByteArray
    ) {
        // Pobieramy widget (wizualną reprezentację pola)
        val widget = signatureField.widgets.firstOrNull()
            ?: throw IllegalStateException("Pole podpisu nie ma widget'a")

        val page = widget.page
        val fieldRect = widget.rectangle

        // DEBUG: 
        println("Pole podpisu wymiary: ${fieldRect.width} x ${fieldRect.height} na pozycji (${fieldRect.lowerLeftX}, ${fieldRect.lowerLeftY})")

        // Używamy dokładnie wymiarów i pozycji pola - zajmujemy cały obszar
        addImageToField(document, page, signatureImageBytes, fieldRect)
    }

    private fun addImageToField(
        document: PDDocument,
        page: PDPage,
        imageBytes: ByteArray,
        fieldRect: org.apache.pdfbox.pdmodel.common.PDRectangle
    ) {
        // Przetwarzamy obraz - usuwamy białe tło
        val processedImageBytes = removeWhiteBackground(imageBytes)

        // Konwertujemy na PDImageXObject
        val pdImage = PDImageXObject.createFromByteArray(document, processedImageBytes, "signature")

        PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true).use { contentStream ->
            // Rysujemy obraz dokładnie w obszarze pola - PDF automatycznie skaluje
            contentStream.drawImage(
                pdImage,
                fieldRect.lowerLeftX,    // X pola
                fieldRect.lowerLeftY,    // Y pola  
                fieldRect.width,         // Szerokość pola
                fieldRect.height         // Wysokość pola
            )

            println("Podpis narysowany w obszarze: X=${fieldRect.lowerLeftX}, Y=${fieldRect.lowerLeftY}, W=${fieldRect.width}, H=${fieldRect.height}")
        }
    }

    private fun addSignatureToFixedPosition(document: PDDocument, signatureImageBytes: ByteArray) {
        // Dodajemy podpis na ostatniej stronie w prawym dolnym rogu
        val lastPage = document.pages.last()
        val pageSize = lastPage.mediaBox

        // Pozycja w prawym dolnym rogu (możesz dostosować)
        val signatureRect = Rectangle2D.Float(
            pageSize.width - 200f, // 200 punktów od prawej krawędzi
            50f, // 50 punktów od dołu
            150f, // szerokość 150 punktów
            75f   // wysokość 75 punktów
        )

        addImageToPage(document, lastPage, signatureImageBytes, signatureRect)
    }

    private fun addImageToPage(
        document: PDDocument,
        page: PDPage,
        imageBytes: ByteArray,
        rect: Rectangle2D.Float
    ) {
        // Przetwarzamy obraz - usuwamy białe tło
        val processedImageBytes = removeWhiteBackground(imageBytes)

        // Konwertujemy PNG na PDImageXObject
        val pdImage = PDImageXObject.createFromByteArray(document, processedImageBytes, "signature")

        PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true).use { contentStream ->
            // Rysujemy obraz w określonym prostokącie (fallback pozycja)
            contentStream.drawImage(
                pdImage,
                rect.x,
                rect.y,
                rect.width,
                rect.height
            )

            println("Podpis narysowany na fallback pozycji: X=${rect.x}, Y=${rect.y}, W=${rect.width}, H=${rect.height}")
        }
    }

    /**
     * Usuwa białe tło z obrazu PNG i robi go przezroczystym
     */
    private fun removeWhiteBackground(imageBytes: ByteArray): ByteArray {
        try {
            val originalImage = ImageIO.read(ByteArrayInputStream(imageBytes))

            // Tworzymy nowy obraz z kanałem alpha (przezroczystość)
            val transparentImage = BufferedImage(
                originalImage.width,
                originalImage.height,
                BufferedImage.TYPE_INT_ARGB
            )

            val graphics = transparentImage.createGraphics()

            // Iterujemy przez każdy piksel
            for (x in 0 until originalImage.width) {
                for (y in 0 until originalImage.height) {
                    val rgb = originalImage.getRGB(x, y)
                    val red = (rgb shr 16) and 0xFF
                    val green = (rgb shr 8) and 0xFF
                    val blue = rgb and 0xFF

                    // Jeśli piksel jest "prawie biały" (tolerance dla różnych odcieni)
                    val isWhiteish = red > 240 && green > 240 && blue > 240

                    if (isWhiteish) {
                        // Robimy przezroczysty (alpha = 0)
                        transparentImage.setRGB(x, y, (0x00FFFFFF and rgb).toInt())
                    } else {
                        // Zostawiamy oryginalny kolor z pełną nieprzezroczystością
                        transparentImage.setRGB(x, y, (0xFF000000.toInt() or rgb))
                    }
                }
            }

            graphics.dispose()

            // Konwertujemy z powrotem na ByteArray
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(transparentImage, "PNG", outputStream)
            return outputStream.toByteArray()

        } catch (e: Exception) {
            println("Błąd podczas usuwania białego tła: ${e.message}")
            // Zwracamy oryginalny obraz jeśli przetwarzanie się nie udało
            return imageBytes
        }
    }

    // Pozostałe metody pomocnicze pozostają bez zmian
    private fun String.transliterate(): String = this
        .replace('ą', 'a')
        .replace('ć', 'c')
        .replace('ę', 'e')
        .replace('ł', 'l')
        .replace('ń', 'n')
        .replace('ó', 'o')
        .replace('ś', 's')
        .replace('ź', 'z')
        .replace('ż', 'z')
        .replace('\u00A0', ' ')

    private fun addNote(note: String?): String =
        if (!note.isNullOrBlank()) "[$note]" else ""

    fun LocalDateTime.format(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return this.format(formatter)
    }

    private fun getFormDataForProtokol(protocol: CarReceptionProtocol): Map<String, String> {
        val notes = protocol.protocolServices.joinToString(",") {
            "${it.name} ${addNote(it.note)}"
        }

        return mapOf(
            "brand" to (protocol.vehicle.make),
            "model" to (protocol.vehicle.model),
            "licenseplate" to (protocol.vehicle.licensePlate),
            "mileage" to (protocol.vehicle.mileage?.toString() ?: ""),
            "services" to notes,
            "fullname" to (protocol.client.name ?: ""),
            "companyname" to (protocol.client.companyName ?: ""),
            "Text7" to (""),
            "phonenumber" to (protocol.client.phone ?: ""),
            "email" to (protocol.client.email ?: ""),
            "tax" to (protocol.client.taxId ?: ""),
            "remarks" to (protocol.notes?: ""),
            "date" to (protocol.period.startDate.format() ?: ""),
        )
    }
}