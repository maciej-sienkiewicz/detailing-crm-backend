package com.carslab.crm.production.modules.visits.application.converter

import com.carslab.crm.production.modules.visits.application.dto.VisitListFilterRequest
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class VisitFilterConverter {

    fun convertFromRequestParams(
        clientName: String?,
        licensePlate: String?,
        status: String?,
        startDate: String?,
        endDate: String?,
        make: String?,
        model: String?,
        serviceName: String?,
        serviceIds: List<String>?,
        title: String?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?
    ): VisitListFilterRequest {
        return VisitListFilterRequest(
            clientName = clientName?.takeIf { it.isNotBlank() },
            licensePlate = licensePlate?.takeIf { it.isNotBlank() },
            status = status?.takeIf { it.isNotBlank() }?.let { parseVisitStatus(it) },
            startDate = startDate?.takeIf { it.isNotBlank() }?.let { parseDateTime(it) },
            endDate = endDate?.takeIf { it.isNotBlank() }?.let { parseDateTime(it) },
            make = make?.takeIf { it.isNotBlank() },
            model = model?.takeIf { it.isNotBlank() },
            serviceName = serviceName?.takeIf { it.isNotBlank() },
            serviceIds = serviceIds?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
            title = title?.takeIf { it.isNotBlank() },
            minPrice = minPrice,
            maxPrice = maxPrice
        )
    }

    private fun parseVisitStatus(status: String): VisitStatus? {
        return try {
            VisitStatus.valueOf(status.uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun parseDateTime(dateTime: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: Exception) {
            null
        }
    }
}