package com.carslab.crm.production.modules.activities.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.activities.application.dto.ActivityResponse
import com.carslab.crm.production.modules.activities.application.dto.CreateActivityRequest
import com.carslab.crm.production.modules.activities.domain.command.CreateActivityCommand
import com.carslab.crm.production.modules.activities.domain.command.RelatedEntity
import com.carslab.crm.production.modules.activities.domain.model.ActivityId
import com.carslab.crm.production.modules.activities.domain.service.ActivityDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ActivityCommandService(
    private val domainService: ActivityDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ActivityCommandService::class.java)

    fun createActivity(request: CreateActivityRequest): ActivityId {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating activity for company: {}", companyId)

        val command = CreateActivityCommand(
            companyId = companyId,
            category = request.category,
            message = request.message,
            description = request.description,
            userId = request.userId,
            userName = request.userName,
            status = request.status,
            statusText = request.statusText,
            primaryEntity = request.primaryEntity?.let {
                RelatedEntity(it.id, it.type, it.name)
            },
            relatedEntities = request.relatedEntities.map {
                RelatedEntity(it.id, it.type, it.name)
            },
            metadata = request.metadata
        )

        val activity = domainService.createActivity(command)

        logger.info("Activity created successfully: {}", activity.id.value)
        return activity.id
    }
}