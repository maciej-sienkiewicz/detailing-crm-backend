package com.carslab.crm.config

import com.carslab.crm.modules.schedulers.DailyAbandonedVisitsJob
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class QuartzConfiguration {

    @Bean
    fun dailyAbandonedVisitsJobDetail(): JobDetail {
        return JobBuilder.newJob(DailyAbandonedVisitsJob::class.java)
            .withIdentity("dailyAbandonedVisitsJob", "cleanupJobs")
            .withDescription("Daily cleanup of abandoned visits from previous day")
            .storeDurably(true)
            .build()
    }

    @Bean
    fun dailyAbandonedVisitsTrigger(
        @Qualifier("dailyAbandonedVisitsJobDetail") jobDetail: JobDetail
    ): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(jobDetail)
            .withIdentity("dailyAbandonedVisitsTrigger", "cleanupJobs")
            .withDescription("Trigger for daily abandoned visits cleanup")
            .withSchedule(
                // ✅ POPRAWIONY Cron: "0 0 1 * * ?" (co dzień o 1:00)
                CronScheduleBuilder.cronSchedule("0 0 1 * * ?")
                    .withMisfireHandlingInstructionDoNothing()
            )
            .build()
    }
}