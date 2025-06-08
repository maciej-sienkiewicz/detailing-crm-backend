package com.carslab.crm.modules.company_settings.domain

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.create.protocol.CreateMediaTypeModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.domain.model.create.protocol.VehicleReleaseDetailsModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.time.LocalDateTime

@Service
class CarReceptionFacade(
    private val carReceptionService: CarReceptionService,
) {
    private val logger = LoggerFactory.getLogger(CarReceptionFacade::class.java)

    fun createProtocol(protocol: CreateProtocolRootModel): ProtocolId {
        return carReceptionService.createProtocol(protocol)
    }

    fun deleteImage(protocolId: ProtocolId, imageId: String) {
        return carReceptionService.deleteImage(protocolId, imageId)
    }

    fun updateProtocol(protocol: CarReceptionProtocol): CarReceptionProtocol {
        return carReceptionService.updateProtocol(protocol)
    }

    fun updateServices(protocolId: ProtocolId, services: List<CreateServiceModel>) {
        carReceptionService.updateServices(protocolId, services)
    }

    fun changeStatus(protocolId: ProtocolId, newStatus: ProtocolStatus): CarReceptionProtocol {
        return carReceptionService.changeStatus(protocolId, newStatus)
    }

    fun getProtocolById(protocolId: ProtocolId): CarReceptionProtocol? {
        return carReceptionService.getProtocolById(protocolId)
    }

    fun getAllProtocols(): List<CarReceptionProtocol> {
        return carReceptionService.getAllProtocols()
    }

    fun searchProtocols(
        clientName: String? = null,
        clientId: Long? = null,
        licensePlate: String? = null,
        status: ProtocolStatus? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): List<CarReceptionProtocol> {
        return carReceptionService
            .searchProtocols(
                clientName = clientName,
                clientId = clientId,
                licensePlate = licensePlate,
                status = status,
                startDate = startDate,
                endDate = endDate
            )
    }

    fun deleteProtocol(protocolId: ProtocolId): Boolean {
        return carReceptionService.deleteProtocol(protocolId)
    }

    fun storeUploadedImage(
        request: MultipartHttpServletRequest,
        protocolId: ProtocolId,
        image: CreateMediaTypeModel
    ) =
        carReceptionService.storeUploadedImage(request, protocolId, image)

    fun releaseVehicle(existingProtocol: CarReceptionProtocol, releaseDetails: VehicleReleaseDetailsModel): CarReceptionProtocol {
        return carReceptionService.releaseVehicle(
            existingProtocol = existingProtocol,
            releaseDetails = releaseDetails
        )
    }

    fun searchProtocolsWithPagination(
        clientName: String? = null,
        clientId: Long? = null,
        licensePlate: String? = null,
        make: String? = null,
        status: ProtocolStatus? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        page: Int = 0,
        size: Int = 10
    ): PaginatedResponse<CarReceptionProtocol> {
        return carReceptionService.searchProtocolsWithPagination(
            clientName = clientName,
            clientId = clientId,
            licensePlate = licensePlate,
            make = make,
            status = status,
            startDate = startDate,
            endDate = endDate,
            page = page,
            size = size
        )
    }

    fun countProtocolsByStatus(status: ProtocolStatus): Int {
        return carReceptionService.countProtocolsByStatus(status)
    }
}