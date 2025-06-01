package io.github.ermadmi78.kobby.task

import io.github.ermadmi78.kobby.generator.kotlin.*
import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.PREDEFINED_SCALARS
import io.github.ermadmi78.kobby.model.Decoration
import io.github.ermadmi78.kobby.model.KobbyDirective
import io.github.ermadmi78.kobby.model.PluginUtils.contextName
import io.github.ermadmi78.kobby.model.PluginUtils.extractCommonPrefix
import io.github.ermadmi78.kobby.model.PluginUtils.forEachPackage
import io.github.ermadmi78.kobby.model.PluginUtils.pathIterator
import io.github.ermadmi78.kobby.model.PluginUtils.removePrefixOrEmpty
import io.github.ermadmi78.kobby.model.PluginUtils.toPackageName
import io.github.ermadmi78.kobby.model._capitalize
import io.github.ermadmi78.kobby.model.parseSchema
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import java.io.File
import java.io.FileReader

/**
 * Created on 02.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
open class KobbyKotlin : DefaultTask() {
    companion object {
        const val TASK_NAME = "kobbyKotlin"
    }

    @InputFiles
    val schemaFiles: ListProperty<RegularFile> = project.objects.listProperty(RegularFile::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaScanDir",
        description = "Root directory to scan schema files (default \"src/main/resources\")"
    )
    val schemaScanDir: Property<String> = project.objects.property(String::class.java)

    private val schemaScanDirFile: File

    /**
     * ANT style include patterns to scan schema files (default `**`/`*`.graphqls)
     */
    @Input
    @Optional
    val schemaScanIncludes: ListProperty<String> = project.objects.listProperty(String::class.java)

    /**
     * ANT style exclude patterns to scan schema files (default empty)
     */
    @Input
    @Optional
    val schemaScanExcludes: ListProperty<String> = project.objects.listProperty(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectivePrimaryKey",
        description = "Name of \"primaryKey\" directive (default \"primaryKey\")"
    )
    val schemaDirectivePrimaryKey: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveRequired",
        description = "Name of \"required\" directive (default \"required\")"
    )
    val schemaDirectiveRequired: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveDefault",
        description = "Name of \"default\" directive (default \"default\")"
    )
    val schemaDirectiveDefault: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveSelection",
        description = "Name of \"selection\" directive (default \"selection\")"
    )
    val schemaDirectiveSelection: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaTruncateReportEnabled",
        description = "Print detailed schema truncation report to console (default false)"
    )
    val schemaTruncateReportEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaTruncateRegexEnabled",
        description = "true - use Regex in truncation query, false - use Kobby Pattern " +
                "(? - matches one character, * - matches zero or more characters, | - OR operator). " +
                "Default false."
    )
    val schemaTruncateRegexEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaTruncateCaseSensitive",
        description = "Are patterns used in a GraphQL schema truncation query case sensitive (default true)"
    )
    val schemaTruncateCaseSensitive: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    val schemaTruncateQuery: MapProperty<String, KobbyTypeOperationQuery> =
        project.objects.mapProperty(String::class.java, KobbyTypeOperationQuery::class.java)

    @Input
    @Optional
    val scalars: MapProperty<String, KotlinType> =
        project.objects.mapProperty(String::class.java, KotlinType::class.java)


    @Input
    @Optional
    @Option(
        option = "relativePackage",
        description = "Is root package name for generated DSL " +
                "should be relative to GraphQL schema directory (default true)"
    )
    val relativePackage: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "rootPackageName",
        description = "Root package name for generated DSL (default \"kobby.kotlin\")"
    )
    val rootPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextPackageName",
        description = "Context package name relative to root package name (default null)"
    )
    val contextPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextName",
        description = "Name of generated DSL context By default is name of GraphQL schema file " +
                "or \"graphql\" if there are multiple schema files"
    )
    val contextName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextPrefix",
        description = "Prefix of generated \"Context\" interface. By default is capitalized context name."
    )
    val contextPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextPostfix",
        description = "Postfix of generated \"Context\" interface (default null)"
    )
    val contextPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextQuery",
        description = "Name of \"query\" function in \"Context\" interface (default \"query\")"
    )
    val contextQuery: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextMutation",
        description = "Name of \"mutation\" function in \"Context\" interface (default \"mutation\")"
    )
    val contextMutation: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextSubscription",
        description = "Name of \"subscription\" function in \"Context\" interface (default \"subscription\")"
    )
    val contextSubscription: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextCommitEnabled",
        description = "Is generation of the commit function enabled for subscription (default false)"
    )
    val contextCommitEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoPackageName",
        description = "Package name for DTO classes relative to root package name (default \"dto\")"
    )
    val dtoPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoPrefix",
        description = "Prefix of DTO classes generated from GraphQL objects, interfaces and unions (default null)"
    )
    val dtoPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoPostfix",
        description = "Postfix of DTO classes generated from GraphQL objects, interfaces and unions (default \"Dto\")"
    )
    val dtoPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoEnumPrefix",
        description = "Prefix of DTO classes generated from GraphQL enums (default null)"
    )
    val dtoEnumPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoEnumPostfix",
        description = "Postfix of DTO classes generated from GraphQL enums (default null)"
    )
    val dtoEnumPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoInputPrefix",
        description = "Prefix of DTO classes generated from GraphQL inputs (default null)"
    )
    val dtoInputPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoInputPostfix",
        description = "Postfix of DTO classes generated from GraphQL inputs (default null)"
    )
    val dtoInputPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoApplyPrimaryKeys",
        description = "Generate equals and hashCode for DTO classes by @primaryKey directive (default false)"
    )
    val dtoApplyPrimaryKeys: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoMaxNumberOfFieldsForImmutableDtoClass",
        description = "Generate immutable DTO class if number of GraphQL type fields <= property value (default 245)"
    )
    val dtoMaxNumberOfFieldsForImmutableDtoClass: Property<Int> = project.objects.property(Int::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoMaxNumberOfFieldsForImmutableInputClass",
        description = "Generate immutable Input class if number of GraphQL Input type fields <= property value (default 245)"
    )
    val dtoMaxNumberOfFieldsForImmutableInputClass: Property<Int> = project.objects.property(Int::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoSerializationEnabled",
        description = "Enable Kotlinx Serialization in generated DTO classes (default false)"
    )
    val dtoSerializationEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoSerializationClassDiscriminator",
        description = "Name of the class descriptor property for polymorphic serialization (default \"__typename\")"
    )
    val dtoSerializationClassDiscriminator: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoSerializationIgnoreUnknownKeys",
        description = "Specifies whether encounters of unknown properties in the input JSON should be ignored " +
                "instead of throwing SerializationException (default true)"
    )
    val dtoSerializationIgnoreUnknownKeys: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoSerializationEncodeDefaults",
        description = "Specifies whether default values of Kotlin properties should be encoded to JSON (default false)"
    )
    val dtoSerializationEncodeDefaults: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoSerializationPrettyPrint",
        description = "Specifies whether resulting JSON should be pretty-printed (default false)"
    )
    val dtoSerializationPrettyPrint: Property<Boolean> = project.objects.property(Boolean::class.java)


    @Input
    @Optional
    @Option(
        option = "dtoJacksonEnabled",
        description = "Add Jackson annotations for generated DTO classes (default true)"
    )
    val dtoJacksonEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoJacksonTypeInfoUse",
        description = "Customize the @JsonTypeInfo annotation's `use` property (default \"NAME\")"
    )
    val dtoJacksonTypeInfoUse: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoJacksonTypeInfoInclude",
        description = "Customize the @JsonTypeInfo annotation's `include` property (default \"PROPERTY\")"
    )
    val dtoJacksonTypeInfoInclude: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoJacksonTypeInfoProperty",
        description = "Customize the @JsonTypeInfo annotation's `property` property (default \"__typename\")"
    )
    val dtoJacksonTypeInfoProperty: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoJacksonJsonInclude",
        description = "Customize the @JsonInclude annotation's `value` property (default \"NON_ABSENT\")"
    )
    val dtoJacksonJsonInclude: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderEnabled",
        description = "Is DTO builders generation enabled (default true)"
    )
    val dtoBuilderEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderPrefix",
        description = "Prefix of DTO builder classes (default null)"
    )
    val dtoBuilderPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderPostfix",
        description = "Postfix of DTO builder classes (default \"Dto\")"
    )
    val dtoBuilderPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderToBuilderFun",
        description = "Name of DTO based \"toBuilder\" function for DTO classes (default \"toBuilder\")"
    )
    val dtoBuilderToBuilderFun: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderToDtoFun",
        description = "Name of builder based \"toDto\" function for DTO classes (default \"toDto\")"
    )
    val dtoBuilderToDtoFun: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderToInputFun",
        description = "Name of builder based \"toInput\" function for DTO input classes (default \"toInput\")"
    )
    val dtoBuilderToInputFun: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderCopyFun",
        description = "Name of builder based \"copy\" function for DTO classes (default \"copy\")"
    )
    val dtoBuilderCopyFun: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLEnabled",
        description = "Is helper DTO classes generation enabled (default true)"
    )
    val dtoGraphQLEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLPackageName",
        description = "Package name for helper DTO classes relative to DTO package name (default \"graphql\")"
    )
    val dtoGraphQLPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLPrefix",
        description = "Prefix for helper DTO classes (default \"<Context name>\")"
    )
    val dtoGraphQLPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLPostfix",
        description = "Postfix for helper DTO classes (default null)"
    )
    val dtoGraphQLPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityEnabled",
        description = "Is entities interfaces generation enabled (default true)"
    )
    val entityEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "entityPackageName",
        description = "Package name for entities interfaces relative to root package name (default \"entity\")"
    )
    val entityPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityPrefix",
        description = "Prefix for entities interfaces (default null)"
    )
    val entityPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityPostfix",
        description = "Postfix for entities interfaces (default null)"
    )
    val entityPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityErrorsFunName",
        description = "GraphQL response errors access function generated for adapters with extended API. " +
                "See \"adapter.extendedApi\" and \"adapter.throwException\" properties. (default \"__errors\")"
    )
    val entityErrorsFunName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityExtensionsFunName",
        description = "GraphQL response extensions access function generated for adapters with extended API. " +
                "See \"adapter.extendedApi\" property. (default \"__extensions\")"
    )
    val entityExtensionsFunName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityContextFunEnabled",
        description = "Generate context access function in entity interface (default true)"
    )
    val entityContextFunEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "entityContextFunName",
        description = "Context access function name in entity interface (default \"__context\")"
    )
    val entityContextFunName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithCurrentProjectionFun",
        description = "Name of \"withCurrentProjection\" function in entity interface " +
                "(default \"__withCurrentProjection\")"
    )
    val entityWithCurrentProjectionFun: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityProjectionPrefix",
        description = "Prefix for projection interfaces (default null)"
    )
    val entityProjectionPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityProjectionPostfix",
        description = "Postfix for projection interfaces (default \"Projection\")"
    )
    val entityProjectionPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityProjectionArgument",
        description = "Name of projection argument in field functions (default \"__projection\")"
    )
    val entityProjectionArgument: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithPrefix",
        description = "Prefix for projection fields that are not marked with the directive \"@default\" (default null)"
    )
    val entityWithPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithPostfix",
        description = "Postfix for projection fields that are not marked with the directive \"@default\" (default null)"
    )
    val entityWithPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithoutPrefix",
        description = "Prefix for default projection fields - marked with the directive \"@default\"" +
                " (default \"__without\")"
    )
    val entityWithoutPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithoutPostfix",
        description = "Postfix for default projection fields (marked with the directive \"@default\") (default null)"
    )
    val entityWithoutPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityMinimizeFun",
        description = "Name of \"minimize\" function in projection interface (default \"__minimize\")"
    )
    val entityMinimizeFun: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualificationPrefix",
        description = "Prefix for qualification interfaces (default null)"
    )
    val entityQualificationPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualificationPostfix",
        description = "Postfix for qualification interfaces (default \"Qualification\")"
    )
    val entityQualificationPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualifiedProjectionPrefix",
        description = "Prefix for qualified projection interface (default null)"
    )
    val entityQualifiedProjectionPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualifiedProjectionPostfix",
        description = "Postfix for qualified projection interface (default \"QualifiedProjection\")"
    )
    val entityQualifiedProjectionPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityOnPrefix",
        description = "Prefix for qualification functions (default \"__on\")"
    )
    val entityOnPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityOnPostfix",
        description = "Postfix for qualification functions (default null)"
    )
    val entityOnPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entitySelectionPrefix",
        description = "Prefix for selection interfaces (default null)"
    )
    val entitySelectionPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entitySelectionPostfix",
        description = "Postfix for selection interfaces (default \"Selection\")"
    )
    val entitySelectionPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entitySelectionArgument",
        description = "Name of selection argument in field functions (default \"__selection\")"
    )
    val entitySelectionArgument: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQueryPrefix",
        description = "Prefix for query interfaces (default null)"
    )
    val entityQueryPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQueryPostfix",
        description = "Postfix for query interfaces (default \"Query\")"
    )
    val entityQueryPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQueryArgument",
        description = "Name of query argument in field functions (default \"__query\")"
    )
    val entityQueryArgument: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implPackageName",
        description = "Package name for entities implementation classes " +
                "relative to root package name (default \"impl\")"
    )
    val implPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implPrefix",
        description = "Prefix for entities implementation classes (default null)"
    )
    val implPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implPostfix",
        description = "Postfix for entities implementation classes (default \"Impl\")"
    )
    val implPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implInternal",
        description = "Is implementation classes should be internal (default true)"
    )
    val implInternal: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "implInnerPrefix",
        description = "Prefix for inner fields in implementation classes (default \"__inner\")"
    )
    val implInnerPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implInnerPostfix",
        description = "Postfix for inner fields in implementation classes (default \"Impl\")"
    )
    val implInnerPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterExtendedApi",
        description = "Is extended adapter API (with GraphQL errors and extensions) enabled (default false)"
    )
    val adapterExtendedApi: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterThrowException",
        description = "Throw exception when receiving non-empty GraphQL errors (default true)"
    )
    val adapterThrowException: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterKtorSimpleEnabled",
        description = "Is simple Ktor adapter generation enabled (default false)"
    )
    val adapterKtorSimpleEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterKtorCompositeEnabled",
        description = "Is composite Ktor adapter generation enabled (default false)"
    )
    val adapterKtorCompositeEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterKtorPackageName",
        description = "Package name for Ktor adapter classes relative to root package name (default \"adapter.ktor\")"
    )
    val adapterKtorPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterKtorPrefix",
        description = "Prefix for Ktor adapter classes (default \"<Context name>\")"
    )
    val adapterKtorPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterKtorPostfix",
        description = "Postfix for Ktor adapter classes (default \"KtorAdapter\")"
    )
    val adapterKtorPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterKtorReceiveTimeoutMillis",
        description = "Default receive message timeout in milliseconds for subscriptions " +
                "in Ktor composite adapter (default 10000). Set it to zero or a negative value to disable it."
    )
    val adapterKtorReceiveTimeoutMillis: Property<Long> = project.objects.property(Long::class.java)

    @Input
    @Optional
    @Option(
        option = "enableNotationWithoutParentheses",
        description = "Is notation without parentheses for projection field getters enabled (default false)"
    )
    val enableNotationWithoutParentheses: Property<Boolean> = project.objects.property(Boolean::class.java)

    @OutputDirectory
    val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

    init {
        group = "kobby"
        description = "Generate Kotlin DSL client by GraphQL schema"

        schemaFiles.convention(project.provider<Iterable<RegularFile>> {
            project.fileTree(schemaScanDir.get()) {
                it.include(schemaScanIncludes.get())
                it.exclude(schemaScanExcludes.get())
            }.filter { it.isFile }.files.map {
                project.layout.file(project.provider { it }).get()
            }
        })

        schemaScanDir.convention("src/main/resources")
        schemaScanDirFile = project.file(schemaScanDir.get()).absoluteFile
        schemaScanIncludes.convention(listOf("**/*.graphqls"))
        schemaScanExcludes.convention(listOf())

        schemaDirectivePrimaryKey.convention(KobbyDirective.PRIMARY_KEY)
        schemaDirectiveRequired.convention(KobbyDirective.REQUIRED)
        schemaDirectiveDefault.convention(KobbyDirective.DEFAULT)
        schemaDirectiveSelection.convention(KobbyDirective.SELECTION)

        schemaTruncateReportEnabled.convention(false)
        schemaTruncateRegexEnabled.convention(false)
        schemaTruncateCaseSensitive.convention(true)

        scalars.convention(PREDEFINED_SCALARS)

        relativePackage.convention(true)
        rootPackageName.convention("kobby.kotlin")

        contextQuery.convention("query")
        contextMutation.convention("mutation")
        contextSubscription.convention("subscription")
        contextCommitEnabled.convention(false)

        dtoPackageName.convention("dto")
        dtoPostfix.convention("Dto")
        dtoApplyPrimaryKeys.convention(false)
        dtoMaxNumberOfFieldsForImmutableDtoClass.convention(245)
        dtoMaxNumberOfFieldsForImmutableInputClass.convention(245)

        dtoSerializationEnabled.convention(project.provider {
            project.hasDependency("org.jetbrains.kotlinx", "kotlinx-serialization-json")
        })
        dtoSerializationClassDiscriminator.convention("__typename")
        dtoSerializationIgnoreUnknownKeys.convention(true)
        dtoSerializationEncodeDefaults.convention(false)
        dtoSerializationPrettyPrint.convention(false)

        dtoJacksonEnabled.convention(project.provider {
            project.hasDependency("com.fasterxml.jackson.core", "jackson-annotations")
        })
        dtoJacksonTypeInfoUse.convention("NAME")
        dtoJacksonTypeInfoInclude.convention("PROPERTY")
        dtoJacksonTypeInfoProperty.convention("__typename")
        dtoJacksonJsonInclude.convention("NON_ABSENT")

        dtoBuilderEnabled.convention(true)
        dtoBuilderPostfix.convention("Builder")
        dtoBuilderToBuilderFun.convention("toBuilder")
        dtoBuilderToDtoFun.convention("toDto")
        dtoBuilderToInputFun.convention("toInput")
        dtoBuilderCopyFun.convention("copy")
        dtoGraphQLEnabled.convention(true)
        dtoGraphQLPackageName.convention("graphql")

        entityEnabled.convention(true)
        entityPackageName.convention("entity")
        entityErrorsFunName.convention("__errors")
        entityExtensionsFunName.convention("__extensions")
        entityContextFunEnabled.convention(true)
        entityContextFunName.convention("__context")
        entityWithCurrentProjectionFun.convention("__withCurrentProjection")
        entityProjectionPostfix.convention("Projection")
        entityProjectionArgument.convention("__projection")
        entityWithoutPrefix.convention("__without")
        entityMinimizeFun.convention("__minimize")
        entityQualificationPostfix.convention("Qualification")
        entityQualifiedProjectionPostfix.convention("QualifiedProjection")
        entityOnPrefix.convention("__on")
        entitySelectionPostfix.convention("Selection")
        entitySelectionArgument.convention("__selection")
        entityQueryPostfix.convention("Query")
        entityQueryArgument.convention("__query")

        implPackageName.convention("entity.impl")
        implPostfix.convention("Impl")
        implInternal.convention(true)
        implInnerPrefix.convention("__inner")

        adapterExtendedApi.convention(false)
        adapterThrowException.convention(true)

        adapterKtorSimpleEnabled.convention(project.provider {
            project.hasDependency("io.ktor", "ktor-client-cio")
        })
        adapterKtorCompositeEnabled.convention(project.provider {
            project.hasDependency("io.ktor", "ktor-client-cio")
        })
        adapterKtorPackageName.convention("adapter.ktor")
        adapterKtorPostfix.convention("KtorAdapter")
        adapterKtorReceiveTimeoutMillis.convention(10_000L)
        enableNotationWithoutParentheses.convention(false)

        outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/kobby/main/kotlin"))
    }

    @TaskAction
    fun generateKotlinDslClientAction() {
        val graphQLSchemaFiles: List<File> = schemaFiles.get().map {
            it.asFile.absoluteFile.also { file ->
                if (!file.isFile) {
                    "Specified schema file does not exist: $it".throwIt()
                }
            }
        }

        if (graphQLSchemaFiles.isEmpty()) {
            "GraphQL schema files not found".throwIt()
        }

        val directiveLayout = mapOf(
            KobbyDirective.PRIMARY_KEY to schemaDirectivePrimaryKey.get(),
            KobbyDirective.REQUIRED to schemaDirectiveRequired.get(),
            KobbyDirective.DEFAULT to schemaDirectiveDefault.get(),
            KobbyDirective.SELECTION to schemaDirectiveSelection.get()
        )

        val contextName = (contextName.orNull ?: graphQLSchemaFiles.singleOrNull()?.contextName)
            ?.filter { it.isJavaIdentifierPart() }
            ?.takeIf { it.firstOrNull()?.isJavaIdentifierStart() ?: false }
            ?: "graphql"
        val capitalizedContextName = contextName._capitalize()

        val rootPackage: List<String> = mutableListOf<String>().also { list ->
            if (relativePackage.get()) {
                graphQLSchemaFiles
                    .map { it.parent.pathIterator() }
                    .extractCommonPrefix()
                    .removePrefixOrEmpty(schemaScanDirFile.path.pathIterator())
                    .forEach {
                        list += it
                    }
            }
            rootPackageName.orNull?.forEachPackage { list += it }
        }

        val contextPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            contextPackageName.orNull?.forEachPackage { list += it }
        }

        val dtoPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            dtoPackageName.orNull?.forEachPackage { list += it }
        }

        val dtoGraphQLPackage: List<String> = mutableListOf<String>().also { list ->
            list += dtoPackage
            dtoGraphQLPackageName.orNull?.forEachPackage { list += it }
        }

        val entityPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            entityPackageName.orNull?.forEachPackage { list += it }
        }

        val implPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            implPackageName.orNull?.forEachPackage { list += it }
        }

        val adapterKtorPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            adapterKtorPackageName.orNull?.forEachPackage { list += it }
        }

        val layout = KotlinLayout(
            scalars.get(),
            KotlinContextLayout(
                contextPackage.toPackageName(),
                contextName,
                Decoration(contextPrefix.orNull ?: capitalizedContextName, contextPostfix.orNull),
                contextQuery.get(),
                contextMutation.get(),
                contextSubscription.get(),
                contextCommitEnabled.get()
            ),
            KotlinDtoLayout(
                dtoPackage.toPackageName(),
                Decoration(dtoPrefix.orNull, dtoPostfix.orNull),
                Decoration(dtoEnumPrefix.orNull, dtoEnumPostfix.orNull),
                Decoration(dtoInputPrefix.orNull, dtoInputPostfix.orNull),
                dtoApplyPrimaryKeys.get(),
                dtoMaxNumberOfFieldsForImmutableDtoClass.get(),
                dtoMaxNumberOfFieldsForImmutableInputClass.get(),
                KotlinDtoSerialization(
                    dtoSerializationEnabled.get(),
                    dtoSerializationClassDiscriminator.get(),
                    dtoSerializationIgnoreUnknownKeys.get(),
                    dtoSerializationEncodeDefaults.get(),
                    dtoSerializationPrettyPrint.get()
                ),
                KotlinDtoJacksonLayout(
                    dtoJacksonEnabled.get(),
                    dtoJacksonTypeInfoUse.get(),
                    dtoJacksonTypeInfoInclude.get(),
                    dtoJacksonTypeInfoProperty.get(),
                    dtoJacksonJsonInclude.get()
                ),
                KotlinDtoBuilderLayout(
                    dtoBuilderEnabled.get(),
                    Decoration(dtoBuilderPrefix.orNull, dtoBuilderPostfix.orNull),
                    dtoBuilderToBuilderFun.get(),
                    dtoBuilderToDtoFun.get(),
                    dtoBuilderToInputFun.get(),
                    dtoBuilderCopyFun.get()
                ),
                KotlinDtoGraphQLLayout(
                    dtoGraphQLEnabled.get(),
                    dtoGraphQLPackage.toPackageName(),
                    Decoration(
                        dtoGraphQLPrefix.orNull?.trim() ?: capitalizedContextName,
                        dtoGraphQLPostfix.orNull
                    )
                )
            ),
            KotlinEntityLayout(
                entityEnabled.get(),
                entityPackage.toPackageName(),
                Decoration(entityPrefix.orNull, entityPostfix.orNull),
                entityErrorsFunName.get(),
                entityExtensionsFunName.get(),
                entityContextFunEnabled.get(),
                entityContextFunName.get(),
                entityWithCurrentProjectionFun.get(),
                KotlinEntityProjectionLayout(
                    Decoration(entityProjectionPrefix.orNull, entityProjectionPostfix.orNull),
                    entityProjectionArgument.get(),
                    Decoration(entityWithPrefix.orNull, entityWithPostfix.orNull),
                    Decoration(entityWithoutPrefix.orNull, entityWithoutPostfix.orNull),
                    entityMinimizeFun.get(),
                    Decoration(entityQualificationPrefix.orNull, entityQualificationPostfix.orNull),
                    Decoration(entityQualifiedProjectionPrefix.orNull, entityQualifiedProjectionPostfix.orNull),
                    Decoration(entityOnPrefix.orNull, entityOnPostfix.orNull),
                    enableNotationWithoutParentheses.get()
                ),
                KotlinEntitySelectionLayout(
                    Decoration(entitySelectionPrefix.orNull, entitySelectionPostfix.orNull),
                    entitySelectionArgument.get(),
                    Decoration(entityQueryPrefix.orNull, entityQueryPostfix.orNull),
                    entityQueryArgument.get()
                )
            ),
            KotlinImplLayout(
                implPackage.toPackageName(),
                Decoration(implPrefix.orNull, implPostfix.orNull),
                implInternal.get(),
                Decoration(implInnerPrefix.orNull, implInnerPostfix.orNull)
            ),
            KotlinAdapterLayout(
                adapterExtendedApi.get() || !adapterThrowException.get(),
                adapterThrowException.get(),
                KotlinAdapterKtorLayout(
                    adapterKtorSimpleEnabled.get(),
                    adapterKtorCompositeEnabled.get(),
                    adapterKtorPackage.toPackageName(),
                    Decoration(
                        adapterKtorPrefix.orNull?.trim() ?: capitalizedContextName,
                        adapterKtorPostfix.orNull
                    ),
                    adapterKtorReceiveTimeoutMillis.orNull?.takeIf { it > 0L }
                )
            )
        )

        if (layout.dto.run { serialization.enabled && jackson.enabled }) {
            logger.warn("[kobby] Kotlinx serialization and Jackson serialization are not supported simultaneously.")
        }

        val targetDirectory = outputDirectory.get().asFile
        if (!targetDirectory.isDirectory && !targetDirectory.mkdirs()) {
            "Failed to create directory for generated sources: $targetDirectory".throwIt()
        }

        var schema = try {
            parseSchema(directiveLayout, *graphQLSchemaFiles.map { FileReader(it) }.toTypedArray())
        } catch (e: Exception) {
            "Schema parsing failed.".throwIt(e)
        }

        // Truncate schema
        val truncateQuery = schemaTruncateQuery.orNull
        if (!truncateQuery.isNullOrEmpty()) {
            schema = try {
                schema.truncate(
                    reportEnabled = schemaTruncateReportEnabled.get(),
                    regexEnabled = schemaTruncateRegexEnabled.get(),
                    caseSensitive = schemaTruncateCaseSensitive.get(),
                    query = truncateQuery
                ) { message ->
                    logger.warn(message)
                }
            } catch (e: Exception) {
                "Schema truncation failed.".throwIt(e)
            }
        }

        try {
            schema.validate().forEach { warning ->
                logger.warn(warning)
            }
        } catch (e: Exception) {
            "Schema validation failed.".throwIt(e)
        }

        val output = try {
            generateKotlin(schema, layout)
        } catch (e: Exception) {
            "Kotlin DSL generation failed.".throwIt(e)
        }

        output.forEach {
            it.writeTo(targetDirectory)
        }
    }

    private fun Project.resolveDependencies(): Sequence<ResolvedDependency> = this.configurations.asSequence()
        .filter { it.isCanBeResolved }
        .map { it.resolvedConfiguration }
        .map { it.lenientConfiguration }
        .flatMap { it.firstLevelModuleDependencies }
        .flatMap {
            sequence {
                yield(it)
                yieldAll(it.children)
            }
        }

    private fun Project.hasDependency(moduleGroup: String, moduleName: String): Boolean =
        this.resolveDependencies().any {
            it.moduleGroup == moduleGroup && it.moduleName == moduleName
        }
}