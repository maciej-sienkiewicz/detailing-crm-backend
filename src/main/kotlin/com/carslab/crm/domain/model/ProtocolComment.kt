package com.carslab.crm.domain.model


data class ProtocolComment(val id: Long = System.currentTimeMillis(),
                           val protocolId: ProtocolId ,
                           val author: String,
                           val content: String ,
                           val timestamp: String,
                           val type: String
)