package com.carslab.crm.infrastructure.config

import com.carslab.crm.modules.clients.domain.port.*
import com.carslab.crm.modules.clients.infrastructure.persistence.adapter.*
import com.carslab.crm.modules.company_settings.domain.port.CompanySettingsRepository
import com.carslab.crm.modules.company_settings.infrastructure.persistence.adapter.CompanySettingsRepositoryAdapter
import com.carslab.crm.domain.port.*
import com.carslab.crm.infrastructure.persistence.adapter.*
import com.carslab.crm.infrastructure.persistence.repository.JpaCalendarColorRepositoryAdapter
import com.carslab.crm.infrastructure.storage.FileImageStorageService
import com.carslab.crm.modules.visits.domain.ports.CarReceptionRepository
import com.carslab.crm.modules.visits.domain.ports.ProtocolCommentsRepository
import com.carslab.crm.modules.visits.domain.ports.ProtocolServicesRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = [
    "com.carslab.crm.infrastructure.persistence.repository",
    "com.carslab.crm.audit.repository",
    "com.carslab.crm.signature.infrastructure.persistance.repository",
    "com.carslab.crm.modules.clients.infrastructure.persistence.repository",
    "com.carslab.crm.finances.infrastructure.repository",
    "com.carslab.crm.modules.finances.infrastructure.repository",
    "com.carslab.crm.modules.company_settings.infrastructure.persistence.repository",
    "com.carslab.crm.infrastructure.storage.repository",
    "com.carslab.crm.infrastructure.backup.googledrive.repository",
    "com.carslab.crm.modules.visits.infrastructure.persistence.repository",
    "com.carslab.crm.modules.visits.infrastructure.persistence.read",
    "com.carslab.crm.modules.email.infrastructure.persistence",
    "com.carslab.crm.modules.activities.infrastructure.persistence",
    "com.carslab.crm.modules.employees.infrastructure.persistence",
    "com.carslab.crm.modules.invoice_templates.infrastructure.persistence",
    "com.carslab.crm.production.modules.companysettings.infrastructure.repository",
    "com.carslab.crm.production.modules.invoice_templates.infrastructure.repository",
    "com.carslab.crm.production.modules.services.infrastructure.repository",
    "com.carslab.crm.production.modules.activities.infrastructure.repository"
])
class PersistenceConfig {

    @Bean
    @Primary
    fun clientRepository(adapter: ClientRepositoryAdapter): ClientRepositoryDeprecated = adapter

    @Bean
    @Primary
    fun vehicleRepository(adapter: VehicleRepositoryDeprecatedAdapter): VehicleRepositoryDeprecated = adapter

    @Bean
    @Primary
    fun carReceptionRepository(adapter: JpaCarReceptionRepositoryAdapter): CarReceptionRepository = adapter

    @Bean
    @Primary
    fun serviceHistoryRepository(adapter: JpaServiceHistoryRepositoryAdapter): ServiceHistoryRepository = adapter

    @Bean
    @Primary
    fun contactAttemptRepository(adapter: JpaContactAttemptRepositoryAdapter): ContactAttemptRepository = adapter

    @Bean
    @Primary
    fun protocolCommentsRepository(adapter: JpaProtocolCommentsRepositoryAdapter): ProtocolCommentsRepository = adapter

    @Bean
    @Primary
    fun clientStatisticsRepository(adapter: ClientStatisticsRepositoryDeprecatedAdapter): ClientStatisticsRepositoryDeprecated = adapter

    @Bean
    @Primary
    fun vehicleStatisticsRepository(adapter: VehicleStatisticsRepositoryAdapter): VehicleStatisticsRepositoryDeprecated = adapter

    @Bean
    @Primary
    fun protocolServicesRepository(adapter: JpaProtocolServicesRepositoryAdapter): ProtocolServicesRepository = adapter

    @Bean
    @Primary
    fun clientVehicleRepository(adapter: ClientVehicleAssociationRepositoryDeprecatedAdapter): ClientVehicleAssociationRepositoryDeprecated = adapter

    @Bean
    @Primary
    fun imageStorageService(service: FileImageStorageService): FileImageStorageService = service

    @Bean
    @Primary
    fun calendarColorRepository(adapter: JpaCalendarColorRepositoryAdapter): CalendarColorRepository = adapter

    // NEW REPOSITORIES FOR VEHICLE TABLE AND STATISTICS
    @Bean
    @Primary
    fun vehicleTableRepository(adapter: VehicleTableRepositoryAdapter): VehicleTableRepository = adapter

    @Bean
    @Primary
    fun vehicleCompanyStatisticsRepository(adapter: VehicleCompanyStatisticsRepositoryAdapter): VehicleCompanyStatisticsRepository = adapter

    @Bean
    @Primary
    fun companySettingsRepository(adapter: CompanySettingsRepositoryAdapter): CompanySettingsRepository = adapter
}