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
            getBuiltInHtmlTemplate()
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

    // NAPRAWKA: Wbudowany szablon jako fallback
    private fun getBuiltInHtmlTemplate(): String {
        return """
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta charset="UTF-8"/>
    <title>Faktura {{invoice.number}}</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: DejaVuSans, Arial, sans-serif;
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
        
        .company-name {
            font-size: 22px;
            font-weight: bold;
            color: #2c3e50;
        }
        
        .invoice-title-section {
            text-align: center;
            padding: 15px 0;
            background: #f8f9fa;
            border-radius: 8px;
        }
        
        .document-title {
            font-size: 28px;
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 5px;
        }
        
        .invoice-number {
            font-size: 16px;
            font-weight: 600;
            color: #e74c3c;
        }
        
        .items-table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
        }
        
        .items-table th, .items-table td {
            border: 1px solid #ddd;
            padding: 8px;
            text-align: left;
        }
        
        .items-table th {
            background: #34495e;
            color: white;
        }
        
        @page {
            margin: 15mm;
            size: A4;
        }
    </style>
</head>
<body>
<div class="invoice-wrapper">
    <header class="invoice-header">
        <div class="header-content">
            <div class="logo-section">{{logoHtml}}</div>
            <div class="company-details">
                <h1 class="company-name">{{sellerName}}</h1>
            </div>
        </div>
        <div class="invoice-title-section">
            <h2 class="document-title">FAKTURA</h2>
            <div class="invoice-number">{{invoice.number}}</div>
        </div>
    </header>
    
    <section>
        <p>Data wystawienia: {{issuedDate}}</p>
        <p>Termin płatności: {{dueDate}}</p>
        <p>Sprzedawca: {{sellerName}}</p>
        <p>Nabywca: {{buyerName}}</p>
    </section>
    
    <section>
        <table class="items-table">
            <thead>
                <tr>
                    <th>Lp.</th>
                    <th>Nazwa</th>
                    <th>Ilość</th>
                    <th>Cena netto</th>
                    <th>VAT</th>
                    <th>Wartość brutto</th>
                </tr>
            </thead>
            <tbody>
                {{itemsHtml}}
            </tbody>
        </table>
    </section>
    
    <section>
        <p><strong>Razem netto: {{totalNetFormatted}}</strong></p>
        <p><strong>Razem VAT: {{totalTaxFormatted}}</strong></p>
        <p><strong>Razem brutto: {{totalGrossFormatted}}</strong></p>
    </section>
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
    font-family: DejaVuSans, Arial, sans-serif;
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

.company-name {
    font-size: 22px;
    font-weight: bold;
    color: #2c3e50;
    margin-bottom: 10px;
    letter-spacing: 0.5px;
}

.invoice-title-section {
    text-align: center;
    padding: 15px 0;
    background: #f8f9fa;
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
}

.items-table {
    width: 100%;
    border-collapse: collapse;
    background: white;
    margin: 20px 0;
}

.items-table th {
    background: #34495e;
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

@media print {
    .invoice-wrapper {
        padding: 0;
        max-width: none;
        margin: 0;
    }
    
    body {
        font-size: 10px;
    }
}

@page {
    margin: 15mm;
    size: A4;
}
        """.trimIndent()
    }
}