plugins {
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.flywaydb.flyway") version "9.22.3"
    java
}

group = "com.edufelip"
version = "0.0.1-SNAPSHOT"
description = "Server for Meer application"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

configurations {
    // Ensure Flyway configuration exists for JDBC drivers when using the Gradle plugin
    maybeCreate("flyway")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("net.coobird:thumbnailator:0.4.20")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0")
    implementation("com.google.cloud:google-cloud-storage:2.40.0")
    implementation("com.fasterxml.uuid:java-uuid-generator:4.3.0")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
    implementation("com.google.api-client:google-api-client:2.6.0")
    implementation("com.google.http-client:google-http-client-jackson2:1.43.3")
    implementation("org.flywaydb:flyway-core:9.22.3")
    implementation("org.postgresql:postgresql")
    add("flyway", "org.postgresql:postgresql:42.7.4")
    // Override vulnerable transitive protobuf to fixed version (CVE-2024-7254)
    implementation("com.google.protobuf:protobuf-java:4.28.2")
    // Override vulnerable transitive grpc-netty-shaded to fixed version (CVE-2025-55163)
    implementation("io.grpc:grpc-netty-shaded:1.75.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName.set("meer")
}

flyway {
    val env = System.getenv()
    fun requireEnv(key: String, alternate: String? = null): String =
        env[key] ?: alternate ?: error("Missing required environment variable: $key (needed for Flyway)")

    val dbHost = requireEnv("DB_HOST")
    val dbPort = requireEnv("DB_PORT")
    val dbName = requireEnv("DB_NAME")

    url = "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}"
    user = requireEnv("DB_USER")
    password = requireEnv("DB_PASSWORD")

    val includeDev = (env["SPRING_PROFILES_ACTIVE"]?.split(',')?.contains("local-db") == true)
    val prodPath = "filesystem:src/main/resources/db/migration"
    val devPath = "filesystem:src/main/resources/db/dev"
    locations = if (includeDev) arrayOf(prodPath, devPath) else arrayOf(prodPath)

    // Ensure Flyway sees the database plugin and driver
    configurations = arrayOf("compileClasspath", "runtimeClasspath", "flyway")

    baselineOnMigrate = true
    baselineVersion = "0"
    cleanDisabled = true
}

// For Flyway 9, adding driver to 'flyway' configuration is sufficient.
