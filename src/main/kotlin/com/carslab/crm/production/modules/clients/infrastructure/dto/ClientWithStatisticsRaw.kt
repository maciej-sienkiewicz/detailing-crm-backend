package com.carslab.crm.production.modules.clients.infrastructure.dto

import java.math.BigDecimal
import java.time.LocalDateTime

interface ClientWithStatisticsRaw {
    fun getClientId(): Long
    fun getClientCompanyId(): Long
    fun getClientFirstName(): String
    fun getClientLastName(): String
    fun getClientEmail(): String
    fun getClientPhone(): String
    fun getClientAddress(): String?
    fun getClientCompany(): String?
    fun getClientTaxId(): String?
    fun getClientNotes(): String?
    fun getClientCreatedAt(): LocalDateTime
    fun getClientUpdatedAt(): LocalDateTime
    fun getClientVersion(): Long
    fun getClientActive(): Boolean

    fun getStatsClientId(): Long?
    fun getStatsVisitCount(): Long?
    fun getStatsTotalRevenue(): BigDecimal?
    fun getStatsVehicleCount(): Long?
    fun getStatsLastVisitDate(): LocalDateTime?
    fun getStatsUpdatedAt(): LocalDateTime?
}