rootProject.name = "kobby-gradle-tests"

dependencyResolutionManagement {
    versionCatalogs {
        create("testLibs") {
            from(files("libs.versions.toml"))
        }
    }
}

include(":cinema-jackson-simple")
include(":cinema-jackson-composite")
include(":cinema-serialization-noparentheses")

