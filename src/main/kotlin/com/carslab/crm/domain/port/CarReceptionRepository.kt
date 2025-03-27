package com.carslab.crm.domain.port

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ContactAttempt
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus

/**
 * Port repozytorium dla protokołów przyjęcia pojazdów.
 * Ta definicja jest częścią warstwy domenowej i definiuje operacje,
 * które muszą być implementowane przez konkretne repozytoria.
 */
interface CarReceptionRepository {
    /**
     * Zapisuje nowy protokół lub aktualizuje istniejący.
     */
    fun save(protocol: CarReceptionProtocol): CarReceptionProtocol

    /**
     * Znajduje protokół po jego identyfikatorze.
     */
    fun findById(id: ProtocolId): CarReceptionProtocol?

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
}