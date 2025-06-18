package com.carslab.crm.infrastructure.cqrs

interface CommandHandler<TCommand : Command<TResult>, TResult> {
    fun handle(command: TCommand): TResult
}