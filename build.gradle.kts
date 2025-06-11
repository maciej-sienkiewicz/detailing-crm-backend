plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.3"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.25"
}

group = "com.carslab"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot starters
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
	implementation("com.google.api-client:google-api-client:2.0.0")
	implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
	implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

	implementation("org.springframework.boot:spring-boot-starter-quartz")

	implementation("org.apache.pdfbox:pdfbox:2.0.28")
	implementation("com.sun.mail:jakarta.mail:2.0.1")
	implementation("jakarta.activation:jakarta.activation-api:2.0.1")
	implementation("jakarta.mail:jakarta.mail-api:2.0.1")


	// Database
	implementation("org.postgresql:postgresql")
	runtimeOnly("com.h2database:h2") // For testing

	// Security & JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

	// Resilience4j
	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
	implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
	implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
	implementation("io.github.resilience4j:resilience4j-timelimiter:2.2.0")

	// Rate limiting
	implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")
	implementation("com.github.vladimir-bukhtoyarov:bucket4j-redis:7.6.0")

	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

	// Metrics
	implementation("io.micrometer:micrometer-registry-prometheus")

	// Kotlin
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	// API Documentation
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0")

	// Mail (if needed)
	implementation("org.springframework.boot:spring-boot-starter-mail")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("com.h2database:h2")
	testImplementation("org.testcontainers:testcontainers")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.testcontainers:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}