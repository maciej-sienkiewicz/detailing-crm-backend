package com.carslab.crm.signature.infrastructure.monitoring

import org.springframework.boot.actuator.health.Health
import org.springframework.boot.actuator.health.HealthIndicator
import org.springframework.boot.actuator.health.Status
import org.springframework.stereotype.Component
import javax.sql.DataSource
import java.sql.Connection
import java.time.Instant

@Component
class SignatureSystemHealthIndicator(
    private val dataSource: DataSource,
    private val webSocketHandler: MultiTenantWebSocketHandler
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val builder = Health.up()

            // Check database connectivity
            checkDatabaseHealth(builder)

            // Check WebSocket system
            checkWebSocketHealth(builder)

            // Check system resources
            checkSystemResources(builder)

            builder.build()
        } catch (e: Exception) {
            Health.down()
                .withException(e)
                .withDetail("timestamp", Instant.now())
                .withDetail("component", "signature-system")
                .build()
        }
    }

    private fun checkDatabaseHealth(builder: Health.Builder) {
        val startTime = System.currentTimeMillis()

        dataSource.connection.use { connection: Connection ->
            val isValid = connection.isValid(5) // 5 second timeout
            val responseTime = System.currentTimeMillis() - startTime

            if (isValid) {
                builder.withDetail("database.status", "UP")
                builder.withDetail("database.responseTime", "${responseTime}ms")

                if (responseTime > 1000) {
                    builder.status(Status.valueOf("WARN"))
                    builder.withDetail("database.warning", "Slow response time")
                }
            } else {
                builder.status(Status.DOWN)
                builder.withDetail("database.status", "DOWN")
            }
        }
    }

    private fun checkWebSocketHealth(builder: Health.Builder) {
        val activeConnections = webSocketHandler.getActiveConnectionsCount()
        val activeTablets = webSocketHandler.getActiveTabletsCount()
        val activeWorkstations = webSocketHandler.getActiveWorkstationsCount()

        builder.withDetail("websocket.active_connections", activeConnections)
        builder.withDetail("websocket.active_tablets", activeTablets)
        builder.withDetail("websocket.active_workstations", activeWorkstations)

        when {
            activeConnections > 1000 -> {
                builder.status(Status.valueOf("WARN"))
                builder.withDetail("websocket.warning", "High number of connections")
            }
            activeTablets == 0 -> {
                builder.status(Status.valueOf("WARN"))
                builder.withDetail("websocket.warning", "No tablets connected")
            }
        }
    }

    private fun checkSystemResources(builder: Health.Builder) {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble()) * 100

        builder.withDetail("memory.used", "${usedMemory / (1024 * 1024)}MB")
        builder.withDetail("memory.max", "${maxMemory / (1024 * 1024)}MB")
        builder.withDetail("memory.usage_percent", "%.2f%%".format(memoryUsagePercent))

        if (memoryUsagePercent > 85) {
            builder.status(Status.valueOf("WARN"))
            builder.withDetail("memory.warning", "High memory usage")
        }
    }
}

@Component
class DatabaseHealthIndicator(
    private val signatureSessionRepository: SignatureSessionRepository
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val startTime = System.currentTimeMillis()

            // Simple query to test DB connectivity
            val count = signatureSessionRepository.count()
            val responseTime = System.currentTimeMillis() - startTime

            Health.up()
                .withDetail("database.signature_sessions_count", count)
                .withDetail("database.query_time", "${responseTime}ms")
                .withDetail("timestamp", Instant.now())
                .build()

        } catch (e: Exception) {
            Health.down()
                .withException(e)
                .withDetail("timestamp", Instant.now())
                .build()
        }
    }
}