import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.unbrokendome.gradle.plugins.testsets.dsl.testSets

plugins {
  application
  kotlin("jvm") version Versions.kotlin
  kotlin("plugin.serialization") version Versions.kotlin
  id("com.diffplug.spotless") version Versions.spotless
  id("org.unbroken-dome.test-sets") version "4.0.0"
}

group = "io.skinnydoo"
version = "0.0.1"
application {
  mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("io.arrow-kt:arrow-stack:1.2.1"))

  implementation("io.arrow-kt:arrow-core")

  val kotlinVersion = "1.9.21"
  val kotlinCoroutinesVersion = "1.7.3"
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$kotlinCoroutinesVersion")

  val koinVersion = "3.5.3"
  implementation("io.insert-koin:koin-ktor:$koinVersion")
  implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

  val ktorVersion = "2.3.7"
  implementation("io.ktor:ktor-server-auth:$ktorVersion")
  implementation("io.ktor:ktor-server-core:$ktorVersion")
  implementation("io.ktor:ktor-server-resources:$ktorVersion")
  implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
  implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
  implementation("io.ktor:ktor-server-host-common:$ktorVersion")
  implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
  implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
  implementation("io.ktor:ktor-server-netty:$ktorVersion")
  implementation("io.ktor:ktor-server-sessions:$ktorVersion")
  implementation("io.ktor:ktor-serialization:$ktorVersion")
  implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
  implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")

  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-cio:$ktorVersion")
  implementation("io.ktor:ktor-client-serialization:$ktorVersion")
  implementation("io.ktor:ktor-client-auth:$ktorVersion")

  val exposedVersion = "0.45.0"
  implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

  implementation("org.mindrot:jbcrypt:0.4")
  implementation("com.zaxxer:HikariCP:5.1.0")
  implementation("ch.qos.logback:logback-classic:1.4.14")
  implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
  implementation("mysql:mysql-connector-java:8.0.28")

  implementation("com.github.spoptchev:scientist:1.0.2")

  implementation("commons-validator:commons-validator:1.8.0")
  //implementation("commons-collections:commons-collections:3.2.2")
  implementation("org.apache.commons:commons-collections4:4.4")
  implementation("com.auth0:java-jwt:4.4.0")

  testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutinesVersion")
  val kotestVersion = "5.8.0"
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
  testImplementation("io.kotest.extensions:kotest-assertions-ktor:2.0.0")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.4.0")
  testImplementation("com.h2database:h2:2.2.220")
}

spotless {
  kotlin {
    ktlint(Versions.ktlint).userData(
      mapOf(
        "indent_size" to "2",
        "indent_style" to "space",
        "tab_width" to "2",
        "max_line_length" to "120",
        "disabled_rules" to "no-wildcard-imports",
      )
    )
  }
  kotlinGradle {
    target("*.gradle.kts")
    ktlint(Versions.ktlint)
  }
}

testSets {
  val integrationTest by creating
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    languageVersion = "1.6"
    freeCompilerArgs = freeCompilerArgs + listOf(
      "-Xopt-in=kotlin.RequiresOptIn",
      "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi",
      "-Xopt-in=io.ktor.locations.KtorExperimentalLocationsAPI",
      "-Xopt-in=kotlin.time.ExperimentalTime",
      "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    )
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}
