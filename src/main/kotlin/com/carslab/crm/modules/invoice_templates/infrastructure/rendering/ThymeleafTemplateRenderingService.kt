// Simplified version without complex regex patterns
package com.carslab.crm.modules.invoice_templates.infrastructure.rendering

import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceGenerationData
import com.carslab.crm.modules.invoice_templates.domain.ports.TemplateRenderingService
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

            // Simple string replacement approach - no complex regex
            variables.forEach { (key, value) ->
                htmlContent = htmlContent.replace("{{$key}}", value.toString())
            }

            // Handle simple conditional blocks using basic string operations
            htmlContent = handleSimpleConditionals(htmlContent, variables)

            // Clean up any remaining template variables
            htmlContent = htmlContent.replace(Regex("\\{\\{[^}]+\\}\\}"), "")

            logger.debug("Template rendered successfully with seller signature support")
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

        val logoHtml = if (data.logoData != null) {
            val logoBase64 = Base64.getEncoder().encodeToString(data.logoData)
            """<img src="data:image/png;base64,$logoBase64" alt="Logo firmy" class="logo-img"/>"""
        } else {
            """<div class="company-initial">${escapeHtml(data.companySettings.basicInfo.companyName.take(1))}</div>"""
        }

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

        // Enhanced signature handling
        val clientSignatureHtml = data.additionalData["client_signature"] as? String ?: run {
            """<div class="signature-placeholder">Miejsce na podpis klienta</div>"""
        }

        val sellerSignatureHtml = data.additionalData["seller_signature"] as? String ?: run {
            """<div class="signature-placeholder">Miejsce na podpis sprzedawcy</div>"""
        }

        // Check if signatures are present for conditional rendering
        val hasClientSignature = (data.additionalData["client_signature"] as? String)?.isNotEmpty() == true &&
                !(data.additionalData["client_signature"] as String).contains("placeholder")

        val hasSellerSignature = (data.additionalData["seller_signature"] as? String)?.isNotEmpty() == true &&
                !(data.additionalData["seller_signature"] as String).contains("placeholder")

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
            "client_signature" to clientSignatureHtml,
            "seller_signature" to sellerSignatureHtml, // NEW: Seller signature variable
            "hasClientSignature" to hasClientSignature, // NEW: Conditional flag
            "hasSellerSignature" to hasSellerSignature, // NEW: Conditional flag
            "generatedAt" to java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
            "hasLogo" to (data.logoData != null),
            "bankAccountNumber" to escapeHtml(data.companySettings.bankSettings?.bankAccountNumber ?: ""),
            "bankName" to escapeHtml(data.companySettings.bankSettings?.bankName ?: ""),
            "notes" to escapeHtml(data.document.notes ?: "")
        )
    }

    /**
     * Simplified conditional handling without complex regex
     */
    private fun handleSimpleConditionals(html: String, variables: Map<String, Any>): String {
        var processedHtml = html

        // Handle seller signature conditionals
        if (processedHtml.contains("{{#if seller_signature}}")) {
            val hasSellerSignature = variables["hasSellerSignature"] as? Boolean ?: false

            // Find and replace the conditional block
            val startMarker = "{{#if seller_signature}}"
            val elseMarker = "{{else}}"
            val endMarker = "{{/if}}"

            val startIndex = processedHtml.indexOf(startMarker)
            if (startIndex != -1) {
                val elseIndex = processedHtml.indexOf(elseMarker, startIndex)
                val endIndex = processedHtml.indexOf(endMarker, startIndex)

                if (elseIndex != -1 && endIndex != -1) {
                    val beforeBlock = processedHtml.substring(0, startIndex)
                    val afterBlock = processedHtml.substring(endIndex + endMarker.length)

                    val replacement = if (hasSellerSignature) {
                        // Get content between #if and else
                        processedHtml.substring(startIndex + startMarker.length, elseIndex)
                    } else {
                        // Get content between else and /if
                        processedHtml.substring(elseIndex + elseMarker.length, endIndex)
                    }

                    processedHtml = beforeBlock + replacement + afterBlock
                }
            }
        }

        // Handle client signature conditionals
        if (processedHtml.contains("{{#if client_signature}}")) {
            val hasClientSignature = variables["hasClientSignature"] as? Boolean ?: false

            val startMarker = "{{#if client_signature}}"
            val elseMarker = "{{else}}"
            val endMarker = "{{/if}}"

            val startIndex = processedHtml.indexOf(startMarker)
            if (startIndex != -1) {
                val elseIndex = processedHtml.indexOf(elseMarker, startIndex)
                val endIndex = processedHtml.indexOf(endMarker, startIndex)

                if (elseIndex != -1 && endIndex != -1) {
                    val beforeBlock = processedHtml.substring(0, startIndex)
                    val afterBlock = processedHtml.substring(endIndex + endMarker.length)

                    val replacement = if (hasClientSignature) {
                        processedHtml.substring(startIndex + startMarker.length, elseIndex)
                    } else {
                        processedHtml.substring(elseIndex + elseMarker.length, endIndex)
                    }

                    processedHtml = beforeBlock + replacement + afterBlock
                }
            }
        }

        return processedHtml
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