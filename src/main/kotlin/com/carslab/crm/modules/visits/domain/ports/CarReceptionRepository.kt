package com.carslab.crm.modules.visits.domain.ports

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.view.protocol.ProtocolView
import java.time.LocalDateTime

interface CarReceptionRepository {

    fun save(protocol: CreateProtocolRootModel): ProtocolId

    fun save(protocol: CarReceptionProtocol): CarReceptionProtocol
    
    fun findById(id: ProtocolId): ProtocolView?
    
    fun findAll(): List<CarReceptionProtocol>
    
    fun findByStatus(status: ProtocolStatus): List<CarReceptionProtocol>
    
    fun findByClientName(clientName: String): List<CarReceptionProtocol>
    
    fun findByLicensePlate(licensePlate: String): List<CarReceptionProtocol>
    
    fun deleteById(id: ProtocolId): Boolean

    fun searchProtocols(
        clientName: String?,
        clientId: Long?,
        licensePlate: String?,
        status: ProtocolStatus?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): List<ProtocolView>

    fun searchProtocolsWithPagination(
        clientName: String?,
        clientId: Long?,
        licensePlate: String?,
        make: String?,
        status: ProtocolStatus?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        page: Int = 0,
        size: Int = 10
    ): Pair<List<ProtocolView>, Long>

    fun countProtocolsByStatus(status: ProtocolStatus): Int
}