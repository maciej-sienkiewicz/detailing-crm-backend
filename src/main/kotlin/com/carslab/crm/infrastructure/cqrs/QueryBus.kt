// src/main/kotlin/com/carslab/crm/infrastructure/cqrs/QueryBus.kt
package com.carslab.crm.infrastructure.cqrs

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

interface QueryBus {
    fun <R> execute(query: Query<R>): R
}

interface Query<R>

interface QueryHandler<Q : Query<R>, R> {
    fun handle(query: Q): R
}

@Service
class SimpleQueryBus(
    private val applicationContext: ApplicationContext
) : QueryBus {

    @Suppress("UNCHECKED_CAST")
    override fun <R> execute(query: Query<R>): R {
        val handlerClass = findHandlerClass(query::class.java)
        val handler = applicationContext.getBean(handlerClass) as QueryHandler<Query<R>, R>
        return handler.handle(query)
    }

    private fun findHandlerClass(queryClass: Class<*>): Class<*> {
        val handlerClassName = queryClass.name.replace("Query", "QueryHandler")
        return try {
            Class.forName(handlerClassName)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("No handler found for query: ${queryClass.simpleName}")
        }
    }
}