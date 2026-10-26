package mx.unam.fciencias.cinema_ciencias.gateway_service.cinema_ciencias_gateway_service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFilterFunction
import org.springframework.web.servlet.function.ServerResponse
import java.time.LocalDateTime

@Component
class GatewayLoggingFilter {

    private val logger = LoggerFactory.getLogger(GatewayLoggingFilter::class.java)

    fun logRequest(): HandlerFilterFunction<ServerResponse, ServerResponse> {
        return HandlerFilterFunction { request, next ->
            if (request.path().contains("/auth/login")) {
                logger.info("🚀 GATEWAY: Petición de TOKEN recibida para la ruta: ${request.path()}")
                logger.info("📅 Timestamp: ${LocalDateTime.now()}")
            }

            val response = next.handle(request)

            if (request.path().contains("/auth/login")) {
                logger.info("🛰️ GATEWAY: Respuesta del Auth-Service recibida con estado: ${response.statusCode()}")
            }

            response
        }
    }
}