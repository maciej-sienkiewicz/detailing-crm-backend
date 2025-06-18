package com.carslab.crm.infrastructure.cqrs

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class CommandBus(private val applicationContext: ApplicationContext) {

    fun <TResult> execute(command: Command<TResult>): TResult {
        val handlerClass = findHandlerClass(command::class)
        val handler = applicationContext.getBean(handlerClass)

        @Suppress("UNCHECKED_CAST")
        return (handler as CommandHandler<Command<TResult>, TResult>).handle(command)
    }

    private fun findHandlerClass(commandClass: KClass<*>): Class<*> {
        // Najpierw spróbuj znaleźć handler po konwencji nazewnictwa
        val handlerClassName = "${commandClass.qualifiedName}Handler"

        return try {
            Class.forName(handlerClassName)
        } catch (e: ClassNotFoundException) {
            // Fallback: szukaj w Spring context
            findHandlerInContext(commandClass)
                ?: throw IllegalArgumentException("No handler found for command: ${commandClass.simpleName}")
        }
    }

    private fun findHandlerInContext(commandClass: KClass<*>): Class<*>? {
        val handlerBeans = applicationContext.getBeansOfType(CommandHandler::class.java)

        return handlerBeans.values
            .firstOrNull { handler ->
                val handlerClassName = handler.javaClass.simpleName
                val commandName = commandClass.simpleName

                // Sprawdź różne konwencje nazewnictwa
                handlerClassName.contains(commandName ?: "") ||
                        handlerClassName.equals("${commandName}Handler", ignoreCase = true) ||
                        handlerClassName.equals("${commandName}CommandHandler", ignoreCase = true)
            }?.javaClass
    }
}