package com.carslab.crm.config

import com.carslab.crm.infrastructure.auth.CustomUserDetailsService
import com.carslab.crm.domain.permissions.PermissionService
import com.carslab.crm.infrastructure.validation.permissions.CustomPermissionEvaluator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.apache.catalina.connector.Connector
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val userDetailsService: CustomUserDetailsService,
    private val permissionService: PermissionService
) {

    @Bean
    fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        mapper.registerModule(JavaTimeModule())

        // Additional options to improve deserialization
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)

        return mapper
    }

    @Bean
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .anyRequest().permitAll()
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .headers { headers -> headers.frameOptions { it.disable() } }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun authenticationProvider(userDetailsService: CustomUserDetailsService): AuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder())
        return provider
    }

    @Bean
    fun permissionEvaluator(): PermissionEvaluator {
        return CustomPermissionEvaluator(permissionService)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()

        val defaultConfiguration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
            allowCredentials = true
            exposedHeaders = listOf("Authorization", "Content-Disposition")
            maxAge = 3600L
        }

        // Specjalna konfiguracja dla endpointów binarnych (PDF, obrazy)
        val binaryConfiguration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
            allowCredentials = true
            exposedHeaders = listOf("Content-Disposition", "Content-Type", "Content-Length")
            maxAge = 3600L
        }

        // Rejestracja konfiguracji
        source.registerCorsConfiguration("/**", defaultConfiguration)
        source.registerCorsConfiguration("/api/printer/protocol/*/pdf", binaryConfiguration)
        source.registerCorsConfiguration("/api/receptions/image/*", binaryConfiguration)

        return source
    }

    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                // Standardowe endpointy
                registry.addMapping("/api/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                    .allowedHeaders("Authorization", "Content-Type", "Accept")
                    .allowCredentials(true)
                    .exposedHeaders("Authorization", "Content-Disposition")
                    .maxAge(3600)

                // Specjalne mapowanie dla binarnych endpointów
                registry.addMapping("/api/printer/protocol/*/pdf")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "OPTIONS")
                    .allowedHeaders("Authorization", "Content-Type", "Accept")
                    .allowCredentials(true)
                    .exposedHeaders("Content-Disposition", "Content-Type", "Content-Length")
                    .maxAge(3600)

                registry.addMapping("/api/receptions/image/*")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "OPTIONS")
                    .allowedHeaders("Authorization", "Content-Type", "Accept")
                    .allowCredentials(true)
                    .exposedHeaders("Content-Disposition", "Content-Type", "Content-Length")
                    .maxAge(3600)
            }
        }
    }

    @Bean
    fun tomcatServletWebServerFactory(): TomcatServletWebServerFactory {
        return object : TomcatServletWebServerFactory() {
            override fun customizeConnector(connector: Connector) {
                super.customizeConnector(connector)
                // Ustawienie dozwolonych znaków
                connector.setProperty("relaxedQueryChars", "[]|{}^\\`")
                connector.setProperty("relaxedPathChars", "[]|{}^\\`")
            }
        }
    }
}