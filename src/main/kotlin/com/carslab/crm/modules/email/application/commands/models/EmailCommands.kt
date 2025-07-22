package com.carslab.crm.modules.email.application.commands.models

import com.carslab.crm.infrastructure.cqrs.Command

data class SendProtocolEmailCommand(
    val protocolId: String,
) : Command<String>