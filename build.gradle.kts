import kotlin.time.Duration

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

	implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
	implementation("com.google.api-client:google-api-client:2.0.0")
	implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")

	implementation("com.google.oauth-client:google-oauth-client:1.34.1")
	implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")

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

	// Thymeleaf + OGNL (NAPRAWKA)
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("ognl:ognl:3.3.4")

	// NAPRAWKA: Usuń OpenHTML to PDF i dodaj Playwright
	// implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10") // USUNIĘTE
	// implementation("com.openhtmltopdf:openhtmltopdf-slf4j:1.0.10") // USUNIĘTE
	// implementation("com.openhtmltopdf:openhtmltopdf-svg-support:1.0.10") // USUNIĘTE

	// NOWE: Playwright for Java - najlepsze rozwiązanie do PDF z HTML
	implementation("com.microsoft.playwright:playwright:1.39.0")

	// Fonty dla PDF (opcjonalne, ale zalecane dla polskich znaków)
	implementation("org.apache.pdfbox:fontbox:2.0.28")

	// Rate limiting
	implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")

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

// NAPRAWKA: Dodaj task do instalacji Playwright browsers
tasks.register("installPlaywrightBrowsers", Exec::class) {
	group = "playwright"
	description = "Install Playwright browsers"

	doFirst {
		println("Installing Playwright browsers...")
	}

	commandLine("java", "-cp", sourceSets.main.get().runtimeClasspath.asPath,
		"com.microsoft.playwright.CLI", "install", "chromium")

	dependsOn("classes")

	// Ignore failure if browsers are already installed
	isIgnoreExitValue = true

	doLast {
		println("Playwright browsers installation completed")
	}
}

// Auto-install browsers po build tylko w development
if (!System.getenv("CI").toBoolean() && !project.hasProperty("skipPlaywrightInstall")) {
	tasks.named("build") {
		finalizedBy("installPlaywrightBrowsers")
	}
}

// Task do manualnego czyszczenia cache Playwright
tasks.register("cleanPlaywrightCache", Delete::class) {
	group = "playwright"
	description = "Clean Playwright browser cache"

	delete(fileTree(System.getProperty("user.home") + "/.cache/ms-playwright"))

	doLast {
		println("Playwright cache cleaned")
	}
}

// Task do sprawdzenia statusu Playwright
tasks.register("checkPlaywright", Exec::class) {
	group = "playwright"
	description = "Check Playwright installation status"

	commandLine("java", "-cp", sourceSets.main.get().runtimeClasspath.asPath,
		"com.microsoft.playwright.CLI", "--version")

	dependsOn("classes")
	isIgnoreExitValue = true
}