package com.carslab.crm.production.modules.templates.domain.policy

import com.carslab.crm.production.modules.templates.domain.models.aggregates.Template
import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class TemplateBusinessPolicy {

    fun validateTemplateCreation(name: String, type: TemplateType, companyId: Long) {
        if (name.isBlank()) {
            throw BusinessException("Template name cannot be blank")
        }

        if (companyId <= 0) {
            throw BusinessException("Invalid company ID")
        }
    }

    fun validateTemplateUpdate(template: Template, newName: String) {
        if (newName.isBlank()) {
            throw BusinessException("Template name cannot be blank")
        }
    }

    fun validateTemplateDeletion(template: Template) {
        // Additional business rules for deletion can be added here
        // For example: cannot delete if template is being used in active processes
    }

    fun canUpdateTemplate(template: Template): Boolean {
        return true // Can be extended with more complex business logic
    }

    fun canDeleteTemplate(template: Template): Boolean {
        return true // Can be extended with more complex business logic
    }
}