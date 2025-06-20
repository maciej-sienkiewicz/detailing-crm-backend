package com.carslab.crm.modules.email.domain.ports

import com.carslab.crm.modules.email.domain.model.EmailHistory
import com.carslab.crm.modules.email.domain.model.EmailHistoryId

interface EmailRepository {
    fun save(emailHistory: EmailHistory): EmailHistory
    fun findById(id: EmailHistoryId): EmailHistory?
    fun updateStatus(id: EmailHistoryId, status: com.carslab.crm.modules.email.domain.model.EmailStatus, errorMessage: String? = null)
}