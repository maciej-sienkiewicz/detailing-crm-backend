package com.carslab.crm.production.modules.vehicles.domain.repository

import com.carslab.crm.production.modules.vehicles.domain.model.*

interface VehicleAnalyticsRepository {

    fun getProfitabilityAnalysis(vehicleId: VehicleId, companyId: Long): VehicleProfitabilityAnalysis?

    fun getVisitPattern(vehicleId: VehicleId, companyId: Long): VehicleVisitPattern?

    fun getServicePreferences(vehicleId: VehicleId, companyId: Long): VehicleServicePreferences?

    fun getBatchProfitabilityAnalysis(vehicleIds: List<VehicleId>, companyId: Long): Map<VehicleId, VehicleProfitabilityAnalysis>

    fun getBatchVisitPatterns(vehicleIds: List<VehicleId>, companyId: Long): Map<VehicleId, VehicleVisitPattern>

    fun getBatchServicePreferences(vehicleIds: List<VehicleId>, companyId: Long): Map<VehicleId, VehicleServicePreferences>

    fun recalculateAnalytics(vehicleId: VehicleId, companyId: Long)
}