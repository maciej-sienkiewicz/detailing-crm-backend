package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.ProtocolComment
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.port.ProtocolCommentsRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryProtocolCommentsRepository: ProtocolCommentsRepository {

    private val protocolComments = ConcurrentHashMap<ProtocolId, List<ProtocolComment>>()

    override fun save(comment: ProtocolComment): ProtocolComment {
        protocolComments[comment.protocolId] = (protocolComments[comment.protocolId] ?: emptyList()) + comment
        return comment
    }

    override fun findById(id: ProtocolId): List<ProtocolComment> {
        return protocolComments[id] ?: emptyList()
    }
}