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
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Template not found: static/template.html", e)
        }
    }

    private fun getDefaultCssStyles(): String {
        return """
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: 'DejaVuSans', Arial, sans-serif;
    font-size: 11px;
    line-height: 1.5;
    color: #2c3e50;
    background: #ffffff;
}

.invoice-wrapper {
    max-width: 210mm;
    margin: 0 auto;
    padding: 15mm;
    background: white;
    min-height: 297mm;
}

/* Header Styles */
.invoice-header {
    border-bottom: 3px solid #34495e;
    padding-bottom: 20px;
    margin-bottom: 25px;
}

.header-content {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
}

.logo-section {
    flex: 0 0 auto;
    margin-right: 30px;
}

.company-logo .logo-img {
    max-width: 180px;
    max-height: 90px;
    object-fit: contain;
}

.logo-placeholder {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.company-initial {
    color: white;
    font-size: 32px;
    font-weight: bold;
}

.company-details {
    flex: 1;
    text-align: right;
}

.company-name {
    font-size: 22px;
    font-weight: bold;
    color: #2c3e50;
    margin-bottom: 10px;
    letter-spacing: 0.5px;
}

.company-info {
    font-size: 10px;
    color: #5a6c7d;
}

.contact-row {
    margin: 3px 0;
}

.contact-row .label {
    font-weight: 600;
    margin-right: 8px;
    color: #34495e;
}

.contact-row .value {
    color: #5a6c7d;
}

.invoice-title-section {
    text-align: center;
    padding: 15px 0;
    background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
    border-radius: 8px;
}

.document-title {
    font-size: 28px;
    font-weight: bold;
    color: #2c3e50;
    margin-bottom: 5px;
    letter-spacing: 2px;
}

.invoice-number {
    font-size: 16px;
    font-weight: 600;
    color: #e74c3c;
    padding: 5px 15px;
    background: white;
    border-radius: 20px;
    display: inline-block;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

/* Details Section */
.invoice-details {
    margin-bottom: 25px;
}

.details-grid {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #f8f9fa;
    padding: 15px 20px;
    border-radius: 8px;
    border-left: 4px solid #3498db;
}

.invoice-dates {
    flex: 1;
}

.date-item {
    margin-bottom: 5px;
    display: flex;
    align-items: center;
}

.date-label {
    font-weight: 600;
    color: #34495e;
    margin-right: 10px;
    min-width: 130px;
}

.date-value {
    color: #2c3e50;
    font-weight: 500;
}

.payment-status {
    flex: 0 0 auto;
}

.status-badge {
    padding: 8px 16px;
    border-radius: 20px;
    font-weight: 600;
    font-size: 10px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.status-paid {
    background: #d5f4e6;
    color: #27ae60;
}

.status-unpaid {
    background: #fde2e4;
    color: #e74c3c;
}

/* Parties Section */
.parties-section {
    margin-bottom: 25px;
}

.parties-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
}

.party-card {
    border: 1px solid #e9ecef;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.party-title {
    background: #34495e;
    color: white;
    padding: 12px 15px;
    font-size: 11px;
    font-weight: bold;
    letter-spacing: 1px;
    margin: 0;
}

.party-content {
    padding: 15px;
}

.party-name {
    font-weight: bold;
    font-size: 12px;
    color: #2c3e50;
    margin-bottom: 8px;
}

.party-address {
    color: #5a6c7d;
    margin-bottom: 8px;
    line-height: 1.4;
}

.party-tax {
    display: flex;
    align-items: center;
}

.tax-label {
    font-weight: 600;
    color: #34495e;
    margin-right: 8px;
}

.tax-value {
    color: #2c3e50;
    font-weight: 500;
}

/* Items Table */
.items-section {
    margin-bottom: 25px;
}

.table-container {
    overflow-x: auto;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.items-table {
    width: 100%;
    border-collapse: collapse;
    background: white;
}

.items-table th {
    background: linear-gradient(135deg, #34495e 0%, #2c3e50 100%);
    color: white;
    padding: 12px 8px;
    text-align: center;
    font-weight: bold;
    font-size: 10px;
    border: none;
}

.items-table td {
    padding: 12px 8px;
    border-bottom: 1px solid #e9ecef;
    text-align: center;
    vertical-align: top;
}

.item-row:nth-child(even) {
    background: #f8f9fa;
}

.item-row:hover {
    background: #e8f4f8;
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
}

.service-name {
    font-weight: 600;
    color: #2c3e50;
    margin-bottom: 4px;
}

.service-details {
    font-size: 9px;
    color: #7f8c8d;
    font-style: italic;
}

.item-number {
    font-weight: 600;
    color: #34495e;
}

/* VAT Summary */
.vat-summary-section {
    margin-bottom: 20px;
}

.summary-container {
    background: #f8f9fa;
    border-radius: 8px;
    padding: 15px;
    border-left: 4px solid #9b59b6;
}

.summary-title {
    color: #2c3e50;
    margin-bottom: 10px;
    font-size: 12px;
    font-weight: bold;
}

.vat-table {
    width: 100%;
    border-collapse: collapse;
}

.vat-table th {
    background: #9b59b6;
    color: white;
    padding: 8px;
    text-align: center;
    font-size: 10px;
    font-weight: bold;
}

.vat-table td {
    padding: 8px;
    text-align: center;
    border-bottom: 1px solid #dee2e6;
}

.vat-row:nth-child(even) {
    background: white;
}

/* Total Summary */
.total-summary {
    margin-bottom: 25px;
    background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
    color: white;
    padding: 20px;
    border-radius: 8px;
}

.summary-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 15px;
}

.summary-item {
    text-align: center;
}

.summary-label {
    display: block;
    font-size: 10px;
    margin-bottom: 5px;
    opacity: 0.9;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.summary-value {
    display: block;
    font-size: 14px;
    font-weight: bold;
}

.total-amount {
    background: rgba(255,255,255,0.1);
    padding: 10px;
    border-radius: 6px;
}

.total-amount .summary-value {
    font-size: 18px;
    color: #f39c12;
}

/* Additional Info */
.additional-info {
    margin-bottom: 25px;
}

.info-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
}

.bank-info, .notes-section {
    background: #f8f9fa;
    border-radius: 8px;
    padding: 15px;
    border-left: 4px solid #27ae60;
}

.info-title {
    color: #2c3e50;
    margin-bottom: 10px;
    font-size: 11px;
    font-weight: bold;
}

.bank-item {
    margin-bottom: 5px;
    display: flex;
    align-items: center;
}

.bank-label {
    font-weight: 600;
    color: #34495e;
    margin-right: 8px;
    min-width: 70px;
}

.bank-value {
    color: #2c3e50;
}

.account-number {
    font-family: 'Courier New', monospace;
    font-weight: bold;
}

.notes-content {
    color: #5a6c7d;
    line-height: 1.4;
}

/* Footer */
.invoice-footer {
    border-top: 2px solid #e9ecef;
    padding-top: 20px;
    margin-top: auto;
}

.footer-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.signature-section {
    flex: 1;
}

.signature-line {
    width: 200px;
    height: 1px;
    background: #bdc3c7;
    margin-bottom: 8px;
}

.signature-label {
    font-size: 9px;
    color: #7f8c8d;
    text-align: center;
    width: 200px;
}

.generation-info {
    flex: 1;
    text-align: right;
}

.generation-text {
    font-size: 9px;
    color: #95a5a6;
}

.generation-date {
    font-weight: 600;
}

/* Print Styles */
@media print {
    .invoice-wrapper {
        padding: 0;
        max-width: none;
        margin: 0;
    }
    
    body {
        font-size: 10px;
    }
    
    .invoice-header,
    .items-section {
        page-break-inside: avoid;
    }
    
    .item-row {
        page-break-inside: avoid;
    }
}

@page {
    margin: 15mm;
    size: A4;
}
        """.trimIndent()
    }
}