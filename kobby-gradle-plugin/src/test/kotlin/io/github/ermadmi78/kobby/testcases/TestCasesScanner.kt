package io.github.ermadmi78.kobby.testcases

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Created on 10.07.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class TestCasesScanner {
    companion object {
        const val TEST_KOTLIN_VERSION = "testKotlinVersion"
    }

    private val testKotlinVersion = System.getProperty(TEST_KOTLIN_VERSION)
        ?: error("$TEST_KOTLIN_VERSION is not configured")

    private fun String.applyVersions(): String = replace(TEST_KOTLIN_VERSION, testKotlinVersion)

    @Test
    fun scan(@TempDir tempDir: Path) {
        val effectiveTempDir: Path = System.getenv("KOBBY_KOTLIN_TEMP_DIR")?.let { Path.of(it) } ?: tempDir

        val resourceResolver: ResourcePatternResolver = PathMatchingResourcePatternResolver()
        val resourceScanRoot: String = javaClass.packageName.replace('.', '/') + '/'
        val failedTestCases = mutableListOf<String>()

        val testCases = resourceResolver.getResources(
            "classpath*:$resourceScanRoot**/build.gradle.kts.txt"
        )
        assertTrue(testCases.isNotEmpty(), "No test cases found")

        for (testCase in testCases) {
            assertTrue(testCase.exists())
            assertTrue(testCase.isFile)

            val parentName = testCase.file.parent
            val index = parentName.lastIndexOf(resourceScanRoot)
            assertTrue(index >= 0)

            val testCaseName = parentName.substring(index + resourceScanRoot.length)
            assertTrue(testCaseName.isNotEmpty())

            val start = Instant.now()
            try {
                println()
                println("Test case [$testCaseName] started")
                val res = executeTestCase(effectiveTempDir, resourceResolver, resourceScanRoot, testCaseName, testCase)
                val duration = Duration.between(start, Instant.now())
                if (res) {
                    println("Test case [$testCaseName] completed (${duration.format()})")
                } else {
                    failedTestCases += testCaseName
                    println("Test case [$testCaseName] failed (${duration.format()})")
                }
            } catch (e: Exception) {
                val duration = Duration.between(start, Instant.now())
                failedTestCases += testCaseName
                e.printStackTrace()
                println("Test case [$testCaseName] failed (${duration.format()})")
            }
        }

        if (failedTestCases.isNotEmpty()) {
            fail("There are failed test cases: ${failedTestCases.joinToString { it }}")
        }
    }

    private fun Duration.format(): String = seconds.absoluteValue.let {
        String.format("%02d:%02d", it / 60, it % 60)
    }

    private fun executeTestCase(
        tempDir: Path,
        resourceResolver: ResourcePatternResolver,
        resourceScanRoot: String,
        testCaseName: String,
        testCaseResource: Resource
    ): Boolean {
        val tempProjectPath = tempDir.resolve(testCaseName)
        Files.createDirectories(tempProjectPath)
        Files.readString(testCaseResource.file.toPath())!!.also {
            Files.writeString(
                tempProjectPath.resolve("build.gradle.kts"),
                it.applyVersions(),
                StandardOpenOption.CREATE_NEW
            )
        }

        val sources = resourceResolver.getResources(
            "classpath*:$resourceScanRoot$testCaseName/src/**/*.txt"
        )
        assertTrue(sources.isNotEmpty(), "Invalid testcase - no sources found")

        val testCaseRootPath = testCaseResource.file.toPath().parent!!
        for (source in sources) {
            assertTrue(source.exists())
            assertTrue(source.isFile)

            val sourcePath = source.file.toPath()
            val targetDir = tempProjectPath.resolve(testCaseRootPath.relativize(sourcePath.parent!!))
            val targetName = sourcePath.fileName.toString().let {
                it.substring(0, it.length - 4)
            }

            Files.createDirectories(targetDir)
            Files.copy(sourcePath, targetDir.resolve(targetName))
        }

        val buildResult = GradleRunner.create()
            .withProjectDir(tempProjectPath.toFile())
            .withPluginClasspath()
            .withArguments("build", "--stacktrace")
            .build()

        assertEquals(TaskOutcome.SUCCESS, buildResult.task(":kobbyKotlin")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, buildResult.task(":compileKotlin")!!.outcome)

        val expectations = resourceResolver.getResources(
            "classpath*:$resourceScanRoot$testCaseName/expected/**/*.txt"
        )
        assertTrue(expectations.isNotEmpty(), "Invalid testcase - no expectations found")

        val expectedMap = mutableMapOf<String, String>()
        for (expectation in expectations) {
            assertTrue(expectation.exists())
            assertTrue(expectation.isFile)

            val expectationPath = expectation.file.toPath()
            val expectationKey = testCaseRootPath.resolve("expected")
                .relativize(expectationPath.parent!!).resolve(
                    expectationPath.fileName.toString().let {
                        it.substring(0, it.length - 4)
                    }
                ).toString()

            expectedMap[expectationKey] = Files.readString(expectationPath)!!
        }

        val actualMap = mutableMapOf<String, String>()
        val actualRootPath = tempProjectPath
            .resolve("build")
            .resolve("generated")
            .resolve("sources")
            .resolve("kobby")
            .resolve("main")
            .resolve("kotlin")
        Files.walkFileTree(actualRootPath, object : SimpleFileVisitor<Path>() {
            override fun visitFile(actualPath: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                requireNotNull(actualPath)
                requireNotNull(attrs)

                val actualKey = actualRootPath.relativize(actualPath).toString()
                actualMap[actualKey] = Files.readString(actualPath)

                return FileVisitResult.CONTINUE
            }
        })

        var areEquals = true

        expectedMap.forEach { (expectedKey, _) ->
            if (expectedKey !in actualMap) {
                areEquals = false
                println("Expected file is missing: $expectedKey")
            }
        }

        actualMap.forEach { (actualKey, _) ->
            if (actualKey !in expectedMap) {
                areEquals = false
                println("Actual file is unexpected: $actualKey")
            }
        }

        expectedMap.forEach { (expectedKey, expectedValue) ->
            if (expectedKey in actualMap) {
                val actualValue = actualMap[expectedKey]!!
                if (expectedValue != actualValue) {
                    areEquals = false
                    println("Unexpected content of file: $expectedKey")
                }
            }
        }

        return areEquals
    }
}