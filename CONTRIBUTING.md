# Contributing guidelines

## Pull Request Checklist

Before sending your pull requests, make sure you followed this list:

- Read [Contributing Guidelines](CONTRIBUTING.md).
- Read [Code of Conduct](CODE_OF_CONDUCT.md).
- Refer to [Development process](#development-process).
- Kobby Gradle and Maven plugins have
  identical [public settings](https://github.com/ermadmi78/kobby/wiki/Available-Gradle-Plugin-Settings). If you change
  it, do it synchronously in both plugins.
- Build the project locally.
- Run Unit Tests and ensure that you have the test coverage for your code.

## Development process

Please follow the steps below in order to make the changes:

1. Clone the repository
1. Set the local java version to 11
1. Create a new developing branch for an issue.
1. Open repository in your favourite IDE.
1. Make code changes to the Kobby Plugin.
1. Build and install the Kobby Plugin locally.

   ```shell script
   # This will build and install both the Gradle and the Maven plugin 
   # to your local maven repository without test execution.
   ./gradlew clean build -x test --stacktrace
   ```

1. The easiest way to test and debug your changes is to use the Kobby Example Projects.

   First find `SNAPHOT` plugin version in
   the [gradle.properties](https://github.com/ermadmi78/kobby/blob/main/gradle.properties) file. It looks like:

   ```properties
   version = 1.1.1-SNAPSHOT
   ```

    1. Kobby Gradle Example

       Clone [Kobby Gradle Example](https://github.com/ermadmi78/kobby-gradle-example) project and
       change [build.gradle.kts](https://github.com/ermadmi78/kobby-gradle-example/blob/main/cinema-api/build.gradle.kts)
       file in `cinema-api` module.

       ```kotlin
       // Change from:
       plugins {
           kotlin("jvm")
           `java-library`
           id("io.github.ermadmi78.kobby") version "1.1.0"
       }
       ```

       ```kotlin
       // To:
       plugins {
           kotlin("jvm")
           `java-library`
       }

       buildscript {
           dependencies {
               classpath("io.github.ermadmi78:kobby-gradle-plugin:1.1.1-SNAPSHOT")
           }
       }

       apply(plugin = "io.github.ermadmi78.kobby")       
       ```

       Build the example:

       ```shell script
       ./gradlew clean build
       ```
    1. Kobby Maven Example

       Clone [Kobby Maven Example](https://github.com/ermadmi78/kobby-maven-example) project and
       change [pom.xml](https://github.com/ermadmi78/kobby-maven-example/blob/main/pom.xml) file.

       ```xml
        <!--  Change from: -->
        <properties>
            <!--  ... skipped -->
            <kobby.version>1.1.0</kobby.version>
            <!--  ... skipped -->
        </properties>
       ```

       ```xml
        <!--  To: -->
        <properties>
            <!--  ... skipped -->
            <kobby.version>1.1.1-SNAPSHOT</kobby.version>
            <!--  ... skipped -->
        </properties>
       ```

       Build the example:

       ```shell script
       ./mvnw clean package
       ```

1. Make sure that Kobby Example Projects are compiling and running.
1. Actualize old testcases

    1. Run this command to actualize old testcases:

       ```shell script
       ./gradlew clean build -Dkobby.testcases.actualize=true
       ```

    1. Look at the local changes in Git to make sure the generated code is correct for the old testcases.
    1. Commit actualized testcases.
    1. Rebuild the project to make sure the testcases are correct.

       ```shell script
       ./gradlew clean build --stacktrace
       ```
1. Please don't forget to write new testcases for new features!!!