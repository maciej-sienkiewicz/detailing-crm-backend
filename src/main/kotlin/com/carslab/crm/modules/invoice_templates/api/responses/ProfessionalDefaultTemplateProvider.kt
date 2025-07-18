package com.carslab.crm.modules.invoice_templates.infrastructure.templates

import com.carslab.crm.modules.invoice_templates.domain.model.*
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ProfessionalDefaultTemplateProvider {

    fun createDefaultTemplate(companyId: Long): InvoiceTemplate {
        return InvoiceTemplate(
            id = InvoiceTemplateId.generate(),
            companyId = companyId,
            name = "Profesjonalny szablon faktury",
            description = "Elegancki szablon faktury dla firm detailingowych",
            templateType = TemplateType.COMPANY_CUSTOM,
            content = TemplateContent(
                htmlTemplate = getDefaultHtmlTemplate(),
                cssStyles = getDefaultCssStyles(),
                logoPlacement = LogoPlacement(
                    position = LogoPosition.TOP_LEFT,
                    maxWidth = 180,
                    maxHeight = 90
                ),
                layout = LayoutSettings(
                    pageSize = PageSize.A4,
                    margins = Margins(top = 15, right = 15, bottom = 15, left = 15),
                    headerHeight = 120,
                    footerHeight = 80,
                    fontFamily = "DejaVuSans",
                    fontSize = 11
                )
            ),
            isActive = true,
            metadata = TemplateMetadata(
                version = "1.0",
                author = "CarsLab CRM",
                tags = setOf("professional", "detailing", "default"),
                legalCompliance = LegalCompliance(
                    country = "PL",
                    vatCompliant = true,
                    requiredFields = emptySet(),
                    lastLegalReview = LocalDateTime.now()
                ),
                supportedLanguages = setOf("pl")
            ),
            audit = com.carslab.crm.domain.model.Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ))
    }

    private fun getDefaultHtmlTemplate(): String {
        return try {
            ClassPathResource("static/template.html").inputStream.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    val content = reader.readText()
                    // NAPRAWKA: Oczyść szablon podczas ładowania
                    cleanTemplateForXmlCompatibility(content)
                }
            }
        } catch (e: Exception) {
            // FALLBACK: Jeśli nie można załadować z pliku, użyj wbudowanego szablonu
           ""
        }
    }

    // NAPRAWKA: Funkcja czyszcząca szablon dla kompatybilności z XML
    private fun cleanTemplateForXmlCompatibility(html: String): String {
        var cleaned = html

        // Napraw podstawowe problemy z self-closing tags
        cleaned = cleaned.replace("<meta charset=\"UTF-8\">", "<meta charset=\"UTF-8\"/>")
        cleaned = cleaned.replace(Regex("<br([^>]*[^/])?>")) { "<br/>" }
        cleaned = cleaned.replace(Regex("<hr([^>]*[^/])?>")) { "<hr/>" }
        cleaned = cleaned.replace(Regex("<img([^>]*[^/])>")) { "<img${it.groupValues[1]}/>" }

        // Usuń problematyczne CSS gradients
        cleaned = cleaned.replace("linear-gradient(135deg, #667eea 0%, #764ba2 100%)", "#667eea")
        cleaned = cleaned.replace("linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)", "#f8f9fa")
        cleaned = cleaned.replace("linear-gradient(135deg, #34495e 0%, #2c3e50 100%)", "#34495e")
        cleaned = cleaned.replace("linear-gradient(135deg, #2c3e50 0%, #34495e 100%)", "#2c3e50")

        // Usuń problematyczne CSS properties
        cleaned = cleaned.replace("rgba(0,0,0,0.1)", "rgb(230,230,230)")
        cleaned = cleaned.replace("rgba(0,0,0,0.05)", "rgb(240,240,240)")
        cleaned = cleaned.replace("rgba(255,255,255,0.1)", "rgb(255,255,255)")
        cleaned = cleaned.replace(Regex("box-shadow:[^;]+;"), "")

        return cleaned
    }

    private fun getDefaultCssStyles(): String {
        return """
            * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            font-size: 11px;
            line-height: 1.4;
            color: #2c3e50;
            background: #ffffff;
        }

        .invoice-wrapper {
            max-width: 210mm;
            margin: 0 auto;
            padding: 20mm;
            background: white;
            min-height: 297mm;
            position: relative;
        }

        /* Compact Header Section */
        .invoice-header {
            margin-bottom: 15px;
            padding-bottom: 10px;
            border-bottom: 2px solid #3498db;
        }

        .header-content {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .logo-section {
            flex: 0 0 auto;
            margin-right: 20px;
        }

        .company-logo .logo-img {
            max-width: 120px;
            max-height: 50px;
            object-fit: contain;
        }

        .company-initial {
            color: white;
            font-size: 18px;
            font-weight: bold;
            background: #3498db;
            width: 45px;
            height: 45px;
            border-radius: 4px;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .company-details {
            flex: 1;
            text-align: left;
            font-size: 9px;
            color: #5a6c7d;
            line-height: 1.3;
        }

        .company-name {
            font-size: 14px;
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 5px;
        }

        .contact-info {
            display: flex;
            gap: 15px;
            flex-wrap: wrap;
        }

        .contact-item {
            white-space: nowrap;
        }

        .contact-item .label {
            font-weight: 600;
            margin-right: 3px;
            color: #34495e;
        }

        /* Invoice Title in Header */
        .invoice-title-section {
            flex: 0 0 auto;
            text-align: right;
        }

        .document-title {
            font-size: 24px;
            font-weight: 300;
            color: #2c3e50;
            margin-bottom: 5px;
            letter-spacing: 2px;
            text-transform: uppercase;
        }

        .invoice-number {
            font-size: 14px;
            font-weight: 600;
            color: #3498db;
            padding: 4px 10px;
            border: 2px solid #3498db;
            display: inline-block;
        }

        /* Compact Invoice Details */
        .invoice-details {
            margin-bottom: 15px;
        }

        .details-container {
            border: 1px solid #bdc3c7;
            border-left: 3px solid #3498db;
            padding: 10px 15px;
            background: #fdfdfd;
        }

        .date-item {
            margin-bottom: 4px;
            display: flex;
            align-items: center;
        }

        .date-label {
            font-weight: 600;
            color: #34495e;
            margin-right: 10px;
            min-width: 110px;
            font-size: 10px;
        }

        .date-value {
            color: #2c3e50;
            font-weight: 500;
            font-size: 10px;
        }

        /* Payment status - removed */

        /* Compact Parties Section */
        .parties-section {
            margin-bottom: 15px;
        }

        .parties-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
        }

        .party-card {
            border: 1px solid #bdc3c7;
        }

        .seller-card {
            border-left: 3px solid #3498db;
        }

        .buyer-card {
            border-left: 3px solid #95a5a6;
        }

        .party-title {
            color: #34495e;
            padding: 8px 12px;
            font-size: 10px;
            font-weight: 600;
            letter-spacing: 1px;
            margin: 0;
            text-transform: uppercase;
            border-bottom: 1px solid #ecf0f1;
            background: white;
        }

        .party-content {
            padding: 10px 12px;
            background: white;
        }

        .party-name {
            font-weight: 700;
            font-size: 11px;
            color: #2c3e50;
            margin-bottom: 4px;
        }

        .party-address {
            color: #5a6c7d;
            margin-bottom: 4px;
            line-height: 1.3;
            font-size: 10px;
        }

        .party-tax {
            display: flex;
            align-items: center;
            margin-top: 4px;
        }

        .tax-label {
            font-weight: 600;
            color: #34495e;
            margin-right: 4px;
            font-size: 10px;
        }

        .tax-value {
            color: #2c3e50;
            font-weight: 600;
            font-family: 'Courier New', monospace;
            font-size: 10px;
        }

        /* Compact Items Table */
        .items-section {
            margin-bottom: 15px;
        }

        .items-table th {
            color: #2c3e50;
            padding: 8px 6px;
            text-align: center;
            font-weight: 600;
            font-size: 9px;
            border-bottom: 2px solid #3498db;
            text-transform: uppercase;
            background: white;
        }

        .items-table td {
            padding: 6px;
            border-bottom: 1px solid #ecf0f1;
            text-align: center;
            vertical-align: top;
            background: white;
            font-size: 10px;
        }

        .item-row:nth-child(even) td {
            background: #fdfdfd;
        }

        .vat-summary-row td {
            border-top: 2px solid #9b59b6;
            border-bottom: 1px solid #9b59b6;
            background: white !important;
            font-weight: 600;
            color: #2c3e50;
        }

        .vat-summary-row:nth-child(even) td {
            background: white !important;
        }

        .total-summary-row td {
            border-top: 3px solid #34495e;
            border-bottom: 3px solid #34495e;
            background: white !important;
            font-weight: 700;
            color: #2c3e50;
            font-size: 11px;
            padding: 8px 6px;
        }

        .total-summary-row .total-gross {
            color: #e67e22;
            font-weight: 800;
        }

        .col-number { width: 6%; }
        .col-description { width: 35%; }
        .col-quantity { width: 8%; }
        .col-unit-price { width: 15%; }
        .col-vat { width: 8%; }
        .col-net { width: 14%; }
        .col-gross { width: 14%; }

        .item-description {
            text-align: left;
            padding-left: 10px;
        }

        .service-name {
            font-weight: 600;
            color: #2c3e50;
            margin-bottom: 2px;
            font-size: 10px;
        }

        .service-details {
            font-size: 8px;
            color: #7f8c8d;
            font-style: italic;
        }

        .item-number {
            font-weight: 600;
            color: #34495e;
        }

        .numeric-value {
            font-family: 'Courier New', monospace;
            font-weight: 500;
        }

        /* VAT Summary - removed as separate section */

        /* Total Summary - removed as separate section */

        /* Compact Additional Info */
        .additional-info {
            margin-bottom: 15px;
        }

        .info-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
        }

        .bank-info {
            border: 1px solid #bdc3c7;
            border-left: 3px solid #27ae60;
            padding: 10px 12px;
            background: white;
        }

        .notes-section {
            border: 1px solid #bdc3c7;
            border-left: 3px solid #f39c12;
            padding: 10px 12px;
            background: white;
        }

        .info-title {
            color: #2c3e50;
            margin-bottom: 6px;
            font-size: 10px;
            font-weight: 600;
            text-transform: uppercase;
        }

        .bank-item {
            margin-bottom: 4px;
            display: flex;
            align-items: center;
        }

        .bank-label {
            font-weight: 600;
            color: #34495e;
            margin-right: 6px;
            min-width: 60px;
            font-size: 9px;
        }

        .bank-value {
            color: #2c3e50;
            font-size: 9px;
        }

        .account-number {
            font-family: 'Courier New', monospace;
            font-weight: 600;
        }

        .notes-content {
            color: #5a6c7d;
            line-height: 1.4;
            font-size: 9px;
        }

        /* Compact Footer */
        .invoice-footer {
            border-top: 1px solid #bdc3c7;
            padding-top: 10px;
            margin-top: auto;
        }

        .footer-content {
            display: flex;
            justify-content: space-between;
            align-items: flex-end;
        }

        .signature-section {
            flex: 1;
        }

        .signature-line {
            width: 150px;
            height: 1px;
            background: #34495e;
            margin-bottom: 5px;
            margin-top: 20px;
        }

        .signature-label {
            font-size: 8px;
            color: #7f8c8d;
            text-align: center;
            width: 150px;
            font-weight: 600;
        }

        .generation-info {
            flex: 1;
            text-align: right;
        }

        .generation-text {
            font-size: 8px;
            color: #95a5a6;
        }

        .generation-date {
            font-weight: 600;
            color: #7f8c8d;
        }

        /* Print Optimizations */
        @media print {
            .invoice-wrapper {
                padding: 10mm;
                max-width: none;
                margin: 0;
            }

            body {
                font-size: 9px;
            }

            /* Remove color fills but keep borders for print */
            .details-container, .party-content, .items-table td,
            .bank-info, .notes-section {
                background: white !important;
            }

            .company-initial {
                background: #3498db !important;
                -webkit-print-color-adjust: exact;
                color-adjust: exact;
            }

            /* Keep colored borders for visual structure */
            .invoice-header, .invoice-number, .details-container,
            .seller-card, .buyer-card, .bank-info, .notes-section {
                -webkit-print-color-adjust: exact;
                color-adjust: exact;
            }
        }

        @page {
            margin: 10mm;
            size: A4;
        }

        /* Responsive adjustments */
        @media (max-width: 768px) {
            .parties-grid {
                grid-template-columns: 1fr;
                gap: 15px;
            }

            .details-grid {
                flex-direction: column;
                align-items: flex-start;
            }

            .info-grid {
                grid-template-columns: 1fr;
                gap: 15px;
            }

            .summary-grid {
                grid-template-columns: 1fr;
                gap: 10px;
            }
        }
        """.trimIndent()
    }
}