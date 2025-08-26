package com.carslab.crm.production.modules.visits.domain.factory

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.command.CreateVisitCommand
import com.carslab.crm.production.modules.visits.domain.command.UpdateVisitCommand
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitDocuments
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitPeriod
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class VisitFactory(
    private val visitServiceFactory: VisitServiceFactory
) {

    fun createVisit(command: CreateVisitCommand): Visit {
        return Visit(
            id = null,
            companyId = command.companyId,
            title = command.title.trim(),
            clientId = ClientId(command.client.id.toLong()),
            vehicleId = VehicleId(command.vehicle.id.toLong()),
            period = VisitPeriod(command.startDate, command.endDate),
            status = command.status,
            services = command.services.map { visitServiceFactory.createService(it) },
            documents = VisitDocuments(command.keysProvided, command.documentsProvided),
            notes = command.notes?.trim(),
            referralSource = command.referralSource,
            appointmentId = command.appointmentId,
            calendarColorId = command.calendarColorId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            deliveryPerson = command.deliveryPerson
        )
    }

    fun updateVisit(existingVisit: Visit, command: UpdateVisitCommand): Visit {
        return existingVisit.copy(
            title = command.title.trim(),
            period = VisitPeriod(command.startDate, command.endDate),
            services = command.services.map { visitServiceFactory.updateService(it) },
            documents = VisitDocuments(command.keysProvided, command.documentsProvided),
            notes = command.notes?.trim(),
            referralSource = command.referralSource,
            appointmentId = command.appointmentId,
            calendarColorId = command.calendarColorId,
            updatedAt = LocalDateTime.now(),
            status = command.status,
            deliveryPerson = command.deliveryPerson
        )
    }
}