package com.carslab.crm.api.controller

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.visits.protocols.PdfService
import com.carslab.crm.modules.visits.infrastructure.storage.ProtocolDocumentStorageService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/printer/")
class PrinterController(
    private val protocolDocumentStorageService: ProtocolDocumentStorageService,
    private val pdfService: PdfService) {

    @GetMapping(
        value = ["/protocol/{id}/pdf"],
        produces = [MediaType.APPLICATION_PDF_VALUE]
    )
    fun generatePdf(
        @PathVariable id: Long,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ByteArrayResource> {
        val resource = (protocolDocumentStorageService
            .findAcceptanceProtocol(ProtocolId(id.toString()))
            ?: pdfService.generatePdf(id))
            .let { ByteArrayResource(it) }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=protokol_$id.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(resource)
    }
}