package com.carslab.crm.signature.infrastructure.monitoring

import com.carslab.crm.signature.service.WebSocketService
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.stereotype.Component
import javax.sql.DataSource
import java.sql.Connection
import java.time.Instant

@Component
class SignatureSystemHealthIndicator(
    private val dataSource: DataSource,
    private val webSocketService: WebSocketService
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

            builder.withDetail("database.status", if (isValid) "UP" else "DOWN")
            builder.withDetail("database.responseTime", "${responseTime}ms")

        }
    }

    private fun checkWebSocketHealth(builder: Health.Builder) {
        val activeConnections = webSocketService.getActiveConnectionsCount()
        val activeTablets = webSocketService.getActiveTabletsCount()
        val activeWorkstations = webSocketService.getActiveWorkstationsCount()

        builder.withDetail("websocket.active_connections", activeConnections)
        builder.withDetail("websocket.active_tablets", activeTablets)
        builder.withDetail("websocket.active_workstations", activeWorkstations)

        when {
            activeConnections > 1000 -> {
                builder.status(Status("WARN"))
                builder.withDetail("websocket.warning", "High number of connections")
            }
            activeTablets == 0 -> {
                builder.status(Status("WARN"))
                builder.withDetail("websocket.warning", "No tablets connected")
            }
            else -> {
                // Status remains UP (default)
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
            builder.status(Status("WARN"))
            builder.withDetail("memory.warning", "High memory usage")
        } else {
            // Status remains UP (default)
        }
    }
}