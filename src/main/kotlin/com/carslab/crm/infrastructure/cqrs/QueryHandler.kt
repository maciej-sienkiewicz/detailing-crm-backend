package com.carslab.crm.infrastructure.cqrs

interface QueryHandler<TQuery : Query<TResult>, TResult> {
    fun handle(query: TQuery): TResult
}