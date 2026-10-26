package mx.unam.fciencias.cinema_ciencias.gateway_service.cinema_ciencias_gateway_service

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

// Los secretos los sirve el Config Server; en el test se apagan Config Client y Eureka.
@SpringBootTest(
	properties = [
		"cinema.security.jwtSecret=8a9b5c2d1e4f7a0c3b6d9e2f5a8c1b4d7e0f3a6c9b2d5e8f1a4c7b0e3d6f9a2c",
		"cinema.security.gatewaySecret=secreto-de-prueba",
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false"
	]
)
class CinemaCienciasGatewayServiceApplicationTests {

	@Test
	fun contextLoads() {
	}

}
