// src/main/kotlin/com/carslab/crm/infrastructure/cqrs/CqrsConfiguration.kt
package com.carslab.crm.infrastructure.cqrs

import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CqrsConfiguration {

    @Bean
    fun commandBus(applicationContext: ApplicationContext): CommandBus {
        return SimpleCommandBus(applicationContext)
    }

    @Bean
    fun queryBus(applicationContext: ApplicationContext): QueryBus {
        return SimpleQueryBus(applicationContext)
    }
}