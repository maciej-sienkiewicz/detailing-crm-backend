package com.carslab.crm.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Configuration for invoice processing services
 */
@Configuration
class InvoiceConfig {

    @Value("\${openai.connect.timeout:60000}")
    private val connectTimeout: Long = 60000

    @Value("\${openai.read.timeout:90000}")
    private val readTimeout: Long = 90000

    /**
     * Provides a configured RestTemplate for making API calls
     */
    @Bean("invoiceRestTemplate")
    fun invoiceRestTemplate(builder: RestTemplateBuilder): RestTemplate {
        @Suppress("DEPRECATION")
        return builder
            .setConnectTimeout(Duration.ofMillis(connectTimeout))
            .setReadTimeout(Duration.ofMillis(readTimeout))
            .additionalMessageConverters(StringHttpMessageConverter(StandardCharsets.UTF_8))
            .build()
    }

    /**
     * Provides a configured ObjectMapper for invoice JSON processing
     */
    @Bean("invoiceObjectMapper")
    fun invoiceObjectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
    }
}