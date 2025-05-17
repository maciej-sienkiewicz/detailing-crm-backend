package com.carslab.crm.infrastructure.logging

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Adnotacja do oznaczania metod, które powinny być objęte aspektem logowania.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogOperation(val operation: String)

/**
 * Aspekt do automatycznego logowania operacji biznesowych.
 * Zapewnia jednolity format logów i zarządzanie kontekstem MDC.
 */
@Aspect
@Component
class LoggingAspect {

    private val logger = LoggerFactory.getLogger(LoggingAspect::class.java)

    companion object {
        private const val OPERATION_ID = "operationId"
        private const val OPERATION_NAME = "operationName"
        private const val CLIENT_ID = "clientId"
        private const val CONTACT_ATTEMPT_ID = "contactAttemptId"

        // Lista parametrów, które powinny być przechwycone i dodane do MDC
        private val MDC_PARAMETER_NAMES = mapOf(
            "clientId" to CLIENT_ID,
            "contactAttemptId" to CONTACT_ATTEMPT_ID
        )
    }

    @Around("@annotation(logOperation)")
    fun logAround(joinPoint: ProceedingJoinPoint, logOperation: LogOperation): Any? {
        val operationId = UUID.randomUUID().toString()
        val operationName = logOperation.operation

        setupMdc(operationId, operationName, joinPoint)

        try {
            logger.debug("Rozpoczęto operację: {}", operationName)
            val result = joinPoint.proceed()
            logger.debug("Zakończono operację: {} | status=SUCCESS", operationName)
            return result
        } catch (e: Exception) {
            logger.error("Błąd podczas operacji: {} | status=FAILED | error={}", operationName, e.message, e)
            throw e
        } finally {
            MDC.clear()
        }
    }

    private fun setupMdc(operationId: String, operationName: String, joinPoint: ProceedingJoinPoint) {
        MDC.put(OPERATION_ID, operationId)
        MDC.put(OPERATION_NAME, operationName)

        // Dodaj parametry do MDC jeśli występują w sygnaturze metody
        val methodParameters = joinPoint.signature.declaringType.methods
            .find { it.name == joinPoint.signature.name }
            ?.parameters ?: return

        val args = joinPoint.args

        methodParameters.forEachIndexed { index, param ->
            if (index < args.size) {
                val paramName = param.name
                val paramValue = args[index]

                // Dla klientów sprawdzamy ID
                if (paramName == "client" && paramValue != null) {
                    val clientIdField = paramValue.javaClass.getDeclaredField("id")
                    clientIdField.isAccessible = true
                    val clientIdObj = clientIdField.get(paramValue)
                    val valueField = clientIdObj.javaClass.getDeclaredField("value")
                    valueField.isAccessible = true
                    MDC.put(CLIENT_ID, valueField.get(clientIdObj).toString())
                }

                // Dla innych parametrów sprawdzamy mapowanie
                MDC_PARAMETER_NAMES[paramName]?.let { mdcKey ->
                    if (paramValue != null) {
                        val valueStr = when (paramValue) {
                            is String -> paramValue
                            else -> {
                                try {
                                    val valueField = paramValue.javaClass.getDeclaredField("value")
                                    valueField.isAccessible = true
                                    valueField.get(paramValue).toString()
                                } catch (e: NoSuchFieldException) {
                                    paramValue.toString()
                                }
                            }
                        }
                        MDC.put(mdcKey, valueStr)
                    }
                }
            }
        }
    }
}