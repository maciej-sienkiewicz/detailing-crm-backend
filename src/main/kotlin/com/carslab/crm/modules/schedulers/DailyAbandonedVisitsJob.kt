package com.carslab.crm.modules.schedulers

import com.carslab.crm.modules.visits.domain.SimpleAbandonedVisitsServiceDeprecated
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class DailyAbandonedVisitsJob(
    private val simpleAbandonedVisitsServiceDeprecated: SimpleAbandonedVisitsServiceDeprecated,
) : Job {

    private val logger = LoggerFactory.getLogger(DailyAbandonedVisitsJob::class.java)

    override fun execute(context: JobExecutionContext) {
        val jobStart = LocalDateTime.now()
        logger.info("🕛 Starting daily abandoned visits cleanup job at $jobStart")

        try {
            // Execute the main cleanup operation
            val result = simpleAbandonedVisitsServiceDeprecated.cancelAbandonedVisits()

            // Log results
            logger.info("✅ Cleanup completed successfully:")
            logger.info("   - Protocols updated: ${result.updatedProtocolsCount}")
            logger.info("   - Comments added: ${result.commentsAddedCount}")

        } catch (exception: Exception) {
            logger.error("❌ Daily cleanup job failed", exception)

            throw JobExecutionException("Daily cleanup failed", exception)
        }
    }
}