package com.carslab.crm.security

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://localhost:*",
                "https://*.crm.com"
            )

            allowedMethods = listOf(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
            )

            allowedHeaders = listOf("*")

            exposedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
            )

            allowCredentials = true
            maxAge = 3600L
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        logger.info("Configuring Security Filter Chain")

        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

            .authorizeHttpRequests { auth ->
                auth
                    // OPTIONS requests MUSZĄ być dozwolone dla wszystkich endpointów
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // Public endpoints
                    .requestMatchers(
                        "/api/health/**",
                        "/api/users",
                        "/actuator/health",
                        "/api/tablets/pair",        // Parowanie tabletu
                        "/api/tablets/register",    // DODANE - Rejestracja tabletu
                        "/api/auth/**",
                        "/ws/**"
                    ).permitAll()

                    // Admin endpoints
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")

                    // Signature endpoints
                    .requestMatchers("/api/signatures/**").hasAnyRole("USER", "ADMIN", "TABLET")

                    // Tablet management - wymagane role
                    .requestMatchers("/api/tablets/**").hasAnyRole("MANAGER", "ADMIN", "USER")

                    // Reception endpoints
                    .requestMatchers("/api/receptions/**").authenticated()

                    .anyRequest().authenticated()
            }

            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

            .build()
    }
}