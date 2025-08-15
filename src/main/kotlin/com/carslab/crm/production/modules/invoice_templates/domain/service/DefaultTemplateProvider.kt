package com.carslab.crm.production.modules.invoice_templates.domain.service

import org.springframework.stereotype.Component

@Component
class DefaultTemplateProvider {

    fun getDefaultHtmlTemplate(): String {
        return """
        <!DOCTYPE html>
        <html lang="pl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Faktura {{invoice.number}}</title>
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }

                body {
                    font-family: 'DejaVu Sans', Arial, sans-serif;
                    font-size: 12px;
                    line-height: 1.4;
                    color: #333;
                    background: white;
                }

                .invoice-container {
                    max-width: 210mm;
                    margin: 0 auto;
                    padding: 20mm;
                    background: white;
                    min-height: 297mm;
                }

                .header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 20px;
                    padding-bottom: 15px;
                    border-bottom: 2px solid #007acc;
                }

                .company-logo {
                    max-width: 150px;
                    max-height: 80px;
                }

                .invoice-title {
                    text-align: right;
                }

                .invoice-title h1 {
                    font-size: 28px;
                    color: #007acc;
                    margin-bottom: 10px;
                }

                .invoice-number {
                    font-size: 16px;
                    font-weight: bold;
                    color: #333;
                }

                .invoice-details {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 20px;
                    margin-bottom: 30px;
                }

                .party-card {
                    border: 1px solid #ddd;
                    padding: 15px;
                    background: #f9f9f9;
                }

                .party-title {
                    font-weight: bold;
                    margin-bottom: 10px;
                    color: #007acc;
                }

                .party-content {
                    line-height: 1.6;
                }

                .invoice-meta {
                    margin-bottom: 30px;
                    padding: 10px;
                    background: #f5f5f5;
                    border-left: 4px solid #007acc;
                }

                .items-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 30px;
                }

                .items-table th,
                .items-table td {
                    border: 1px solid #ddd;
                    padding: 8px;
                    text-align: left;
                }

                .items-table th {
                    background: #007acc;
                    color: white;
                    font-weight: bold;
                }

                .items-table .numeric {
                    text-align: right;
                }

                .total-row {
                    background: #f0f0f0;
                    font-weight: bold;
                }

                .footer {
                    margin-top: 50px;
                    padding-top: 20px;
                    border-top: 1px solid #ddd;
                    text-align: center;
                    font-size: 10px;
                    color: #666;
                }

                @media print {
                    .invoice-container {
                        padding: 10mm;
                        margin: 0;
                    }
                }
            </style>
        </head>
        <body>
            <div class="invoice-container">
                <header class="header">
                    <div class="company-info">
                        {{logoHtml}}
                        <div>
                            <strong>{{sellerName}}</strong><br>
                            {{sellerAddress}}<br>
                            NIP: {{sellerTaxId}}
                        </div>
                    </div>
                    <div class="invoice-title">
                        <h1>FAKTURA</h1>
                        <div class="invoice-number">{{invoice.number}}</div>
                    </div>
                </header>

                <div class="invoice-meta">
                    <strong>Data wystawienia:</strong> {{issuedDate}}<br>
                    <strong>Termin płatności:</strong> {{dueDate}}<br>
                    <strong>Sposób płatności:</strong> {{paymentMethod}}
                </div>

                <div class="invoice-details">
                    <div class="party-card">
                        <div class="party-title">Sprzedawca</div>
                        <div class="party-content">
                            <strong>{{sellerName}}</strong><br>
                            {{sellerAddress}}<br>
                            NIP: {{sellerTaxId}}
                        </div>
                    </div>
                    <div class="party-card">
                        <div class="party-title">Nabywca</div>
                        <div class="party-content">
                            <strong>{{buyerName}}</strong><br>
                            {{buyerAddress}}<br>
                            NIP: {{buyerTaxId}}
                        </div>
                    </div>
                </div>

                <table class="items-table">
                    <thead>
                        <tr>
                            <th>Lp.</th>
                            <th>Nazwa towaru/usługi</th>
                            <th>Ilość</th>
                            <th>Cena jedn. netto</th>
                            <th>VAT %</th>
                            <th>Wartość netto</th>
                            <th>Wartość brutto</th>
                        </tr>
                    </thead>
                    <tbody>
                        {{itemsHtml}}
                        <tr class="total-row">
                            <td colspan="5"><strong>RAZEM</strong></td>
                            <td class="numeric"><strong>{{totalNetFormatted}}</strong></td>
                            <td class="numeric"><strong>{{totalGrossFormatted}}</strong></td>
                        </tr>
                    </tbody>
                </table>

                <div class="footer">
                    <p>Dokument wygenerowany: {{generatedAt}}</p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}