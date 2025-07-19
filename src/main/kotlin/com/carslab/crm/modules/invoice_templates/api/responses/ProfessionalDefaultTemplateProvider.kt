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
            description = "Elegancki szablon faktury dla firm detailingowych - zoptymalizowany dla Playwright",
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
                version = "2.0",
                author = "CarsLab CRM - Playwright Edition",
                tags = setOf("professional", "detailing", "default", "playwright"),
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
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            getBuiltInPlaywrightTemplate()
        }
    }

    private fun getBuiltInPlaywrightTemplate(): String {
        return """
        <!DOCTYPE html>
        <html xmlns="http://www.w3.org/1999/xhtml" lang="pl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Faktura {{invoice.number}}</title>
            <style>
                ${getDefaultCssStyles()}
            </style>
        </head>
        <body>
        <div class="invoice-wrapper">
            <header class="invoice-header">
                <div class="header-content">
                    <div class="logo-section">
                        <div class="company-logo">
                            {{logoHtml}}
                        </div>
                    </div>

                    <div class="company-details">
                        <h1 class="company-name">{{sellerName}}</h1>
                        <p class="company-address">{{sellerAddress}}</p>
                        <div class="contact-info">
                            <span class="contact-item">
                                <span class="label">NIP:</span>{{sellerTaxId}}
                            </span>
                            <span class="contact-item">
                                <span class="label">Tel:</span>{{sellerPhone}}
                            </span>
                            <span class="contact-item">
                                <span class="label">Web:</span>{{sellerWebsite}}
                            </span>
                        </div>
                    </div>

                    <div class="invoice-title-section">
                        <h2 class="document-title">Faktura</h2>
                        <div class="invoice-number">{{invoice.number}}</div>
                    </div>
                </div>
            </header>

            <section class="invoice-details">
                <div class="details-container">
                    <div class="date-item">
                        <span class="date-label">Data wystawienia:</span>
                        <span class="date-value">{{issuedDate}}</span>
                    </div>
                    <div class="date-item">
                        <span class="date-label">Termin płatności:</span>
                        <span class="date-value">{{dueDate}}</span>
                    </div>
                    <div class="date-item">
                        <span class="date-label">Sposób płatności:</span>
                        <span class="date-value">{{paymentMethod}}</span>
                    </div>
                </div>
            </section>

            <section class="parties-section">
                <div class="parties-grid">
                    <div class="party-card seller-card">
                        <h3 class="party-title">Sprzedawca</h3>
                        <div class="party-content">
                            <div class="party-name">{{sellerName}}</div>
                            <div class="party-address">{{sellerAddress}}</div>
                            <div class="party-tax">
                                <span class="tax-label">NIP:</span>
                                <span class="tax-value">{{sellerTaxId}}</span>
                            </div>
                        </div>
                    </div>

                    <div class="party-card buyer-card">
                        <h3 class="party-title">Nabywca</h3>
                        <div class="party-content">
                            <div class="party-name">{{buyerName}}</div>
                            <div class="party-address">{{buyerAddress}}</div>
                            <div class="party-tax">
                                <span class="tax-label">NIP:</span>
                                <span class="tax-value">{{buyerTaxId}}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            <section class="items-section">
                <table class="items-table">
                    <thead>
                    <tr>
                        <th class="col-number">Lp.</th>
                        <th class="col-description">Nazwa towaru / usługi</th>
                        <th class="col-quantity">Ilość</th>
                        <th class="col-unit-price">Cena jedn. netto</th>
                        <th class="col-vat">VAT</th>
                        <th class="col-net">Wartość netto</th>
                        <th class="col-gross">Wartość brutto</th>
                    </tr>
                    </thead>
                    <tbody>
                    {{itemsHtml}}
                    <tr class="vat-summary-row">
                        <td colspan="4" style="text-align: right; font-weight: 600;">RAZEM VAT</td>
                        <td class="numeric-value">{{totalTaxFormatted}}</td>
                        <td class="numeric-value">{{totalNetFormatted}}</td>
                        <td class="numeric-value">{{totalGrossFormatted}}</td>
                    </tr>
                    <tr class="total-summary-row">
                        <td colspan="4" style="text-align: right; font-weight: 700;">SUMA</td>
                        <td class="numeric-value">{{totalTaxFormatted}}</td>
                        <td class="numeric-value">{{totalNetFormatted}}</td>
                        <td class="total-gross numeric-value">{{totalGrossFormatted}}</td>
                    </tr>
                    </tbody>
                </table>
            </section>

            <section class="additional-info">
                <div class="info-grid">
                    <div class="bank-info">
                        <h4 class="info-title">Dane do przelewu</h4>
                        <div class="bank-details">
                            <div class="bank-item">
                                <span class="bank-label">Bank:</span>
                                <span class="bank-value">{{bankName}}</span>
                            </div>
                            <div class="bank-item">
                                <span class="bank-label">Nr konta:</span>
                                <span class="bank-value account-number">{{bankAccountNumber}}</span>
                            </div>
                        </div>
                    </div>

                    <div class="notes-section">
                        <h4 class="info-title">Uwagi</h4>
                        <p class="notes-content">{{notes}}</p>
                    </div>
                </div>
            </section>

            <footer class="invoice-footer">
                <div class="footer-content">
                    <div class="signature-section">
                        <div class="signature-line"></div>
                        <p class="signature-label">Osoba upoważniona do wystawiania faktur</p>
                    </div>

                    <div class="generation-info">
                        <p class="generation-text">
                            Dokument wygenerowany automatycznie w dniu:
                            <span class="generation-date">{{generatedAt}}</span>
                        </p>
                    </div>
                </div>
            </footer>
        </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun getDefaultCssStyles(): String {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            body {
                font-family: 'DejaVu Sans', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
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

            /* NAPRAWKA: Używamy CSS Grid/Flexbox - Playwright obsługuje wszystko! */
            .invoice-header {
                margin-bottom: 15px;
                padding-bottom: 10px;
                border-bottom: 2px solid #3498db;
            }

            .header-content {
                display: grid;
                grid-template-columns: auto 1fr auto;
                gap: 20px;
                align-items: center;
            }

            .logo-section .logo-img {
                max-width: 120px;
                max-height: 50px;
                object-fit: contain;
            }

            .company-initial {
                color: white;
                font-size: 18px;
                font-weight: bold;
                background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
                width: 45px;
                height: 45px;
                border-radius: 4px;
                display: flex;
                align-items: center;
                justify-content: center;
            }

            .company-details {
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

            .contact-item .label {
                font-weight: 600;
                margin-right: 3px;
                color: #34495e;
            }

            .invoice-title-section {
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

            .invoice-details {
                margin-bottom: 15px;
            }

            .details-container {
                border: 1px solid #bdc3c7;
                border-left: 3px solid #3498db;
                padding: 10px 15px;
                background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
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

            /* Parties Section - pełne wsparcie CSS Grid */
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

            /* Items Table */
            .items-section {
                margin-bottom: 15px;
            }

            .items-table {
                width: 100%;
                border-collapse: collapse;
                font-size: 10px;
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
                background: rgba(155, 89, 182, 0.1) !important;
                font-weight: 600;
                color: #2c3e50;
            }

            .total-summary-row td {
                border-top: 3px solid #34495e;
                border-bottom: 3px solid #34495e;
                background: linear-gradient(135deg, #34495e 0%, #2c3e50 100%) !important;
                font-weight: 700;
                color: white;
                font-size: 11px;
                padding: 8px 6px;
            }

            .total-summary-row .total-gross {
                color: #f39c12;
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

            /* Additional Info - Grid Layout */
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
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }

            .notes-section {
                border: 1px solid #bdc3c7;
                border-left: 3px solid #f39c12;
                padding: 10px 12px;
                background: white;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
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

            /* Footer */
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
                background: linear-gradient(135deg, #34495e 0%, #2c3e50 100%);
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

                /* Zachowaj kolory dla lepszego wyglądu w PDF */
                .company-initial,
                .total-summary-row td,
                .signature-line {
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
                .parties-grid,
                .info-grid {
                    grid-template-columns: 1fr;
                    gap: 15px;
                }

                .header-content {
                    grid-template-columns: 1fr;
                    text-align: center;
                }
            }
        """.trimIndent()
    }
}