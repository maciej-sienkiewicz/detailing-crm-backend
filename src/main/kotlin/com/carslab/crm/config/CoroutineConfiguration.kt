package com.carslab.crm.config

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfiguration {

    @Bean
    fun backgroundTaskScope(): CoroutineScope {
        return CoroutineScope(
            SupervisorJob() + // Błąd w jednej coroutine nie anuluje innych
                    Dispatchers.IO + // I/O operations
                    CoroutineName("BackgroundTasks")
        )
    }

    @PreDestroy
    fun cleanup() {
        backgroundTaskScope().cancel() // Proper cleanup
    }
}