package com.carslab.crm.infrastructure.cqrs

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

interface CommandBus {
    fun <R> execute(command: Command<R>): R
}

interface Command<R>

interface CommandHandler<C : Command<R>, R> {
    fun handle(command: C): R
}

@Service
class SimpleCommandBus(
    private val applicationContext: ApplicationContext
) : CommandBus {

    @Suppress("UNCHECKED_CAST")
    override fun <R> execute(command: Command<R>): R {
        val handlerClass = findHandlerClass(command::class.java)
        val handler = applicationContext.getBean(handlerClass) as CommandHandler<Command<R>, R>
        return handler.handle(command)
    }

    private fun findHandlerClass(commandClass: Class<*>): Class<*> {
        val handlerClassName = commandClass.name.replace("Command", "CommandHandler")
        return try {
            Class.forName(handlerClassName)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("No handler found for command: ${commandClass.simpleName}")
        }
    }
}