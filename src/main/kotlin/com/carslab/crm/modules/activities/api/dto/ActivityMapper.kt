// src/main/kotlin/com/carslab/crm/modules/activities/api/dto/ActivityMapper.kt
package com.carslab.crm.modules.activities.api.dto

import com.carslab.crm.modules.activities.application.queries.models.*
import java.time.format.DateTimeFormatter

object ActivityMapper {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    fun toDetailDto(readModel: ActivityReadModel): ActivityDetailDto {
        return ActivityDetailDto(
            id = readModel.id,
            timestamp = readModel.timestamp.format(dateTimeFormatter),
            category = ActivityCategory.valueOf(readModel.category.name),
            message = readModel.message,
            userId = readModel.userId,
            userName = readModel.userName,
            userColor = readModel.userColor,
            entityType = readModel.entityType?.let { EntityType.valueOf(it.name) },
            entityId = readModel.entityId,
            entities = readModel.entities?.map { toEntityDto(it) },
            status = readModel.status?.let { ActivityStatus.valueOf(it.name) },
            statusText = readModel.statusText,
            metadata = readModel.metadata?.let { toMetadataDto(it) }
        )
    }

    fun toEntityDto(readModel: ActivityEntityReadModel): ActivityEntityDto {
        return ActivityEntityDto(
            id = readModel.id,
            type = EntityType.valueOf(readModel.type.name),
            displayName = readModel.displayName,
            relatedId = readModel.relatedId,
            metadata = readModel.metadata
        )
    }

    fun toMetadataDto(readModel: ActivityMetadataReadModel): ActivityMetadataDto {
        return ActivityMetadataDto(
            notes = readModel.notes,
            previousValue = readModel.previousValue,
            newValue = readModel.newValue,
            appointmentDuration = readModel.appointmentDuration,
            servicesList = readModel.servicesList,
            vehicleCondition = readModel.vehicleCondition,
            damageCount = readModel.damageCount,
            commentType = readModel.commentType,
            isResolved = readModel.isResolved,
            notificationType = readModel.notificationType,
            notificationContent = readModel.notificationContent,
            isRead = readModel.isRead,
            systemAction = readModel.systemAction,
            affectedRecords = readModel.affectedRecords
        )
    }

    fun toDailySummaryDto(readModel: DailySummaryReadModel): DailySummaryDto {
        return DailySummaryDto(
            date = readModel.date.format(dateFormatter),
            appointmentsScheduled = readModel.appointmentsScheduled,
            protocolsCompleted = readModel.protocolsCompleted,
            vehiclesServiced = readModel.vehiclesServiced,
            newClients = readModel.newClients,
            commentsAdded = readModel.commentsAdded,
            totalActivities = readModel.totalActivities
        )
    }

    fun toAnalyticsDto(readModel: ActivityAnalyticsReadModel): ActivityAnalyticsDto {
        return ActivityAnalyticsDto(
            summary = ActivityAnalyticsSummaryDto(
                totalActivities = readModel.summary.totalActivities,
                categoriesBreakdown = readModel.summary.categoriesBreakdown.mapKeys {
                    ActivityCategory.valueOf(it.key.name)
                },
                usersBreakdown = readModel.summary.usersBreakdown,
                statusBreakdown = readModel.summary.statusBreakdown,
                trendsData = readModel.summary.trendsData.map { trend ->
                    TrendDataPointDto(
                        date = trend.date.format(dateFormatter),
                        count = trend.count,
                        categories = trend.categories.mapKeys {
                            ActivityCategory.valueOf(it.key.name)
                        }
                    )
                }
            ),
            dailySummaries = readModel.dailySummaries.map { toDailySummaryDto(it) },
            topUsers = readModel.topUsers.map { user ->
                TopUserDto(
                    userId = user.userId,
                    userName = user.userName,
                    activityCount = user.activityCount,
                    categories = user.categories.mapKeys {
                        ActivityCategory.valueOf(it.key.name)
                    }
                )
            },
            entityStats = readModel.entityStats.map { stats ->
                EntityStatsDto(
                    entityType = EntityType.valueOf(stats.entityType.name),
                    entityId = stats.entityId,
                    entityDisplayName = stats.entityDisplayName,
                    activityCount = stats.activityCount,
                    lastActivity = stats.lastActivity.format(dateTimeFormatter)
                )
            }
        )
    }
}