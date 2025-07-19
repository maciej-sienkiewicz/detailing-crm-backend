package com.carslab.crm.modules.invoice_templates.infrastructure.rendering

import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceGenerationData
import com.carslab.crm.modules.invoice_templates.domain.ports.TemplateRenderingService
import com.carslab.crm.modules.invoice_templates.infrastructure.templates.ProfessionalDefaultTemplateProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class ThymeleafTemplateRenderingService : TemplateRenderingService {
    private val logger = LoggerFactory.getLogger(ThymeleafTemplateRenderingService::class.java)

    override fun renderTemplate(template: InvoiceTemplate, data: InvoiceGenerationData): String {
        try {
            val variables = prepareVariables(data)

            var htmlContent = template.content.htmlTemplate

            // Przeprowadź podstawienia zmiennych
            variables.forEach { (key, value) ->
                htmlContent = htmlContent.replace("{{$key}}", value.toString())
            }

            // Usuń nieużywane placeholder'y
            htmlContent = htmlContent.replace(Regex("\\{\\{[^}]+\\}\\}"), "")

            logger.debug("Template rendered successfully")
            return htmlContent

        } catch (e: Exception) {
            logger.error("Error rendering template: {}", template.name, e)
            throw RuntimeException("Failed to render invoice template", e)
        }
    }

    override fun validateTemplateSyntax(htmlTemplate: String): Boolean {
        return try {
            htmlTemplate.contains("<div") || htmlTemplate.contains("<html")
        } catch (e: Exception) {
            false
        }
    }

    private fun prepareVariables(data: InvoiceGenerationData): Map<String, Any> {
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        // Logo
        val logoHtml = if (data.logoData != null) {
            val logoBase64 = Base64.getEncoder().encodeToString(data.logoData)
            """<img src="data:image/png;base64,$logoBase64" alt="Logo firmy" class="logo-img"/>"""
        } else {
            """<div class="company-initial">${escapeHtml(data.companySettings.basicInfo.companyName.take(1))}</div>"""
        }

        // NAPRAWKA: Items HTML - bez dodatkowych podsumowań
        val itemsHtml = data.document.items.mapIndexed { index, item ->
            """
            <tr class="item-row">
                <td class="item-number">${index + 1}</td>
                <td class="item-description">
                    <div class="service-name">${escapeHtml(item.name)}</div>
                    ${if (item.description?.isNotEmpty() == true) """<div class="service-details">${escapeHtml(item.description)}</div>""" else ""}
                </td>
                <td class="item-quantity">${item.quantity}</td>
                <td class="item-unit-price">${formatMoney(item.unitPrice)}</td>
                <td class="item-vat">${item.taxRate}%</td>
                <td class="item-net">${formatMoney(item.totalNet)}</td>
                <td class="item-gross">${formatMoney(item.totalGross)}</td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")

        // NAPRAWKA: Pojedyncze, profesjonalne podsumowanie
        val summaryHtml = """
            <tr class="summary-row">
                <td colspan="4" class="summary-label">RAZEM</td>
                <td class="summary-tax">${formatMoney(data.document.totalTax)}</td>
                <td class="summary-net">${formatMoney(data.document.totalNet)}</td>
                <td class="summary-gross">${formatMoney(data.document.totalGross)}</td>
            </tr>
        """.trimIndent()

        return mapOf(
            "invoice.number" to escapeHtml(data.document.number),
            "sellerName" to escapeHtml(data.companySettings.basicInfo.companyName),
            "sellerAddress" to escapeHtml(data.companySettings.basicInfo.address ?: ""),
            "sellerTaxId" to escapeHtml(data.companySettings.basicInfo.taxId ?: ""),
            "sellerPhone" to escapeHtml(data.companySettings.basicInfo.phone ?: ""),
            "sellerWebsite" to escapeHtml(data.companySettings.basicInfo.website ?: ""),
            "buyerName" to escapeHtml(data.document.buyerName ?: ""),
            "buyerAddress" to escapeHtml(data.document.buyerAddress ?: ""),
            "buyerTaxId" to escapeHtml(data.document.buyerTaxId ?: ""),
            "issuedDate" to data.document.issuedDate.format(dateFormatter),
            "dueDate" to (data.document.dueDate?.format(dateFormatter) ?: ""),
            "paymentMethod" to formatPaymentMethod(data.document.paymentMethod),
            "paymentStatus" to formatPaymentStatus(data.document.status),
            "statusClass" to if (formatPaymentStatus(data.document.status) == "Opłacona") "status-paid" else "status-unpaid",
            "totalNetFormatted" to formatMoney(data.document.totalNet),
            "totalTaxFormatted" to formatMoney(data.document.totalTax),
            "totalGrossFormatted" to formatMoney(data.document.totalGross),
            "logoHtml" to logoHtml,
            "itemsHtml" to itemsHtml,
            // NAPRAWKA: Usuń vatSummaryHtml i zastąp summaryHtml
            "summaryHtml" to summaryHtml,
            "generatedAt" to java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
            "hasLogo" to (data.logoData != null),
            "bankAccountNumber" to escapeHtml(data.companySettings.bankSettings?.bankAccountNumber ?: ""),
            "bankName" to escapeHtml(data.companySettings.bankSettings?.bankName ?: ""),
            "notes" to escapeHtml(data.document.notes ?: "")
        )
    }

    private fun escapeHtml(text: String?): String {
        if (text == null) return ""
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun formatMoney(amount: BigDecimal): String {
        return String.format("%.2f zł", amount)
    }

    private fun formatPaymentMethod(method: com.carslab.crm.domain.model.view.finance.PaymentMethod): String {
        return when (method) {
            com.carslab.crm.domain.model.view.finance.PaymentMethod.CASH -> "Gotówka"
            com.carslab.crm.domain.model.view.finance.PaymentMethod.BANK_TRANSFER -> "Przelew bankowy"
            com.carslab.crm.domain.model.view.finance.PaymentMethod.CARD -> "Karta płatnicza"
            else -> "Inna"
        }
    }

    private fun formatPaymentStatus(status: com.carslab.crm.api.model.DocumentStatus): String {
        return when (status) {
            com.carslab.crm.api.model.DocumentStatus.PAID -> "Opłacona"
            com.carslab.crm.api.model.DocumentStatus.NOT_PAID -> "Nieopłacona"
            com.carslab.crm.api.model.DocumentStatus.PARTIALLY_PAID -> "Częściowo opłacona"
            com.carslab.crm.api.model.DocumentStatus.OVERDUE -> "Przeterminowana"
            com.carslab.crm.api.model.DocumentStatus.CANCELLED -> "Anulowana"
        }
    }
}