package com.carslab.crm.api.controller

import com.carslab.crm.domain.PdfService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/printer/")
@CrossOrigin(origins = ["*"])
class PrinterController(private val pdfService: PdfService) {

    @GetMapping("/protocol/{id}/pdf")
    fun generatePdf(@PathVariable id: Long, request: HttpServletRequest): ResponseEntity<ByteArrayResource> {
        println("Origin: ${request.getHeader("Origin")}")
        println("Access-Control-Request-Method: ${request.getHeader("Access-Control-Request-Method")}")

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