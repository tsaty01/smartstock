plugins {
	java
	alias(libs.plugins.spring.boot)
}

group = "com.smartstock"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
	implementation(platform(libs.spring.boot.bom))
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.flyway)
	implementation(libs.spring.boot.starter.oauth2.resource.server)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.thymeleaf)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.flyway.postgresql)
	runtimeOnly(libs.postgresql)
	testImplementation(libs.spring.boot.starter.actuator.test)
	testImplementation(libs.spring.boot.starter.data.jpa.test)
	testImplementation(libs.spring.boot.starter.flyway.test)
	testImplementation(libs.spring.boot.starter.security.test)
	testImplementation(libs.spring.boot.starter.validation.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	// Tests run a real PostgreSQL 18 from bundled binaries, without Docker.
	testImplementation(libs.embedded.postgres)
	testImplementation(platform(libs.postgres.binaries.bom))
	testRuntimeOnly(libs.junit.platform.launcher)
	mockitoAgent(platform(libs.spring.boot.bom))
	mockitoAgent(libs.mockito.core) { isTransitive = false }
}

tasks.jar {
	archiveClassifier = ""
}

tasks.bootJar {
	archiveClassifier = "all"
}

tasks.withType<Test> {
	useJUnitPlatform()
	// Load Mockito as an agent instead of letting it self-attach at runtime (blocked by default in newer JDKs).
	jvmArgs("-javaagent:${mockitoAgent.asPath}")
}
