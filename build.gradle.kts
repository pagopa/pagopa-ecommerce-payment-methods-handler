plugins {
  kotlin("jvm") version "2.2.10"
  id("io.quarkus")
  id("org.sonarqube") version "6.0.1.5171"
  id("com.diffplug.spotless") version "7.0.2"
  id("com.dipien.semantic-version") version "2.0.0" apply false
  id("org.openapi.generator") version "7.15.0"
  jacoco
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
  implementation("io.quarkus:quarkus-rest-client")
  implementation("io.quarkus:quarkus-rest-client-jackson")
  implementation("io.quarkus:quarkus-arc")
  implementation("io.quarkus:quarkus-smallrye-health")
  implementation("io.quarkus:quarkus-opentelemetry")
  implementation("io.quarkus:quarkus-logging-json")
  implementation("io.quarkiverse.openapi.generator:quarkus-openapi-generator:2.12.1")
  implementation("io.quarkus:quarkus-hibernate-validator")
  implementation("io.quarkus:quarkus-smallrye-openapi")
  testImplementation("io.quarkus:quarkus-junit5")
  testImplementation("io.quarkus:quarkus-junit5-mockito")
  testImplementation(kotlin("test"))
  testImplementation("io.rest-assured:rest-assured")
  testImplementation("org.mockito:mockito-core:5.19.0")
  testImplementation("org.mockito:mockito-junit-jupiter:5.19.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
  testImplementation("io.quarkus:quarkus-junit5-mockito")
  testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")
}

group = "it.pagopa.ecommerce"

version = "0.0.3-SNAPSHOT"

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
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

openApiGenerate {
  generatorName.set("jaxrs-spec")
  inputSpec.set(layout.projectDirectory.file("api-spec/v1/api.yaml").asFile.absolutePath)
  outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath)

  apiPackage.set("it.pagopa.ecommerce.payment.methods.v1.server.api")
  modelPackage.set("it.pagopa.ecommerce.payment.methods.v1.server.model")

  configOptions.set(
    mapOf(
      "interfaceOnly" to "true",
      "openApiNullable" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "dateLibrary" to "java8",
      "useJakartaEe" to "true",
      "useBeanValidation" to "true",
      "oas3" to "true",
      "useSwaggerAnnotations" to "false",
      "generateSupportingFiles" to "true",
      "supportAsync" to "true",
    )
  )

  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
}

sourceSets {
  named("main") {
    java {
      // OpenAPI generator output
      srcDir(layout.buildDirectory.dir("generated/src/gen/java"))
      // Quarkus code generation
      srcDir("build/classes/java/quarkus-generated-sources/open-api")
    }
  }
}

tasks.named("compileKotlin") { dependsOn(tasks.named("openApiGenerate")) }

tasks
  .register("applySemanticVersionPlugin") {
    group = "Versioning"
    description =
      "Applies the semantic-version plugin after the Kotlin build script model is prepared."

    dependsOn("prepareKotlinBuildScriptModel")
  }
  .apply { apply(plugin = "com.dipien.semantic-version") }

tasks.jacocoTestReport {
  dependsOn(tasks.test) // tests are required to run before generating the report
  reports { xml.required.set(true) }
}

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.named("compileJava") { dependsOn("quarkusGenerateCode") }

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
