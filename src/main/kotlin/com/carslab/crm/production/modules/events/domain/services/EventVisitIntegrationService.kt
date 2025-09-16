package com.carslab.crm.production.modules.events.domain.services

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
import com.carslab.crm.production.modules.events.application.dto.ConvertToVisitRequest
import com.carslab.crm.production.modules.events.application.service.command.EventOccurrenceCommandService
import com.carslab.crm.production.modules.events.domain.activity.EventActivitySender
import com.carslab.crm.production.modules.events.domain.models.value_objects.EventOccurrenceId
import com.carslab.crm.production.modules.events.domain.repositories.EventOccurrenceRepository
import com.carslab.crm.production.modules.events.domain.repositories.RecurringEventRepository
import com.carslab.crm.production.modules.vehicles.application.service.VehicleQueryService
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import com.carslab.crm.production.modules.visits.application.service.command.VisitCommandService
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EventVisitIntegrationService(
    private val eventOccurrenceCommandService: EventOccurrenceCommandService,
    private val visitCommandService: VisitCommandService,
    private val clientQueryService: ClientQueryService,
    private val vehicleQueryService: VehicleQueryService,
    private val eventOccurrenceRepository: EventOccurrenceRepository,
    private val recurringEventRepository: RecurringEventRepository,
    private val securityContext: SecurityContext,
    private val eventActivitySender: EventActivitySender
) {
    private val logger = LoggerFactory.getLogger(EventVisitIntegrationService::class.java)

    fun convertOccurrenceToVisit(
        occurrenceId: EventOccurrenceId,
        request: ConvertToVisitRequest
    ): com.carslab.crm.production.modules.visits.application.dto.VisitResponse {
        logger.info("Converting occurrence to visit: {}", occurrenceId.value)

        validateConversionData(request)

        val visitRequest = eventOccurrenceCommandService.prepareVisitCreation(occurrenceId, request)
        val enrichedVisitRequest = enrichVisitRequest(visitRequest, request)

        val visitResponse = visitCommandService.createVisit(enrichedVisitRequest)

        val visitId = com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId.of(visitResponse.id.toLong())
        val occurrenceResponse = eventOccurrenceCommandService.markOccurrenceAsConverted(occurrenceId, visitId)

        // Pobieramy dane dla aktywności
        val occurrence = eventOccurrenceRepository.findById(occurrenceId)
            ?: throw EntityNotFoundException("Event occurrence not found: ${occurrenceId.value}")

        val companyId = securityContext.getCurrentCompanyId()
        val recurringEvent = recurringEventRepository.findById(occurrence.recurringEventId, companyId)
            ?: throw EntityNotFoundException("Recurring event not found: ${occurrence.recurringEventId}")

        // Rejestrujemy aktywność konwersji
        eventActivitySender.onOccurrenceConvertedToVisit(occurrence, recurringEvent, visitResponse)

        logger.info("Occurrence converted to visit successfully: {} -> {}", occurrenceId.value, visitResponse.id)
        return visitResponse
    }

    private fun validateConversionData(request: ConvertToVisitRequest) {
        try {
            clientQueryService.getClient(request.clientId.toString())
        } catch (e: Exception) {
            throw BusinessException("Client not found: ${request.clientId}")
        }

        try {
            vehicleQueryService.getVehicle(request.vehicleId.toString())
        } catch (e: Exception) {
            throw BusinessException("Vehicle not found: ${request.vehicleId}")
        }
    }

    private fun enrichVisitRequest(
        visitRequest: CreateVisitRequest,
        conversionRequest: ConvertToVisitRequest
    ): CreateVisitRequest {
        val client = clientQueryService.getClient(conversionRequest.clientId.toString())
        val vehicle = vehicleQueryService.getVehicle(conversionRequest.vehicleId.toString())

        return visitRequest.copy(
            ownerName = client.client.fullName,
            email = client.client.email,
            phone = client.client.phone,
            address = client.client.address,
            companyName = client.client.company,
            taxId = client.client.taxId,
            licensePlate = vehicle.vehicle.licensePlate,
            make = vehicle.vehicle.make,
            model = vehicle.vehicle.model,
            productionYear = vehicle.vehicle.year,
            mileage = vehicle.vehicle.mileage,
            vin = vehicle.vehicle.vin,
            color = vehicle.vehicle.color
        )
    }
}