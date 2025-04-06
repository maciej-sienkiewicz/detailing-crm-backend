package com.carslab.crm.api.controller

import com.carslab.crm.domain.PdfService
import org.springframework.core.io.ByteArrayResource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/api/printer/")
class PrinterController(private val pdfService: PdfService) {

    @GetMapping("/protocol/{id}/pdf")
    fun generatePdf(@PathVariable id: Long): ResponseEntity<ByteArrayResource> {
        val pdfBytes = pdfService.generatePdf(id)

        val resource = ByteArrayResource(pdfBytes)

        val headers = HttpHeaders().apply {
            add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=protokol_$id.pdf")
            add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
            add(HttpHeaders.PRAGMA, "no-cache")
            add(HttpHeaders.EXPIRES, "0")
        }

        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(pdfBytes.size.toLong())
            .contentType(MediaType.APPLICATION_PDF)
            .body(resource)
    }
}