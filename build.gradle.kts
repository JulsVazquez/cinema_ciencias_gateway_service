plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "3.4.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "mx.unam.fciencias.cinema_ciencias.gateway_service"
version = "0.0.1-SNAPSHOT"
description = "API Gateway y enrutador principal para Cinema Ciencias"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

// Con 2024.0.0 (Netflix 4.2.0) + Boot 3.4.3 el gateway no se registra en Eureka:
// "Unable to set redirect follow using reflection". Desde Netflix 4.2.1 (2024.0.1) se
// reescribió la creación del cliente HTTP de Eureka; se sube al último parche de la línea.
// Si el log sigue mostrando ese error, subir el gateway a Boot 3.5.x + Cloud 2025.0.x.
extra["springCloudVersion"] = "2024.0.3"

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.cloud:spring-cloud-starter-config")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.cloud:spring-cloud-starter-gateway-mvc")
	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
	implementation("io.jsonwebtoken:jjwt-api:0.13.0")
	implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
	implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
