plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
}

group = "com.pelypets.todolist"
version = "1.0.0"
application {
    mainClass = "com.pelypets.todolist.server.ApplicationKt"
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverConfigYaml)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.serverAuth)
    implementation(libs.ktor.serverAuthJwt)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.ktor.serverRequestValidation)
    implementation(libs.ktor.serverCallLogging)
    implementation(libs.ktor.serverCallId)
    implementation(libs.ktor.serverRateLimit)
    implementation(libs.ktor.serverForwardedHeader)
    implementation(libs.ktor.serverMetricsMicrometer)
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.ktor.serverSwagger)
    runtimeOnly(libs.logstash.logbackEncoder)
    implementation(libs.koin.ktor)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.bcrypt)
    implementation(libs.google.apiClient)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.ktor.clientContentNegotiation)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.test {
    // Colima / Docker Desktop: Testcontainers needs the host socket path to mount it into Ryuk.
    environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
}
