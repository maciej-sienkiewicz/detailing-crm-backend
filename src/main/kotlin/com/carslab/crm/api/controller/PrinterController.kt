package com.carslab.crm.api.controller

import com.carslab.crm.domain.PdfService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/printer/")
@CrossOrigin(origins = ["*"])
class PrinterController(private val pdfService: PdfService) {

    @GetMapping(
        value = ["/protocol/{id}/pdf"],
        produces = [MediaType.APPLICATION_PDF_VALUE]
    )
    fun generatePdf(
        @PathVariable id: Long,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ByteArrayResource> {
        println("Nagłówki requestu:")
        request.headerNames.asIterator().forEachRemaining { headerName ->
            println("$headerName: ${request.getHeader(headerName)}")
        }

        // Ustaw ręcznie nagłówki CORS
        response.setHeader("Access-Control-Allow-Credentials", "true")
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition, Content-Type")

        val pdfBytes = pdfService.generatePdf(id)
        val resource = ByteArrayResource(pdfBytes)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=protokol_$id.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(resource)
    }
}