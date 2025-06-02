package com.carslab.crm.api.model.commands

import com.carslab.crm.clients.domain.model.ClientAudit
import com.carslab.crm.clients.domain.model.ClientId
import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.client.CreateClientModel
import com.carslab.crm.domain.model.stats.ClientStats
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Mapper dla modeli DTO związanych z klientami.
 */
object ClientCommandMapper {
    private val DATE_FORMATTER = DateTimeFormatter.ISO_DATE
    private val DATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME

    /**
     * Konwertuje komendę tworzenia klienta na model domenowy.
     */
    fun fromCreateCommand(command: CreateClientCommand): CreateClientModel {
        val now = LocalDateTime.now()

        return CreateClientModel(
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email,
            phone = command.phone,
            address = command.address,
            company = command.company,
            taxId = command.taxId,
            notes = command.notes,
            audit = ClientAudit(
                createdAt = now,
                updatedAt = now
            )
        )
    }

    /**
     * Konwertuje komendę aktualizacji klienta na model domenowy.
     */
    fun fromUpdateCommand(command: UpdateClientCommand, existingClient: ClientDetails? = null): ClientDetails {
        val now = LocalDateTime.now()

        // Zachowanie danych audytowych
        val createdAt = existingClient?.audit?.createdAt ?: now

        return ClientDetails(
            id = ClientId(command.id.toLong()),
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email,
            phone = command.phone,
            address = command.address,
            company = command.company,
            taxId = command.taxId,
            notes = command.notes,
            audit = ClientAudit(
                createdAt = createdAt,
                updatedAt = now
            )
        )
    }

    /**
     * Konwertuje model domenowy na podstawowe DTO klienta.
     */
    fun toDto(client: ClientDetails): ClientDto {
        return ClientDto(
            id = client.id.value.toString(),
            firstName = client.firstName,
            lastName = client.lastName,
            email = client.email,
            phone = client.phone,
            address = client.address,
            company = client.company,
            taxId = client.taxId,
            notes = client.notes,
            createdAt = client.audit.createdAt,
            updatedAt = client.audit.updatedAt
        )
    }

    /**
     * Konwertuje model domenowy klienta ze statystykami na rozszerzone DTO.
     */
    fun toExpandedDto(clientStats: com.carslab.crm.domain.model.ClientStats): ClientExpandedDto {
        return ClientExpandedDto(
            id = clientStats.client.id.value.toString(),
            firstName = clientStats.client.firstName,
            lastName = clientStats.client.lastName,
            email = clientStats.client.email,
            phone = clientStats.client.phone,
            address = clientStats.client.address,
            company = clientStats.client.company,
            taxId = clientStats.client.taxId,
            notes = clientStats.client.notes,
            createdAt = clientStats.client.audit.createdAt,
            updatedAt = clientStats.client.audit.updatedAt,
            totalVisits = clientStats.stats?.visitNo?.toInt() ?: 0,
            totalRevenue = clientStats.stats?.gmv?.toDouble() ?: 0.0,
            vehicles = clientStats.vehicles.map { "${it.make} ${it.model} (${it.year})" }
        )
    }

    /**
     * Konwertuje model domenowy klienta na rozszerzone DTO bez statystyk.
     */
    fun toExpandedDto(client: ClientDetails): ClientExpandedDto {
        return ClientExpandedDto(
            id = client.id.value.toString(),
            firstName = client.firstName,
            lastName = client.lastName,
            email = client.email,
            phone = client.phone,
            address = client.address,
            company = client.company,
            taxId = client.taxId,
            notes = client.notes,
            createdAt = client.audit.createdAt,
            updatedAt = client.audit.updatedAt,
            totalVisits = 0,
            totalRevenue = 0.0,
            vehicles = emptyList()
        )
    }

    /**
     * Konwertuje statystyki klienta na DTO statystyk.
     */
    fun toStatisticsDto(stats: ClientStats): ClientStatisticsDto {
        return ClientStatisticsDto(
            totalVisits = stats.visitNo,
            totalRevenue = stats.gmv,
            vehicleNo = stats.vehiclesNo
        )
    }

    /**
     * Konwertuje komendę tworzenia próby kontaktu na model domenowy.
     */
    fun fromCreateContactAttemptCommand(command: CreateContactAttemptCommand): ContactAttempt {
        val now = LocalDateTime.now()
        val date = LocalDateTime.parse(command.date, DATETIME_FORMATTER)

        return ContactAttempt(
            id = ContactAttemptId(),
            clientId = command.clientId,
            date = date,
            type = mapContactAttemptType(command.type),
            description = command.description,
            result = mapContactAttemptResult(command.result),
            audit = ContactAttemptAudit(
                createdAt = now,
                updatedAt = now
            )
        )
    }

    /**
     * Konwertuje komendę aktualizacji próby kontaktu na model domenowy.
     */
    fun fromUpdateContactAttemptCommand(command: UpdateContactAttemptCommand, existingAttempt: ContactAttempt? = null): ContactAttempt {
        val now = LocalDateTime.now()
        val date = LocalDateTime.parse(command.date, DATETIME_FORMATTER)

        // Zachowanie danych audytowych
        val createdAt = existingAttempt?.audit?.createdAt ?: now

        return ContactAttempt(
            id = ContactAttemptId(command.id),
            clientId = command.clientId,
            date = date,
            type = mapContactAttemptType(command.type),
            description = command.description,
            result = mapContactAttemptResult(command.result),
            audit = ContactAttemptAudit(
                createdAt = createdAt,
                updatedAt = now
            )
        )
    }

    /**
     * Konwertuje model domenowy próby kontaktu na DTO.
     */
    fun toContactAttemptDto(contactAttempt: ContactAttempt): ContactAttemptDto {
        return ContactAttemptDto(
            id = contactAttempt.id.value,
            clientId = contactAttempt.clientId,
            date = contactAttempt.date.format(DATETIME_FORMATTER),
            type = contactAttempt.type.name,
            description = contactAttempt.description,
            result = contactAttempt.result.name,
            createdAt = contactAttempt.audit.createdAt.format(DATETIME_FORMATTER),
            updatedAt = contactAttempt.audit.updatedAt.format(DATETIME_FORMATTER)
        )
    }

    // Metody pomocnicze

    private fun mapContactAttemptType(type: String): ContactAttemptType {
        return try {
            ContactAttemptType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid contact attempt type: $type")
        }
    }

    private fun mapContactAttemptResult(result: String): ContactAttemptResult {
        return try {
            ContactAttemptResult.valueOf(result.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid contact attempt result: $result")
        }
    }
}