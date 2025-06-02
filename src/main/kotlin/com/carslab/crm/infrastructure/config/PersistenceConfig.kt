package com.carslab.crm.infrastructure.config

import com.carslab.crm.clients.domain.port.ClientRepository
import com.carslab.crm.clients.domain.port.ClientStatisticsRepository
import com.carslab.crm.clients.domain.port.ClientVehicleAssociationRepository
import com.carslab.crm.clients.domain.port.ClientVehicleRepository
import com.carslab.crm.clients.domain.port.VehicleRepository
import com.carslab.crm.clients.domain.port.VehicleStatisticsRepository
import com.carslab.crm.clients.infrastructure.persistence.adapter.ClientVehicleAssociationRepositoryAdapter
import com.carslab.crm.clients.infrastructure.persistence.adapter.VehicleRepositoryAdapter
import com.carslab.crm.clients.infrastructure.persistence.adapter.VehicleStatisticsRepositoryAdapter
import com.carslab.crm.domain.port.*
import com.carslab.crm.infrastructure.persistence.adapter.*
import com.carslab.crm.infrastructure.persistence.repository.JpaCalendarColorRepositoryAdapter
import com.carslab.crm.infrastructure.storage.FileImageStorageService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = ["com.carslab.crm.infrastructure.persistence.repository", "com.carslab.crm.audit.repository", "com.carslab.crm.signature.infrastructure.persistance.repository", "com.carslab.crm.clients.infrastructure.persistance"])
class PersistenceConfig {

    @Bean
    @Primary
    fun clientRepository(adapter: ClientRepositoryAdapter): ClientRepository = adapter

    @Bean
    @Primary
    fun vehicleRepository(adapter: VehicleRepositoryAdapter): VehicleRepository = adapter

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
    fun clientStatisticsRepository(adapter: ClientStatisticsRepositoryAdapter): ClientStatisticsRepository = adapter

    @Bean
    @Primary
    fun vehicleStatisticsRepository(adapter: VehicleStatisticsRepositoryAdapter): VehicleStatisticsRepository = adapter

    @Bean
    @Primary
    fun protocolServicesRepository(adapter: JpaProtocolServicesRepositoryAdapter): ProtocolServicesRepository = adapter

    @Bean
    @Primary
    fun clientVehicleRepository(adapter: ClientVehicleAssociationRepositoryAdapter): ClientVehicleAssociationRepository = adapter

    @Bean
    @Primary
    fun imageStorageService(service: FileImageStorageService): FileImageStorageService = service

    @Bean
    @Primary
    fun serviceRecipeRepository(adapter: JpaServiceRecipeRepositoryAdapter): ServiceRecipeRepository = adapter

    @Bean
    @Primary
    fun calendarColorRepository(adapter: JpaCalendarColorRepositoryAdapter): CalendarColorRepository = adapter
}