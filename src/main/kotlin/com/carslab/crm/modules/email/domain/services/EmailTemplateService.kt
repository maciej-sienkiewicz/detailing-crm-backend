package com.carslab.crm.modules.email.domain.services

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.modules.email.domain.model.ProtocolEmailData
import com.carslab.crm.production.modules.templates.application.service.query.TemplateQueryService
import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class EmailTemplateService(
    private val templateQueryService: TemplateQueryService,
    private val securityContext: SecurityContext,
    private val universalStorageService: UniversalStorageService,
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }

    fun generateProtocolEmail(
        protocolData: ProtocolEmailData,
        companyName: String,
        companyEmail: String,
        additionalVariables: Map<String, String> = emptyMap()
    ): String {
        val companyId = securityContext.getCurrentCompanyId()
        var content: String = templateQueryService.findActiveTemplateByTemplateType(TemplateType.MAIL_ON_VISIT_STARTED, companyId)
            ?.let { universalStorageService.retrieveFile(it.id) }
            ?.let { String(it, Charsets.UTF_8) }
            ?: throw IllegalStateException("No active email template found for company: $companyId")

        val formattedServicePeriod = formatServicePeriod(protocolData.servicePeriod)

        val variables = mutableMapOf(
            "COMPANY_NAME" to companyName,
            "CLIENT_NAME" to protocolData.clientName,
            "VEHICLE_MAKE" to protocolData.vehicleMake,
            "VEHICLE_MODEL" to protocolData.vehicleModel,
            "LICENSE_PLATE" to protocolData.licensePlate,
            "SERVICE_PERIOD" to formattedServicePeriod,
            "COMPANY_EMAIL" to companyEmail
        )

        variables.putAll(additionalVariables)

        variables.forEach { (key, value) ->
            content = content.replace("{{$key}}", value)
        }

        content = handleConditionalBlocks(content, variables)

        return content
    }

    fun generateSubject(protocolData: ProtocolEmailData): String {
        return "Protokół przyjęcia pojazdu ${protocolData.vehicleMake} ${protocolData.vehicleModel} • ${protocolData.licensePlate}"
    }

    private fun formatServicePeriod(servicePeriod: String): String {
        try {
            val parts = servicePeriod.split(" - ")

            if (parts.size == 2) {
                val startDateTime = LocalDateTime.parse(parts[0])
                val endDateTime = LocalDateTime.parse(parts[1])

                val startFormatted = startDateTime.format(DATETIME_FORMATTER)
                val endFormatted = endDateTime.format(DATE_FORMATTER)

                return "$startFormatted - $endFormatted"
            }
        } catch (e: Exception) {
        }

        return servicePeriod
    }

    private fun handleConditionalBlocks(content: String, variables: Map<String, String>): String {
        var result = content

        val conditionalRegex = Regex("\\{\\{#if (\\w+)\\}\\}(.*?)\\{\\{/if\\}\\}", RegexOption.DOT_MATCHES_ALL)

        result = conditionalRegex.replace(result) { matchResult ->
            val variableName = matchResult.groupValues[1]
            val blockContent = matchResult.groupValues[2]
            val variableValue = variables[variableName]

            if (!variableValue.isNullOrBlank()) {
                blockContent
            } else {
                ""
            }
        }

        return result
    }
}