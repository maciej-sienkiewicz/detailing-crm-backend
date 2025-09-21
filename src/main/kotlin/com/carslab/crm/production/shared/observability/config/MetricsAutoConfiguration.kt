package com.carslab.crm.production.shared.observability.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = [
    "com.carslab.crm.production.shared.observability"
])
@ConditionalOnProperty(
    name = ["observability.metrics.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class MetricsAutoConfiguration