package com.carslab.crm.modules.email.application.commands.models

import com.carslab.crm.infrastructure.cqrs.Command

data class SendProtocolEmailCommand(
    val protocolId: String,
    val recipientEmail: String? = null,
    val customSubject: String? = null,
    val additionalVariables: Map<String, String> = emptyMap()
) : Command<String>