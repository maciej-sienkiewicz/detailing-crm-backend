package com.carslab.crm.config

import com.carslab.crm.modules.schedulers.DailyAbandonedVisitsJob
import org.quartz.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.*
import javax.sql.DataSource

@Configuration
@EnableScheduling
class QuartzConfiguration {

    private val logger = LoggerFactory.getLogger(QuartzConfiguration::class.java)

    @Bean
    fun dailyAbandonedVisitsJobDetail(): JobDetail {
        logger.info("Creating dailyAbandonedVisitsJobDetail bean")
        return JobBuilder.newJob(DailyAbandonedVisitsJob::class.java)
            .withIdentity("dailyAbandonedVisitsJob", "cleanupJobs")
            .withDescription("Daily cleanup of abandoned visits from previous day")
            .storeDurably(true)
            .requestRecovery(true)
            .build()
    }

    @Bean
    fun dailyAbandonedVisitsTrigger(
        @Qualifier("dailyAbandonedVisitsJobDetail") jobDetail: JobDetail
    ): Trigger {
        logger.info("Creating dailyAbandonedVisitsTrigger bean")

        // Zacznij trigger za 2 minuty od teraz, żeby dać czas na pełne uruchomienie aplikacji
        val startTime = Date(System.currentTimeMillis() + 120000) // +2 minuty

        return TriggerBuilder.newTrigger()
            .forJob(jobDetail)
            .withIdentity("dailyAbandonedVisitsTrigger", "cleanupJobs")
            .withDescription("Trigger for daily abandoned visits cleanup")
            .startAt(startTime)
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 0/2 * * * ?") // Co 2 minuty
                    .withMisfireHandlingInstructionDoNothing()
            )
            .build()
    }
}
