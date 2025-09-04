plugins {
  kotlin("jvm") version "2.2.10"
  kotlin("plugin.allopen") version "2.2.10"
  id("io.quarkus")
  id("org.sonarqube") version "6.0.1.5171"
  id("com.diffplug.spotless") version "7.0.2"
  id("com.dipien.semantic-version") version "2.0.0" apply false
}

repositories {
  mavenCentral()
  mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
  implementation("io.quarkus:quarkus-jacoco")
  implementation(kotlin("stdlib-jdk8"))
  implementation(
    enforcedPlatform(
      "${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"
    )
  )
  implementation("io.quarkus:quarkus-kotlin")
  implementation("io.quarkus:quarkus-rest")
  implementation("io.quarkus:quarkus-rest-jackson")
  implementation("io.quarkus:quarkus-resteasy-client")
  implementation("io.quarkus:quarkus-resteasy-client-jackson")
  implementation("io.quarkus:quarkus-arc")
  implementation("io.quarkus:quarkus-smallrye-health")
  implementation("io.quarkus:quarkus-opentelemetry")
  implementation("io.quarkus:quarkus-logging-json")
  testImplementation("io.quarkus:quarkus-junit5")
  testImplementation("io.quarkus:quarkus-junit5-mockito")
  testImplementation(kotlin("test"))
  testImplementation("io.rest-assured:rest-assured")
  testImplementation("org.mockito:mockito-core:5.19.0")
  testImplementation("org.mockito:mockito-junit-jupiter:5.19.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
  testImplementation("io.quarkus:quarkus-junit5-mockito")
  implementation("io.quarkus:quarkus-vertx-web:2.16.12.Final")
}

group = "it.pagopa.ecommerce"

version = "0.0.1-SNAPSHOT"

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

allOpen {
  annotation("jakarta.ws.rs.Path")
  annotation("jakarta.enterprise.context.ApplicationScoped")
  annotation("jakarta.persistence.Entity")
  annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<Test> {
  systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

tasks.withType<JavaCompile> {
  options.encoding = "UTF-8"
  options.compilerArgs.add("-parameters")
}

kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    javaParameters = true
  }
}

tasks
  .register("applySemanticVersionPlugin") { dependsOn("prepareKotlinBuildScriptModel") }
  .apply { apply(plugin = "com.dipien.semantic-version") }

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    toggleOffOn()
    targetExclude("build/**/*")
    ktfmt().kotlinlangStyle()
  }
  kotlinGradle {
    toggleOffOn()
    targetExclude("build/**/*.kts")
    ktfmt().googleStyle()
  }
  java {
    target("**/*.java")
    targetExclude("build/**/*")
    eclipse().configFile("eclipse-style.xml")
    toggleOffOn()
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
}
