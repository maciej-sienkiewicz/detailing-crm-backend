package com.carslab.crm.domain.port

import com.carslab.crm.domain.model.ProtocolComment
import com.carslab.crm.domain.model.ProtocolId

interface ProtocolCommentsRepository {
    fun save(comment: ProtocolComment): ProtocolComment

    fun findById(id: ProtocolId): List<ProtocolComment>
}