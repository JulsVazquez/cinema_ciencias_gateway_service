package mx.unam.fciencias.cinema_ciencias.gateway_service.cinema_ciencias_gateway_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
class CinemaCienciasGatewayServiceApplication

fun main(args: Array<String>) {
	runApplication<CinemaCienciasGatewayServiceApplication>(*args)
}
