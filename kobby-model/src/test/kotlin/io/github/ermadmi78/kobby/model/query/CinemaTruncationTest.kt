package io.github.ermadmi78.kobby.model.query

import org.junit.jupiter.api.Test

/**
 * Created on 23.03.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class CinemaTruncationTest {
    @Test
    fun testEmptyTruncation() = Cinema.schema.assertTruncation {
        // Nothing
    }.print(false).shouldExclude {
        // Nothing
    }

    @Test
    fun testIncludeAnyTypeTruncation() = Cinema.schema.assertTruncation {
        forAny {}
    }.print(false).shouldExclude {
        // Nothing
    }

    @Test
    fun testIncludeAnyFieldTruncation() = Cinema.schema.assertTruncation {
        forAny {
            include {
                field("*")
            }
        }
    }.print(false).shouldExclude {
        // Nothing
    }

    @Test
    fun testExcludeField() = Cinema.schema.assertTruncation {
        forQuery {
            exclude {
                field("country")
                field("countries")
            }
        }
    }.shouldExclude {
        objectFields("Query", "country", "countries")
    }

    @Test
    fun testIncludeField() = Cinema.schema.assertTruncation {
        forQuery {
            include {
                field("country")
                field("countries")
            }
        }
    }.print(false).shouldExclude {
        objectFields("Query", "film", "films", "actor", "actors", "taggable")
    }

    @Test
    fun testExcludeFieldByPattern() = Cinema.schema.assertTruncation {
        forRoot {
            exclude {
                field("*countr*|*Countr*")
            }
        }
    }.print(false).shouldExclude {
        objectFields("Query", "country", "countries")
        objectFields("Mutation", "createCountry")
        objectFields("Subscription", "countryCreated")
    }

    @Test
    fun testIncludeFieldByPattern() = Cinema.schema.assertTruncation {
        forRoot {
            include {
                field("*countr*|*Countr*")
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Query", "film", "films", "actor", "actors", "taggable")
        objectFields(
            "Mutation",
            "createFilm", "createActor", "updateBirthday", "associate", "tagFilm", "tagActor"
        )
        objectFields("Subscription", "filmCreated", "actorCreated")
    }

    @Test
    fun testExcludeTypeField() = Cinema.schema.assertTruncation {
        forType("Country") {
            exclude {
                field("actor")
                field("actors")
            }
        }
    }.print(false).shouldExclude {
        objectFields("Country", "actor", "actors")
    }

    @Test
    fun testTryToExcludeOverriddenObjectField() = Cinema.schema.assertTruncation {
        forType("Country") {
            exclude {
                field("id")
            }
        }
    }.print(false).shouldExclude {
        // Nothing
    }

    @Test
    fun testTryToExcludeOverriddenInterfaceField() = Cinema.schema.assertTruncation {
        forType("Taggable") {
            exclude {
                field("id")
            }
        }
    }.print(false).shouldExclude {
        // Nothing
    }

    @Test
    fun testExcludeInterfaceField() = Cinema.schema.assertTruncation {
        forType("Entity") {
            exclude {
                field("id")
            }
        }
    }.print(false).shouldExclude {
        objectFields("Country", "id")
        objectFields("Film", "id")
        objectFields("Actor", "id")
        interfaceFields("Entity", "id")
        interfaceFields("Taggable", "id")
    }

    @Test
    fun testIncludeOverriddenFieldsOnly() = Cinema.schema.assertTruncation {
        forType("Actor") {
            include {
                anyOverriddenField()
            }
        }
    }.print(false).shouldExclude {
        objectFields(
            "Actor",
            "firstName", "lastName", "birthday", "gender", "countryId", "country", "films"
        )
    }

    @Test
    fun testExcludeRootDependency() = Cinema.schema.assertTruncation {
        forRoot {
            exclude {
                dependency("Country")
            }
        }
    }.print(false).shouldExclude {
        objectFields("Query", "country", "countries")
        objectFields("Mutation", "createCountry")
        objectFields("Subscription", "countryCreated")
    }

    @Test
    fun testIncludeRootDependency() = Cinema.schema.assertTruncation {
        forRoot {
            include {
                dependency("Country")
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Query", "film", "films", "actor", "actors", "taggable")
        objectFields(
            "Mutation",
            "createFilm", "createActor", "updateBirthday", "associate", "tagFilm", "tagActor"
        )
        objectFields("Subscription", "filmCreated", "actorCreated")
    }

    @Test
    fun testExcludeInterfaceScalarDependency() = Cinema.schema.assertTruncation {
        forType("Entity") {
            exclude {
                dependency("JSON")
            }
        }
    }.print(false).shouldExclude {
        scalars("JSON")
        objectFields("Country", "fields")
        objectFields("Film", "fields")
        objectFields("Actor", "fields")
        interfaceFields("Entity", "fields")
        interfaceFields("Taggable", "fields")
    }

    @Test
    fun testExcludeAnyCountryDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                dependency("Country")
            }
        }
    }.print(false).shouldExclude {
        objects("Country")
        objectFields("Query", "country", "countries")
        objectFields("Mutation", "createCountry")
        objectFields("Subscription", "countryCreated")
        objectFields("Film", "country")
        objectFields("Actor", "country")
    }

    @Test
    fun testExcludeAnyFilmDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                dependency("Film")
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput")
        objectFields("Query", "film", "films")
        objectFields("Mutation", "createFilm")
        objectFields("Subscription", "filmCreated")
        objectFields("Country", "film", "films")
        objectFields("Actor", "films")
    }

    @Test
    fun testExcludeAnyFilmSubTypeDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                subTypeDependency("Film")
            }
        }
    }.print(false).shouldExclude {
        objectFields("Query", "taggable")
        objectFields("Country", "taggable", "native")
    }

    @Test
    fun testExcludeAnyFilmDirectAndSubTypeDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                dependency("Film")
                subTypeDependency("Film")
            }
        }
    }.print(false).shouldExclude {
        objects("Film")
        enums("Genre")
        inputs("FilmInput")
        objectFields("Query", "film", "films", "taggable")
        objectFields("Mutation", "createFilm")
        objectFields("Subscription", "filmCreated")
        objectFields("Country", "film", "films", "taggable", "native")
        objectFields("Actor", "films")
    }

    @Test
    fun testExcludeAnyActorDirectAndSubTypeDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                dependency("Actor")
                subTypeDependency("Actor")
            }
        }
    }.print(false).shouldExclude {
        scalars("Date")
        objects("Actor")
        enums("Gender")
        inputs("ActorInput")
        objectFields("Query", "actor", "actors", "taggable")
        objectFields("Mutation", "createActor", "updateBirthday")
        objectFields("Subscription", "actorCreated")
        objectFields("Country", "actor", "actors", "taggable", "native")
        objectFields("Film", "actors")
    }

    @Test
    fun testExcludeAnyFilmAndActorDirectAndSubTypeDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                dependency("Film|Actor")
                subTypeDependency("Film|Actor")
            }
        }
    }.print(false).shouldExclude {
        scalars("Date")
        objects("Film", "Actor", "Tag")
        interfaces("Taggable")
        unions("Native")
        enums("Genre", "Gender")
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Query", "film", "films", "actor", "actors", "taggable")
        objectFields("Mutation", "createFilm", "createActor", "updateBirthday")
        objectFields("Subscription", "filmCreated", "actorCreated")
        objectFields("Country", "film", "films", "actor", "actors", "taggable", "native")
    }

    @Test
    fun testExcludeAnyTaggableDirectDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                dependency("Taggable")
            }
        }
    }.print(false).shouldExclude {
        objectFields("Query", "taggable")
        objectFields("Country", "taggable")
    }

    @Test
    fun testExcludeAnyTaggableSuperTypeDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                superTypeDependency("Taggable")
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Query", "film", "films", "actor", "actors")
        objectFields("Mutation", "createFilm", "createActor", "updateBirthday")
        objectFields("Subscription", "filmCreated", "actorCreated")
        objectFields("Country", "film", "films", "actor", "actors")
        objectFields("Film", "actors")
        objectFields("Actor", "films")
    }

    @Test
    fun testExcludeAnyTaggableDirectAndSuperTypeDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                dependency("Taggable")
                superTypeDependency("Taggable")
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Query", "film", "films", "actor", "actors", "taggable")
        objectFields("Mutation", "createFilm", "createActor", "updateBirthday")
        objectFields("Subscription", "filmCreated", "actorCreated")
        objectFields("Country", "film", "films", "actor", "actors", "taggable")
        objectFields("Film", "actors")
        objectFields("Actor", "films")
    }

    @Test
    fun testExcludeAnyNativeDirectDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                dependency("Native")
            }
        }
    }.print(false).shouldExclude {
        objectFields("Country", "native")
    }

    @Test
    fun testExcludeAnyNativeSuperTypeDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                superTypeDependency("Native")
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Query", "film", "films", "actor", "actors")
        objectFields("Mutation", "createFilm", "createActor", "updateBirthday")
        objectFields("Subscription", "filmCreated", "actorCreated")
        objectFields("Country", "film", "films", "actor", "actors")
        objectFields("Film", "actors")
        objectFields("Actor", "films")
    }

    @Test
    fun testExcludeAnyNativeDirectAndSuperTypeDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                dependency("Native")
                superTypeDependency("Native")
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Query", "film", "films", "actor", "actors")
        objectFields("Mutation", "createFilm", "createActor", "updateBirthday")
        objectFields("Subscription", "filmCreated", "actorCreated")
        objectFields("Country", "film", "films", "actor", "actors", "native")
        objectFields("Film", "actors")
        objectFields("Actor", "films")
    }

    @Test
    fun testExcludeAnyTaggableAndNativeDirectAndSuperTypeDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                dependency("Taggable|Native")
                superTypeDependency("Taggable|Native")
            }
        }
    }.print(false).shouldExclude {
        scalars("Date")
        objects("Film", "Actor", "Tag")
        interfaces("Taggable")
        unions("Native")
        enums("Genre", "Gender")
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Query", "film", "films", "actor", "actors", "taggable")
        objectFields("Mutation", "createFilm", "createActor", "updateBirthday")
        objectFields("Subscription", "filmCreated", "actorCreated")
        objectFields("Country", "film", "films", "actor", "actors", "taggable", "native")
    }

    @Test
    fun testExcludeDirectDependencyForInterface() = Cinema.schema.assertTruncation {
        forType("Entity") {
            exclude {
                dependency("JSON")
            }
        }
    }.print(false).shouldExclude {
        scalars("JSON")
        objectFields("Country", "fields")
        objectFields("Film", "fields")
        objectFields("Actor", "fields")
        interfaceFields("Entity", "fields")
        interfaceFields("Taggable", "fields")
    }

    @Test
    fun testExcludeAnyScalarAndEnumDependencyForObject() = Cinema.schema.assertTruncation {
        forType("Film") {
            exclude {
                anyScalarDependency()
            }
        }
        forType("Actor") {
            exclude {
                anyEnumDependency()
            }
        }
    }.print(false).shouldExclude {
        objectFields("Film", "title", "countryId")
        objectFields("Actor", "gender")
    }

    @Test
    fun testExcludeAnyObjectInterfaceUnionDependencyForObject() = Cinema.schema.assertTruncation {
        forType("Film|Actor") {
            exclude {
                anyObjectDependency()
            }
        }
        forQuery {
            exclude {
                anyInterfaceDependency()
            }
        }
        forType("Country") {
            exclude {
                anyUnionDependency()
            }
        }
    }.print(false).shouldExclude {
        objectFields("Query", "taggable")
        objectFields("Country", "native")
        objectFields("Film", "country", "actors")
        objectFields("Actor", "country", "films")
    }

    @Test
    fun testExcludeAnyObjectDependencyForInterface() = Cinema.schema.assertTruncation {
        forType("Taggable") {
            exclude {
                anyObjectDependency()
            }
        }
    }.print(false).shouldExclude {
        objects("Tag")
        objectFields("Film", "tags")
        objectFields("Actor", "tags")
        interfaceFields("Taggable", "tags")
    }

    @Test
    fun testExcludeArgumentDependencyForObject() = Cinema.schema.assertTruncation {
        forType("Country") {
            exclude {
                argumentDependency("Date")
            }
        }
    }.print(false).shouldExclude {
        objectFields("Country", "actors")
    }

    @Test
    fun testExcludeAnyScalarEnumInputArgumentDependencyForObject() = Cinema.schema.assertTruncation {
        forType("Film|Actor") {
            exclude {
                anyScalarArgumentDependency()
            }
        }
        forQuery {
            exclude {
                anyEnumArgumentDependency()
            }
        }
        forMutation {
            exclude {
                anyInputArgumentDependency()
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Query", "films", "actors")
        objectFields("Mutation", "createFilm", "createActor")
        objectFields("Film", "actors")
        objectFields("Actor", "films")
    }

    @Test
    fun testExcludeAnyTransitiveTagDependency() = Cinema.schema.assertTruncation {
        forAny {
            exclude {
                transitiveDependency("Tag")
            }
        }
    }.print(false).shouldExclude {
        scalars("JSON", "Date")
        objects("Country", "Film", "Actor", "Tag")
        interfaces("Entity", "Taggable")
        unions("Native")
        enums("Genre", "Gender")
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Query", "country", "countries", "film", "films", "actor", "actors", "taggable")
        objectFields("Mutation", "createCountry", "createFilm", "createActor", "updateBirthday")
        objectFields("Subscription", "countryCreated", "filmCreated", "actorCreated")
    }

    @Test
    fun testExcludeMutationTransitiveTagInputDependency() = Cinema.schema.assertTruncation {
        forMutation {
            exclude {
                transitiveDependency("TagInput")
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Mutation", "createFilm", "createActor")
    }

    @Test
    fun testIncludeMinWeight() = Cinema.schema.assertTruncation {
        forMutation {
            include {
                minWeight(9)
            }
        }
    }.print(false).shouldExclude {
        objectFields("Mutation", "associate", "tagFilm", "tagActor")
    }

    @Test
    fun testIncludeMaxWeight() = Cinema.schema.assertTruncation {
        forMutation {
            include {
                maxWeight(9)
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Mutation", "createFilm", "createActor")
    }

    @Test
    fun testIncludeMinMaxWeight() = Cinema.schema.assertTruncation {
        forMutation {
            include {
                minWeight(9)
                maxWeight(11)
            }
        }
    }.print(false).shouldExclude {
        objectFields("Mutation", "associate", "tagFilm", "tagActor")
    }

    @Test
    fun testExcludeEnumValue() = Cinema.schema.assertTruncation {
        forType("Genre") {
            exclude {
                enumValue("DRAMA|COMEDY")
            }
        }
    }.print(false).shouldExclude {
        enumValues("Genre", "COMEDY")
    }

    @Test
    fun testExcludeEnumValueWithDefault() = Cinema.schema.assertTruncation {
        forType("Genre") {
            exclude {
                enumValue("DRAMA|COMEDY")
            }
        }
        forMutation {
            exclude {
                argumentDependency("FilmInput")
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput")
        objectFields("Mutation", "createFilm")
        enumValues("Genre", "DRAMA", "COMEDY")
    }

    @Test
    fun testAliasInPattern() = Cinema.schema.assertTruncation {
        forType("${KobbyTypeAlias.QUERY}|${KobbyTypeAlias.MUTATION}") {
            exclude {
                argumentDependency("${KobbyTypeAlias.ANY_ENUM}|${KobbyTypeAlias.ANY_INPUT}")
            }
        }
    }.print(false).shouldExclude {
        inputs("FilmInput", "ActorInput", "TagInput")
        objectFields("Query", "films", "actors")
        objectFields("Mutation", "createFilm", "createActor")
    }
}