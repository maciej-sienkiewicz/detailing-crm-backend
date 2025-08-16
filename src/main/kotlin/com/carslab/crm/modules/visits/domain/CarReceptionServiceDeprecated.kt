package com.carslab.crm.modules.visits.domain

import com.carslab.crm.modules.clients.domain.*
import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.view.protocol.ProtocolView
import com.carslab.crm.modules.visits.domain.ports.CarReceptionRepository
import com.carslab.crm.modules.visits.domain.ports.ProtocolServicesRepository
import com.carslab.crm.infrastructure.storage.FileImageStorageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CarReceptionServiceDeprecated(
    private val carReceptionRepository: CarReceptionRepository,
    private val clientApplicationService: ClientApplicationService,
    private val vehicleApplicationServiceDeprecated: VehicleApplicationServiceDeprecated,
    private val protocolServicesRepository: ProtocolServicesRepository,
    private val imageStorageService: FileImageStorageService,
) {
    private val logger = LoggerFactory.getLogger(CarReceptionServiceDeprecated::class.java)

    /**
         * logger.info("This is the new way of creating protocols")
         *
         * val clientId = clientApplicationService.getClientById(createProtocolCommand.client.id.toLong())
         *  .getOrElse { clientApplicationService.createClient(createProtocolCommand.client.toCreateClientRequest()) }
         *
         *  val vehicleId = vehicleApplicationService.getVehicleById(createProtocolCommand.vehicle.id.toLong())
         *  .getOrElse { vehicleApplicationService.createVehicle(createProtocolCommand.vehicle.toCreateVehicleRequest()) }
         *
         *  val savedProtocol = carReceptionRepository.save(createProtocolCommand.toCarReceptionProtocol(clientId, vehicleId))
         *
         *  eventEmitter.emit(NEW_VISIT_CREATED, savedProtocol)
         *  logger.info("Created protocol with ID: ${savedProtocolId.value}")
         *  return savedProtocolId
         */

    fun getProtocolById(protocolId: ProtocolId): CarReceptionProtocol? {
        logger.debug("Getting protocol by ID: ${protocolId.value}")
        return carReceptionRepository.findById(protocolId)?.enrichProtocol()
    }

    private fun ProtocolView.enrichProtocol(): CarReceptionProtocol {
        val vehicleDetail = vehicleApplicationServiceDeprecated.getVehicleById(vehicleId.value)
        val clientDetail = clientApplicationService.getClientById(clientId.value)
        val services = protocolServicesRepository.findByProtocolId(id)
        val images = imageStorageService.getImagesByProtocol(id)

        return CarReceptionProtocol(
            id = id,
            title = title,
            vehicle = VehicleDetails(
                id = vehicleId,
                make = vehicleDetail?.make ?: "",
                model = vehicleDetail?.model ?: "",
                licensePlate = vehicleDetail?.licensePlate ?: "",
                productionYear = vehicleDetail?.year ?: 0,
                vin = vehicleDetail?.vin ?: "",
                color = vehicleDetail?.color ?: "",
                mileage = vehicleDetail?.mileage
            ),
            client = ClientDetails(
                id = clientId.value,
                name = clientDetail?.fullName ?: "",
                email = clientDetail?.email ?: "",
                phone = clientDetail?.phone ?: "",
                companyName = clientDetail?.company,
                taxId = clientDetail?.taxId,
                address = clientDetail?.address ?: "",
            ),
            period = period,
            status = status,
            protocolServices = services
                .map {
                    ProtocolService(
                        id = it.id.toString(),
                        name = it.name,
                        basePrice = it.basePrice,
                        discount = it.discount,
                        finalPrice = it.finalPrice,
                        approvalStatus = it.approvalStatus,
                        note = it.note,
                        quantity = it.quantity
                    )
                },
            notes = notes,
            referralSource = ReferralSource.SEARCH_ENGINE,
            otherSourceDetails = "",
            audit = AuditInfo(
                createdAt = createdAt,
                updatedAt = LocalDateTime.now(),
                statusUpdatedAt = LocalDateTime.now(),
                createdBy = LocalDateTime.now().toString(),
                updatedBy = LocalDateTime.now().toString(),
                appointmentId = ""
            ),
            mediaItems = images.map {
                MediaItem(
                    id = it.id,
                    type = MediaType.PHOTO,
                    name = it.name,
                    size = it.size,
                    description = "Zamockowany opis",
                    createdAt = LocalDateTime.now(),
                    tags = it.tags
                )
            },
            documents = Documents(keysProvided, documentsProvided),
            calendarColorId = calendarColorId,
        )
    }
}