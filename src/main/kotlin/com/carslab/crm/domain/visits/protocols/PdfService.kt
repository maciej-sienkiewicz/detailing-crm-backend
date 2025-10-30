package com.carslab.crm.domain.visits.protocols

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.modules.visits.api.commands.CarReceptionDetailDto
import com.carslab.crm.production.modules.templates.application.service.query.TemplateQueryService
import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.shared.exception.TemplateNotFoundException
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.font.PDType0Font
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
    val signatureFieldName: String = "signature"
)

@Service
class PdfService(
    private val visitQueryService: VisitDetailQueryService,
    private val templateQueryService: TemplateQueryService,
    private val universalStorageService: UniversalStorageService,
    private val securityContext: SecurityContext
) {

    fun generatePdf(protocolId: Long, signatureData: SignatureData? = null): ByteArray {
        val companyId = securityContext.getCurrentCompanyId()
        val template = templateQueryService.findActiveTemplateByTemplateType(
            TemplateType.SERVICE_AGREEMENT,
            companyId
        ) ?: throw TemplateNotFoundException(
            "Nie znaleziono aktywnego szablonu dla protokołu przyjęcia pojazdu. Uzupełnij szablon w ustawieniach."
        )
        val resource = universalStorageService.retrieveFile(template.id)
            ?: throw IllegalStateException("Cannot load template file")

        val protocol = visitQueryService.getVisitDetail(protocolId.toString())
        val formData = getFormDataForProtokol(protocol)

        ByteArrayOutputStream().use { outputStream ->
            PDDocument.load(resource).use { document ->
                val acroForm: PDAcroForm? = document.documentCatalog.acroForm

                if (acroForm == null) {
                    throw IllegalStateException("Brak formularza w podanym pliku PDF.")
                }

                // KLUCZOWE: Ładujemy font PRZED wypełnianiem pól
                val polishFont = loadPolishFont(document)
                setPolishFont(document, acroForm, polishFont)

                // Wypełnianie standardowych pól tekstowych z fontem
                fillTextFields(acroForm, formData, polishFont)
                acroForm.getField("keys")?.setValue(if(protocol.keysProvided) "keys" else "Off")
                acroForm.getField("documents")?.setValue(if(protocol.documentsProvided) "documents" else "Off")

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

    /**
     * Ustawia font wspierający polskie znaki dla całego formularza
     */
    private fun setPolishFont(document: PDDocument, acroForm: PDAcroForm, font: PDType0Font) {
        try {
            // Ustawiamy font jako domyślny dla całego formularza
            val defaultResources = acroForm.defaultResources ?: PDResources()
            defaultResources.put(org.apache.pdfbox.cos.COSName.getPDFName("Helv"), font)
            acroForm.defaultResources = defaultResources

            // Ustawiamy domyślny wygląd z tym fontem
            acroForm.defaultAppearance = "/Helv 12 Tf 0 g"

            // KLUCZOWE: Wymuszamy regenerację wyglądu wszystkich pól
            acroForm.needAppearances = true

            println("✓ Font wspierający polskie znaki został ustawiony")
        } catch (e: Exception) {
            println("⚠ Nie udało się ustawić fontu dla formularza: ${e.message}")
        }
    }

    /**
     * Ładuje font wspierający polskie znaki
     * Próbuje w kolejności: resources -> systemowy -> internet cache
     */
    private fun loadPolishFont(document: PDDocument): PDType0Font {
        // Opcja 1: Font z resources
        try {
            val fontResource = ClassPathResource("fonts/DejaVuSans.ttf")
            if (fontResource.exists()) {
                println("✓ Używam fontu z resources")
                return PDType0Font.load(document, fontResource.inputStream)
            }
        } catch (e: Exception) {
            println("Nie znaleziono fontu w resources")
        }

        // Opcja 2: Font systemowy
        val systemFontPaths = listOf(
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/calibri.ttf",
            "/System/Library/Fonts/Helvetica.ttc",
            "/Library/Fonts/Arial.ttf"
        )

        for (fontPath in systemFontPaths) {
            try {
                val fontFile = java.io.File(fontPath)
                if (fontFile.exists()) {
                    println("✓ Używam fontu systemowego: $fontPath")
                    return PDType0Font.load(document, fontFile)
                }
            } catch (e: Exception) {
                // Próbujemy kolejny
            }
        }

        // Opcja 3: Pobierz font z internetu i cachuj lokalnie
        try {
            val cachedFont = downloadAndCacheFont()
            println("✓ Używam fontu pobranego z internetu (cache)")
            return PDType0Font.load(document, cachedFont)
        } catch (e: Exception) {
            println("✗ Nie udało się pobrać fontu: ${e.message}")
        }

        throw IllegalStateException(
            "Nie znaleziono żadnego fontu wspierającego polskie znaki. " +
                    "Zainstaluj fonts-dejavu w Dockerfile lub dodaj DejaVuSans.ttf do resources."
        )
    }

    /**
     * Pobiera font DejaVu Sans z internetu i cachuje go lokalnie w /tmp
     */
    private fun downloadAndCacheFont(): java.io.File {
        val cacheDir = java.io.File("/tmp/carslab-fonts")
        val cachedFontFile = java.io.File(cacheDir, "DejaVuSans.ttf")

        // Jeśli font już jest w cache, użyj go
        if (cachedFontFile.exists() && cachedFontFile.length() > 100_000) {
            println("Używam fontu z cache: ${cachedFontFile.absolutePath}")
            return cachedFontFile
        }

        // Pobierz font z internetu
        println("Pobieram font DejaVu Sans z internetu...")
        cacheDir.mkdirs()

        val fontUrl = "https://github.com/dejavu-fonts/dejavu-fonts/raw/master/ttf/DejaVuSans.ttf"

        java.net.URL(fontUrl).openStream().use { input ->
            cachedFontFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        println("✓ Font pobrany i zapisany w cache: ${cachedFontFile.absolutePath}")
        return cachedFontFile
    }

    private fun fillTextFields(acroForm: PDAcroForm, formData: Map<String, String>, font: PDType0Font) {
        formData.forEach { (fieldName, value) ->
            val field = acroForm.getField(fieldName) as? PDTextField
            if (field != null) {
                try {
                    // Ustawiamy wartość - PDFBox użyje fontu z defaultResources formularza
                    field.setValue(value)

                    println("✓ Ustawiono wartość dla pola $fieldName: $value")
                } catch (e: Exception) {
                    println("✗ Błąd podczas ustawiania wartości dla pola $fieldName: ${e.message}")
                }
            } else {
                println("⚠ Nie znaleziono pola tekstowego: $fieldName")
            }
        }
    }

    private fun addSignatureToDocument(
        document: PDDocument,
        acroForm: PDAcroForm,
        signatureData: SignatureData
    ) {
        try {
            val signatureField = acroForm.getField("signature") as? PDSignatureField

            if (signatureField != null) {
                addSignatureToField(document, signatureField, signatureData.signatureImageBytes)
            } else {
                addSignatureToFixedPosition(document, signatureData.signatureImageBytes)
            }
        } catch (e: Exception) {
            println("Błąd podczas dodawania podpisu: ${e.message}")
            addSignatureToFixedPosition(document, signatureData.signatureImageBytes)
        }
    }

    private fun addSignatureToField(
        document: PDDocument,
        signatureField: PDSignatureField,
        signatureImageBytes: ByteArray
    ) {
        val widget = signatureField.widgets.firstOrNull()
            ?: throw IllegalStateException("Pole podpisu nie ma widget'a")

        val page = widget.page
        val fieldRect = widget.rectangle

        println("Pole podpisu wymiary: ${fieldRect.width} x ${fieldRect.height} na pozycji (${fieldRect.lowerLeftX}, ${fieldRect.lowerLeftY})")

        addImageToField(document, page, signatureImageBytes, fieldRect)
    }

    private fun addImageToField(
        document: PDDocument,
        page: PDPage,
        imageBytes: ByteArray,
        fieldRect: org.apache.pdfbox.pdmodel.common.PDRectangle
    ) {
        val processedImageBytes = removeWhiteBackground(imageBytes)
        val pdImage = PDImageXObject.createFromByteArray(document, processedImageBytes, "signature")

        PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true).use { contentStream ->
            contentStream.drawImage(
                pdImage,
                fieldRect.lowerLeftX,
                fieldRect.lowerLeftY,
                fieldRect.width,
                fieldRect.height
            )

            println("Podpis narysowany w obszarze: X=${fieldRect.lowerLeftX}, Y=${fieldRect.lowerLeftY}, W=${fieldRect.width}, H=${fieldRect.height}")
        }
    }

    private fun addSignatureToFixedPosition(document: PDDocument, signatureImageBytes: ByteArray) {
        val lastPage = document.pages.last()
        val pageSize = lastPage.mediaBox

        val signatureRect = Rectangle2D.Float(
            pageSize.width - 200f,
            50f,
            150f,
            75f
        )

        addImageToPage(document, lastPage, signatureImageBytes, signatureRect)
    }

    private fun addImageToPage(
        document: PDDocument,
        page: PDPage,
        imageBytes: ByteArray,
        rect: Rectangle2D.Float
    ) {
        val processedImageBytes = removeWhiteBackground(imageBytes)
        val pdImage = PDImageXObject.createFromByteArray(document, processedImageBytes, "signature")

        PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true).use { contentStream ->
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

    private fun removeWhiteBackground(imageBytes: ByteArray): ByteArray {
        try {
            val originalImage = ImageIO.read(ByteArrayInputStream(imageBytes))

            val transparentImage = BufferedImage(
                originalImage.width,
                originalImage.height,
                BufferedImage.TYPE_INT_ARGB
            )

            for (x in 0 until originalImage.width) {
                for (y in 0 until originalImage.height) {
                    val rgb = originalImage.getRGB(x, y)
                    val red = (rgb shr 16) and 0xFF
                    val green = (rgb shr 8) and 0xFF
                    val blue = rgb and 0xFF

                    val isWhiteish = red > 240 && green > 240 && blue > 240

                    if (isWhiteish) {
                        transparentImage.setRGB(x, y, (0x00FFFFFF and rgb).toInt())
                    } else {
                        transparentImage.setRGB(x, y, (0xFF000000.toInt() or rgb))
                    }
                }
            }

            val outputStream = ByteArrayOutputStream()
            ImageIO.write(transparentImage, "PNG", outputStream)
            return outputStream.toByteArray()

        } catch (e: Exception) {
            println("Błąd podczas usuwania białego tła: ${e.message}")
            return imageBytes
        }
    }

    private fun addNote(note: String?): String =
        if (!note.isNullOrBlank()) "[$note]" else ""

    fun LocalDateTime.format(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return this.format(formatter)
    }

    private fun getFormDataForProtokol(protocol: CarReceptionDetailDto): Map<String, String> {
        val notes = protocol.selectedServices.joinToString(",") {
            "${it.name} ${addNote(it.note)}"
        }

        return mapOf(
            "brand" to (protocol.make),
            "model" to (protocol.model),
            "licenseplate" to (protocol.licensePlate),
            "mileage" to (protocol.mileage?.toString() ?: ""),
            "services" to notes,
            "fullname" to (protocol.deliveryPerson?.name ?: protocol.ownerName),
            "companyname" to (protocol.companyName ?: ""),
            "Text7" to (""),
            "phonenumber" to (protocol.deliveryPerson?.phone ?: protocol.phone ?: ""),
            "email" to (protocol.deliveryPerson?.let { "" } ?: protocol.email ?: ""),
            "tax" to (protocol.taxId ?: ""),
            "remarks" to (protocol.notes?: ""),
            "date" to (protocol.startDate.format()),
            "price" to (protocol.selectedServices.sumOf { it.finalPrice.priceBrutto }.toString())
        )
    }
}