# PagoPA Payment Methods Handler

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pagopa_pagopa-ecommerce-payment-methods-handler&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=pagopa_pagopa-ecommerce-payment-methods-handler)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=pagopa_pagopa-ecommerce-payment-methods-handler&metric=coverage)](https://sonarcloud.io/summary/new_code?id=pagopa_pagopa-ecommerce-payment-methods-handler)

`pagopa-ecommerce-payment-methods-handler` is a high-performance, cloud-native microservice responsible for managing user payment methods within PagoPA e-commerce platform. Built with Quarkus and Kotlin, it is designed for exceptionally fast startup times, low memory consumption, and seamless scalability.

## Technology Stack

- Kotlin
- Quarkus

### Environment Variables

The following environment variables are used to configure the application.

| Variable                         | Description                                                                                                                                                     |
|:---------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **OpenTelemetry (OTel)**         |                                                                                                                                                                 |
| `OTEL_RESOURCE_ATTRIBUTES`       | A comma-separated list of key-value pairs that identify this service in observability platforms. For example, `service.name=my-app,deployment.environment=dev`. |
| `OTEL_EXPORTER_OTLP_ENDPOINT`    | The full URL of the OpenTelemetry Collector's OTLP endpoint where traces and metrics will be sent.                                                              |
| `OTEL_EXPORTER_OTLP_PROTOCOL`    | The protocol to use for the OTLP exporter. Common values are `grpc` (default) or `http/protobuf`.                                                               |
| `OTEL_LOGS_EXPORTER`             | Controls the log exporting pipeline. Set to `none` to disable sending logs via OTel.                                                                            |
| `OTEL_TRACES_SAMPLER`            | The sampling strategy for traces. `always_on` samples every trace, which is useful for development but not recommended for production.                          |
| **Logging (ECS)**                |                                                                                                                                                                 |
| `ECS_SERVICE_NAME`               | The name of this service as it should appear in structured logs (e.g., ECS JSON format).                                                                        |
| `ECS_SERVICE_ENVIRONMENT`        | The deployment environment (e.g., `dev`, `uat`, `prod`) for structured logs.                                                                                    |
| `ROOT_LOG_LEVEL`                 | The root logging level for the application (e.g., `INFO`, `DEBUG`, `WARN`).                                                                                     |
| **Security**                     |                                                                                                                                                                 |
| `SECURITY_API_KEY_SECURED_PATHS` | A comma-separated list of URL paths that are protected by the API Key filter (e.g., `/payment-methods`).                                                        |
| `SECURITY_API_KEY_PRIMARY`       | The primary API key that clients must provide to access secured paths.                                                                                          |
| `SECURITY_API_KEY_SECONDARY`     | A secondary/backup API key, often used for key rotation or for a different set of clients.                                                                      |
| **AFM Client**                   |                                                                                                                                                                 |
| `AFM_URI_V1`                     | The base URI for the external AFM service v1 APIs.                                                                                                              |
| `AFM_KEY`                        | The subscription key required to authenticate with the AFM service.                                                                                             |
| `AFM_CONNECTION_TIMEOUT`         | The timeout in milliseconds for establishing a connection to the AFM service.                                                                                   |
| `AFM_READ_TIMEOUT`               | The timeout in milliseconds for waiting for data after a connection to the AFM service has been established.                                                    |

An example configuration of these environment variables is in the `.env.example` file.

It is recommended to create a new .env file by copying the example one, using the following command (make sure you are
in the .env.example folder):

```shell
cp .env.example .env
```

Prepend this command to other commands in order to use the environment variables in the provided .env file:

```shell
env $(grep -v '^#' .env | xargs)
```

## Working with Windows

If you are developing on Windows, it is recommended the use of WSL2 combined with IntelliJ IDEA.

The IDE should be installed on Windows, with the repository cloned into a folder in WSL2. All the necessary tools will
be installed in the Linux distro of your choice.

You can find more info on how to set up the environment following the link below.

https://www.jetbrains.com/help/idea/how-to-use-wsl-development-environment-in-product.html

After setting up the WSL environment, you can test the application by building it through either Quarkus or Docker.

## Running the application in dev mode

The application can be run in dev mode that enables live coding using:

```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

To build an _über-jar_, execute the following command:

```shell script
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./gradlew build -Dquarkus.native.enabled=true
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/pagopa-ecommerce-payment-methods-handler-<version>-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/gradle-tooling>.

## Docker

The project can be built and run using Docker.jvm or Dockerfile.native files.

## Dependency lock

This feature use the content of `gradle.lockfile` to check the declared dependencies against the locked one.

If a transitive dependencies have been upgraded the build will fail because of the locked version mismatch.

The following command can be used to upgrade dependency lockfile:

```shell
./gradlew dependencies --write-locks 
```

Running the above command will cause the `gradle.lockfile` to be updated against the current project dependency
configuration

## Dependency verification

This feature is enabled by adding the gradle `./gradle/verification-metadata.xml` configuration file.

Perform checksum comparison against dependency artifact (jar files, zip, ...) and metadata (pom.xml, gradle module
metadata, ...) used during build
and the ones stored into `verification-metadata.xml` file raising error during build in case of mismatch.

The following command can be used to recalculate dependency checksum:

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies
```

In the above command the `clean`, `spotlessApply` `build` tasks where chosen to be run
in order to discover all transitive dependencies used during build and also the ones used during
spotless apply task used to format source code.

The above command will upgrade the `verification-metadata.xml` adding all the newly discovered dependencies' checksum.
Those checksum should be checked against a trusted source to check for corrispondence with the library author published
checksum.

`/gradlew --write-verification-metadata sha256` command appends all new dependencies to the verification files but does
not remove
entries for unused dependencies.

This can make this file grow every time a dependency is upgraded.

To detect and remove old dependencies make the following steps:

1. Delete, if present, the `gradle/verification-metadata.dryrun.xml`
2. Run the gradle write-verification-metadata in dry-mode (this will generate a verification-metadata-dryrun.xml file
   leaving untouched the original verification file)
3. Compare the verification-metadata file and the verification-metadata.dryrun one checking for differences and removing
   old unused dependencies

The 1-2 steps can be performed with the following commands

```Shell
rm -f ./gradle/verification-metadata.dryrun.xml 
./gradlew --write-verification-metadata sha256 clean spotlessApply build --dry-run
```

The resulting `verification-metadata.xml` modifications must be reviewed carefully checking the generated
dependencies checksum against official websites or other secure sources.

If a dependency is not discovered during the above command execution it will lead to build errors.

You can add those dependencies manually by modifying the `verification-metadata.xml`
file adding the following component:

```xml

<verification-metadata>
    <!-- other configurations... -->
    <components>
        <!-- other components -->
        <component group="GROUP_ID" name="ARTIFACT_ID" version="VERSION">
            <artifact name="artifact-full-name.jar">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
            <artifact name="artifact-pom-file.pom">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
        </component>
    </components>
</verification-metadata>
```

Add those components at the end of the components list and then run the

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies
```

that will reorder the file with the added dependencies checksum in the expected order.

Finally, you can add new dependencies both to gradle.lockfile writing verification metadata running

```shell
 ./gradlew dependencies --write-locks --write-verification-metadata sha256 --no-build-cache --refresh-dependencies
```

For more information read the
following [article](https://docs.gradle.org/8.1/userguide/dependency_verification.html#sec:checksum-verification)

## Contributors 👥

Made with ❤️ by PagoPA S.p.A.

### Maintainers

See `CODEOWNERS` file