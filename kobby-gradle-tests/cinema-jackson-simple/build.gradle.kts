plugins {
    `kobby-conventions`
}

kobby {
    kotlin {
        scalars = mapOf(
            "Date" to typeOf("java.time", "LocalDate"),
            "JSON" to typeMap.parameterize(typeString, typeAny.nullable())
        )
    }
}

dependencies {
    implementation(testLibs.ktor.cio)
    implementation(testLibs.ktor.content.negotiation)
    implementation(testLibs.ktor.serialization.jackson)
    implementation(testLibs.jackson.annotations)
}
