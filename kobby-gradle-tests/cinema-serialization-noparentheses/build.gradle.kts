plugins {
    `kobby-conventions`
    alias(testLibs.plugins.kotlinx.serialization)
}

kobby {
    kotlin {
        scalars = mapOf(
            "Date" to typeOf("java.time", "LocalDate").serializer("serializers", "LocalDateSerializer"),
            "JSON" to typeOf("kotlinx.serialization.json", "JsonObject")
        )

        entity {
            projection {
                enableNotationWithoutParentheses = true
            }
        }

        dto {
            serialization {
                enabled = true
            }
        }

        adapter {
            extendedApi = true
            ktor {
                compositeEnabled = true
            }
        }
    }
}

dependencies {
    implementation(testLibs.ktor.cio)
    implementation(testLibs.ktor.content.negotiation)
    implementation(testLibs.ktor.serialization.kotlinx.json)
}
