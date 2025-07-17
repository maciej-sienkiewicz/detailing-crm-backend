package com.carslab.crm.modules.invoice_templates.infrastructure.rendering

import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceGenerationData
import com.carslab.crm.modules.invoice_templates.domain.ports.TemplateRenderingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.StringTemplateResolver
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class ThymeleafTemplateRenderingService : TemplateRenderingService {
    private val logger = LoggerFactory.getLogger(ThymeleafTemplateRenderingService::class.java)

    private val templateEngine: TemplateEngine = TemplateEngine().apply {
        setTemplateResolver(StringTemplateResolver().apply {
            templateMode = TemplateMode.HTML
            // Usunięto cacheable - nie jest dostępne publicznie w StringTemplateResolver
        })
    }

    override fun renderTemplate(template: InvoiceTemplate, data: InvoiceGenerationData): String {
        try {
            val context = Context(Locale.forLanguageTag("pl"))

            // Ustawienie podstawowych zmiennych
            context.setVariable("invoice", data.document)
            context.setVariable("company", data.companySettings)
            context.setVariable("items", data.document.items)

            // Formatowanie dat
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            context.setVariable("issuedDate", data.document.issuedDate.format(dateFormatter))
            context.setVariable("dueDate", data.document.dueDate?.format(dateFormatter))

            // Formatowanie kwot
            context.setVariable("totalNetFormatted", formatMoney(data.document.totalNet))
            context.setVariable("totalTaxFormatted", formatMoney(data.document.totalTax))
            context.setVariable("totalGrossFormatted", formatMoney(data.document.totalGross))

            // Logo
            data.logoData?.let { logoBytes ->
                val logoBase64 = Base64.getEncoder().encodeToString(logoBytes)
                context.setVariable("logoBase64", logoBase64)
                context.setVariable("hasLogo", true)
            } ?: context.setVariable("hasLogo", false)

            // Data generowania
            context.setVariable("generatedAt", java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            ))

            // Dane sprzedawcy
            context.setVariable("sellerName", data.companySettings.basicInfo.companyName)
            context.setVariable("sellerAddress", data.companySettings.basicInfo.address)
            context.setVariable("sellerTaxId", data.companySettings.basicInfo.taxId)
            context.setVariable("sellerPhone", data.companySettings.basicInfo.phone)
            context.setVariable("sellerWebsite", data.companySettings.basicInfo.website)

            // Dane nabywcy
            context.setVariable("buyerName", data.document.buyerName)
            context.setVariable("buyerAddress", data.document.buyerAddress)
            context.setVariable("buyerTaxId", data.document.buyerTaxId)

            // Płatność
            context.setVariable("paymentMethod", formatPaymentMethod(data.document.paymentMethod))
            context.setVariable("paymentStatus", formatPaymentStatus(data.document.status))

            // Formatowanie pozycji
            val formattedItems = data.document.items.map { item ->
                mapOf(
                    "name" to item.name,
                    "description" to item.description,
                    "quantity" to item.quantity.toString(),
                    "unitPrice" to formatMoney(item.unitPrice),
                    "taxRate" to "${item.taxRate}%",
                    "totalNet" to formatMoney(item.totalNet),
                    "totalGross" to formatMoney(item.totalGross)
                )
            }
            context.setVariable("formattedItems", formattedItems)

            // Podsumowanie VAT
            val vatSummary = calculateVatSummary(data.document.items)
            context.setVariable("vatSummary", vatSummary)

            // Dodatkowe dane
            data.additionalData.forEach { (key, value) ->
                context.setVariable(key, value)
            }

            // Renderowanie szablonu
            val fullTemplate = buildFullTemplate(template)
            return templateEngine.process(fullTemplate, context)

        } catch (e: Exception) {
            logger.error("Error rendering template: {}", template.name, e)
            throw RuntimeException("Failed to render invoice template", e)
        }
    }

    override fun validateTemplateSyntax(htmlTemplate: String): Boolean {
        try {
            val context = Context()
            // Poprawka: używamy mapy zamiast odwołania do nieistniejącej zmiennej
            context.setVariable("invoice", mapOf("number" to "TEST/001"))
            context.setVariable("company", mapOf("name" to "Test Company"))

            templateEngine.process(htmlTemplate, context)
            return true
        } catch (e: Exception) {
            logger.warn("Template validation failed: {}", e.message)
            return false
        }
    }

    private fun buildFullTemplate(template: InvoiceTemplate): String {
        return """
            <!DOCTYPE html>
            <html xmlns:th="http://www.thymeleaf.org">
            <head>
                <meta charset="UTF-8">
                <title>Faktura [[${'$'}{invoice.number}]]</title>
                <style>
                    ${template.content.cssStyles}
                </style>
            </head>
            <body>
                ${template.content.htmlTemplate}
            </body>
            </html>
        """.trimIndent()
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

    private fun calculateVatSummary(items: List<com.carslab.crm.domain.model.view.finance.DocumentItem>): List<Map<String, Any>> {
        return items.groupBy { it.taxRate }
            .map { (taxRate, groupedItems) ->
                val netSum = groupedItems.sumOf { it.totalNet }
                val taxSum = groupedItems.sumOf { it.totalGross - it.totalNet }
                val grossSum = groupedItems.sumOf { it.totalGross }

                mapOf(
                    "taxRate" to "${taxRate}%",
                    "netSum" to formatMoney(netSum),
                    "taxSum" to formatMoney(taxSum),
                    "grossSum" to formatMoney(grossSum)
                )
            }
    }
}