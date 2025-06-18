package com.carslab.crm.infrastructure.cqrs

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class QueryBus(private val applicationContext: ApplicationContext) {

    @Suppress("UNCHECKED_CAST")
    fun <TResult> execute(query: Query<TResult>): TResult {
        val handlerClass = findHandlerClass(query::class)
        val handler = applicationContext.getBean(handlerClass) as QueryHandler<Query<TResult>, TResult>
        return handler.handle(query)
    }

    private fun findHandlerClass(queryClass: KClass<*>): Class<*> {
        val handlerClassName = queryClass.qualifiedName + "Handler"
        return try {
            Class.forName(handlerClassName)
        } catch (e: ClassNotFoundException) {
            // Fallback: look for handler beans in the context
            val handlerBeans = applicationContext.getBeansOfType(QueryHandler::class.java)
            handlerBeans.values.firstOrNull { handler ->
                handler.javaClass.simpleName.contains(queryClass.simpleName!!)
            }?.javaClass ?: throw IllegalArgumentException("No handler found for query: ${queryClass.simpleName}")
        }
    }
}