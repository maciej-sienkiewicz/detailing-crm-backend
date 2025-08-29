package com.carslab.crm.infrastructure.config

import com.carslab.crm.domain.port.*
import com.carslab.crm.infrastructure.persistence.repository.JpaCalendarColorRepositoryAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = [
    "com.carslab.crm.infrastructure.persistence.repository",
    "com.carslab.crm.audit.repository",
    "com.carslab.crm.signature.infrastructure.persistance.repository",
    "com.carslab.crm.finances.infrastructure.repository",
    "com.carslab.crm.modules.finances.infrastructure.repository",
    "com.carslab.crm.modules.company_settings.infrastructure.persistence.repository",
    "com.carslab.crm.infrastructure.storage.repository",
    "com.carslab.crm.infrastructure.backup.googledrive.repository",
    "com.carslab.crm.modules.email.infrastructure.persistence",
    "com.carslab.crm.modules.employees.infrastructure.persistence",
    "com.carslab.crm.modules.invoice_templates.infrastructure.persistence",
    "com.carslab.crm.production.modules.companysettings.infrastructure.repository",
    "com.carslab.crm.production.modules.invoice_templates.infrastructure.repository",
    "com.carslab.crm.production.modules.services.infrastructure.repository",
    "com.carslab.crm.production.modules.activities.infrastructure.repository",
    "com.carslab.crm.production.modules.clients.infrastructure.repository",
    "com.carslab.crm.production.modules.vehicles.infrastructure.repository",
    "com.carslab.crm.production.modules.associations.infrastructure.repository",
    "com.carslab.crm.production.modules.visits.infrastructure.repository",
    "com.carslab.crm.production.modules.stats.infrastructure.repository",
    "com.carslab.crm.production.modules.templates.infrastructure.repository"
])
class PersistenceConfig {

    @Bean
    @Primary
    fun calendarColorRepository(adapter: JpaCalendarColorRepositoryAdapter): CalendarColorRepository = adapter

}
