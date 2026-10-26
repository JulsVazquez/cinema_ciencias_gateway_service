package mx.unam.fciencias.cinema_ciencias.gateway_service.cinema_ciencias_gateway_service

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RequestPredicates.path
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http

/**
 * Rutas estáticas hacia el Auth Service (login, registro y usuarios).
 * Las rutas dinámicas hacia el monolito (lb://cinema-ciencias) están en el application.yaml
 * y se resuelven contra Eureka.
 */
@Configuration
class GatewayConfig(private val loggingFilter: GatewayLoggingFilter) {

    @Bean
    fun authServiceRoute(): RouterFunction<ServerResponse> {
        return route("auth-service-route")
            .POST("/api/v1/cinema/auth/**", http("http://localhost:8082"))
            .route(path("/api/v1/cinema/user/**"), http("http://localhost:8082"))
            .filter(loggingFilter.logRequest())
            .build()
    }
}
