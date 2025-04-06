package com.carslab.crm.domain

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField
import org.springframework.stereotype.Service
import org.springframework.util.ResourceUtils
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class PdfService(
    private val protocolFacade: CarReceptionFacade,
) {

    fun generatePdf(protocolId: Long): ByteArray {
        val protocol: CarReceptionProtocol = protocolFacade.getProtocolById(ProtocolId(protocolId.toString()))
            ?: throw IllegalStateException("Nie znaleziono protokołu o ID: $protocolId")

        // Na razie używamy przykładowych danych
        val formData = getFormDataForProtokol(protocol)

        // Ścieżka do pliku szablonu PDF
        val pdfTemplate = ResourceUtils.getFile("classpath:static/x.pdf")

        ByteArrayOutputStream().use { outputStream ->
            PDDocument.load(pdfTemplate).use { document ->
                val acroForm: PDAcroForm? = document.documentCatalog.acroForm

                if (acroForm == null) {
                    throw IllegalStateException("Brak formularza w podanym pliku PDF.")
                }

                // Wypełnianie pól formularza
                formData.forEach { (fieldName, value) ->
                    val field: PDTextField = acroForm.getField(fieldName) as PDTextField
                    if (field != null) {
                        field.setValue(value.transliterate())
                    } else {
                        println("Nie znaleziono pola: $fieldName")
                    }
                }

                // Tutaj możemy zatwierdzić odpowiednie pola (np. przyciski)
                acroForm.getField("Button20")?.setValue(if(protocol.documents.keysProvided) "No" else "Off")
                acroForm.getField("Button21")?.setValue(if(protocol.documents.documentsProvided) "No" else "Off")
                document.documentCatalog.acroForm?.flatten()
                // Zapisz wypełniony dokument do strumienia bajtów
                document.save(outputStream)
            }

            return outputStream.toByteArray()
        }
    }

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

    private fun addNote(note: String?): String =
        if(!note.isNullOrBlank()) "[$note]" else ""

    fun LocalDateTime.format(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return this.format(formatter)
    }

    private fun getFormDataForProtokol(protocol: CarReceptionProtocol): Map<String, String> {
        val notes = protocol.protocolServices?.joinToString("\n") {
            """
                ${it.name} (${it.quantity} szt.) ${addNote(it.note)}
                """.trimIndent()
        } ?: ""

        // Przykładowe dane dla demonstracji
        return mapOf(
            "Text1" to (protocol.vehicle.make),
            "Text2" to (protocol.vehicle.model),
            "Text3" to (protocol.vehicle.licensePlate),
            "Text4" to (protocol.vehicle.mileage?.toString() ?: ""),
            "Text11" to notes,
            "Text5" to (protocol.client.name ?: ""),
            "Text6" to (protocol.client.companyName ?: ""),
            "Text7" to (""),
            "Text8" to (protocol.client.phone ?: ""),
            "Text9" to (protocol.client.email ?: ""),
            "Text10" to (protocol.client.taxId ?: ""),
            "Text12" to (protocol.notes?: ""),
            "Text28" to (protocol.period.startDate.format() ?: ""),
        )
    }
}