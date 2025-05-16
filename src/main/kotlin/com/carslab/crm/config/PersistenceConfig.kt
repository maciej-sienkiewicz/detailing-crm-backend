package com.carslab.crm.infrastructure.config

import com.carslab.crm.domain.port.*
import com.carslab.crm.infrastructure.persistence.adapter.*
import com.carslab.crm.infrastructure.persistence.repository.JpaCalendarColorRepositoryAdapter
import com.carslab.crm.infrastructure.persistence.repository.UserRepository
import com.carslab.crm.infrastructure.storage.FileImageStorageService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = ["com.carslab.crm.infrastructure.persistence.repository"])
class PersistenceConfig {

    @Bean
    @Primary
    fun clientRepository(adapter: JpaClientRepositoryAdapter): ClientRepository = adapter

    @Bean
    @Primary
    fun vehicleRepository(adapter: JpaVehicleRepositoryAdapter): VehicleRepository = adapter

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
    fun clientStatisticsRepository(adapter: JpaClientStatisticsRepositoryAdapter): ClientStatisticsRepository = adapter

    @Bean
    @Primary
    fun vehicleStatisticsRepository(adapter: JpaVehicleStatisticsRepositoryAdapter): VehicleStatisticsRepository = adapter

    @Bean
    @Primary
    fun protocolServicesRepository(adapter: JpaProtocolServicesRepositoryAdapter): ProtocolServicesRepository = adapter

    @Bean
    @Primary
    fun clientVehicleRepository(adapter: JpaClientVehicleRepositoryAdapter): ClientVehicleRepository = adapter

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