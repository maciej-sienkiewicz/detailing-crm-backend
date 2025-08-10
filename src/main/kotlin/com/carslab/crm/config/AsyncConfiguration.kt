package com.carslab.crm.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfiguration {

    @Bean(name = ["employeeEventExecutor"])
    fun employeeEventExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 10
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("EmployeeEvent-")
        executor.setRejectedExecutionHandler { runnable, _ ->
            runnable.run()
        }
        executor.initialize()
        return executor
    }

    @Bean(name = ["salaryProcessingExecutor"])
    fun salaryProcessingExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 1
        executor.maxPoolSize = 5
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("SalaryProcess-")
        executor.setRejectedExecutionHandler { runnable, _ ->
            runnable.run()
        }
        executor.initialize()
        return executor
    }
}