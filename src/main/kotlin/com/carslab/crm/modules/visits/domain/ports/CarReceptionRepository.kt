package com.carslab.crm.modules.visits.domain.ports

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.view.protocol.ProtocolView
import java.time.LocalDateTime

/**
 * Port repozytorium dla protokołów przyjęcia pojazdów.
 * Ta definicja jest częścią warstwy domenowej i definiuje operacje,
 * które muszą być implementowane przez konkretne repozytoria.
 */
interface CarReceptionRepository {
    /**
     * Zapisuje nowy protokół lub aktualizuje istniejący.
     */
    fun save(protocol: CreateProtocolRootModel): ProtocolId

    fun save(protocol: CarReceptionProtocol): CarReceptionProtocol

    /**
     * Znajduje protokół po jego identyfikatorze.
     */
    fun findById(id: ProtocolId): ProtocolView?

    /**
     * Znajduje wszystkie protokoły.
     */
    fun findAll(): List<CarReceptionProtocol>

    /**
     * Znajduje protokoły po statusie.
     */
    fun findByStatus(status: ProtocolStatus): List<CarReceptionProtocol>

    /**
     * Znajduje protokoły dla danego klienta.
     */
    fun findByClientName(clientName: String): List<CarReceptionProtocol>

    /**
     * Znajduje protokoły dla danego pojazdu (numer rejestracyjny).
     */
    fun findByLicensePlate(licensePlate: String): List<CarReceptionProtocol>

    /**
     * Usuwa protokół o podanym identyfikatorze.
     */
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