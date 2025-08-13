package ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.markdown.markdown
import data.ProjectConfig
import dataset.DatasetManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import org.jetbrains.research.mutassertix.agent.PrivateAgentUtils

/**
 * A specialized agent that aims to fix failing test files.
 *
 * @constructor Constructs a [TestFixerAgent] agent with a prompt executor.
 *
 * @param executor The executor managing prompt execution to communicate with the LLM.
 */
class TestFixerAgent(executor: PromptExecutor) {
    private val logger: Logger = LoggerFactory.getLogger(TestFixerAgent::class.java)
    private val numberOfIterations = 3
    private val toolRegistry = PublicAgentUtils.getToolRegistry()

    val strategy = PublicAgentUtils.createCommonStrategy("test-fixer-agent-strategy")

    val agentConfig = AIAgentConfig(
        prompt = prompt("test-fixer-agent-prompt") {
            system {
                markdown {
                    +"You are an AI agent specialized in fixing failing test files."
                    +"Based on the provided file content, project structure, and test output, you'll identify and resolve test failures."
                    +"The system will write your changes directly to the file."
                    +"You can run tests on your own."
                    +"You will make at most $numberOfIterations iterations attempting to fix the test failures before giving up."
                }
            }
        },
        model = PrivateAgentUtils.getGPT4MiniModel(),
        maxAgentIterations = 500
    )

    val agent = AIAgent(
        promptExecutor = executor,
        strategy = strategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry
    ) {
        handleEvents {
            CustomEventHandler.create()()
        }
    }

    /**
     * Runs a test file via the command line and returns the output.
     *
     * @param projectConfig The configuration for the project.
     * @param testFilePath The path to the test file.
     * @param targetTest The name of the target test.
     * @return The output from running the test.
     */
    private fun runTest(projectConfig: ProjectConfig, testFilePath: String, targetTest: String): String {
        val command = buildTestCommand(projectConfig, testFilePath, targetTest)

        // Start a new process to execute the test command
        val process = ProcessBuilder()
            .command("sh", "-c", command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }

        // Wait for the test process to complete
        process.waitFor()

        return output
    }

    /**
     * Determines whether a test file is using JUnit 4 or JUnit 5.
     *
     * @param testFilePath The path to the test file.
     * @return True if the test is using JUnit 5, false if it's using JUnit 4.
     */
    private fun isJUnit5Test(testFilePath: String): Boolean {
        val file = File(testFilePath)
        if (!file.exists()) {
            return false
        }

        val content = file.readText()

        // Check for JUnit 5 imports
        val junit5Patterns = listOf(
            "import org.junit.jupiter",
            "import static org.junit.jupiter"
        )

        // Check for JUnit 4 imports
        val junit4Patterns = listOf(
            "import org.junit.Test",
            "import static org.junit.Assert"
        )

        // If we find JUnit 5 imports, it's a JUnit 5 test
        if (junit5Patterns.any { content.contains(it) }) {
            return true
        }

        // If we find JUnit 4 imports, it's a JUnit 4 test
        if (junit4Patterns.any { content.contains(it) }) {
            return false
        }

        // Default to JUnit 5 if we can't determine
        return true
    }

    /**
     * Builds the command to run a test file.
     *
     * @param projectConfig The configuration for the project.
     * @param testFilePath The path to the test file.
     * @param targetTest The name of the target test.
     * @return The command to run the test.
     */
    private fun buildTestCommand(projectConfig: ProjectConfig, testFilePath: String, targetTest: String): String {
        val sourceDir = projectConfig.sourceDir
        val projectDependencies = projectConfig.buildTool.projectDependencies
        val libraryDependencies = projectConfig.libraryDependencies

        // Build classpath
        val classpath = buildClasspath(sourceDir, projectDependencies, libraryDependencies)

        // Determine if it's a JUnit 5 test
        val isJUnit5 = isJUnit5Test(testFilePath)

        // Build command based on JUnit version
        return if (isJUnit5) {
            // JUnit 5 command using the JUnit Platform Console Launcher
            "java -cp $classpath org.junit.platform.console.ConsoleLauncher --select-class=$targetTest"
        } else {
            // JUnit 4 command using JUnitCore
            "java -cp $classpath org.junit.runner.JUnitCore $targetTest"
        }
    }

    /**
     * Builds the classpath for running tests.
     *
     * @param sourceDir The source directory of the project.
     * @param projectDependencies The project dependencies.
     * @param libraryDependencies The library dependencies.
     * @return The classpath for running tests.
     */
    private fun buildClasspath(
        sourceDir: String,
        projectDependencies: List<String>,
        libraryDependencies: List<String>
    ): String {
        val libFolder = "build/libs"

        val junitLibs = listOf(
            "junit-4.13.2.jar",
            "hamcrest-core-1.3.jar",
            "junit-jupiter-api-5.12.2.jar",
            "junit-platform-commons-1.12.2.jar",
            "junit-platform-launcher-1.12.2.jar",
            "junit-platform-engine-1.12.2.jar",
            "junit-platform-console-1.12.2.jar",
            "junit-jupiter-engine-5.12.2.jar",
            "opentest4j-1.3.0.jar"
        ) + libraryDependencies.map { it.split("/").last() }

        return (projectDependencies.map { "$sourceDir/$it" } + junitLibs.map { "$libFolder/$it" }).joinToString(":")
    }

    /**
     * Checks if a test is passing based on the test output.
     *
     * @param testOutput The output from running the test.
     * @param isJUnit5 Whether the test is using JUnit 5.
     * @return True if the test is passing, false otherwise.
     */
    private fun isTestPassing(testOutput: String, isJUnit5: Boolean): Boolean {
        return if (isJUnit5) {
            testOutput.contains("0 tests failed")
        } else {
            !testOutput.contains("FAILURES") && !testOutput.contains("Exception")
        }
    }

    /**
     * Runs the agent to fix a failing test file.
     *
     * @param projectConfig The configuration for the project.
     * @param testFilePath The path to the test file.
     * @param className The name of the target test.
     * @return True if the test was fixed, false otherwise.
     */
    suspend fun run(
        datasetManager: DatasetManager,
        projectConfig: ProjectConfig,
        testFilePath: String,
        className: String
    ): Boolean {
        logger.info("Starting TestFixerAgent for test file: {}", testFilePath)

        val testFile = File(testFilePath)
        if (!testFile.exists()) {
            logger.warn("Test file does not exist: {}", testFilePath)
            return false
        }

        val initialFileContent = testFile.readText()
        var currentFileContent = initialFileContent
        logger.debug("Initial file content loaded, size: {} bytes", initialFileContent.length)

        // Determine if it's a JUnit 5 test
        val isJUnit5 = isJUnit5Test(testFilePath)
        logger.debug("Test framework detected: {}", if (isJUnit5) "JUnit 5" else "JUnit 4")

        logger.info("Running initial test to check status")
        var testOutput = runTest(projectConfig, testFilePath, className)
        logger.debug("Initial test output: {}", testOutput.take(500) + (if (testOutput.length > 500) "..." else ""))

        // Check if the test is already passing
        if (isTestPassing(testOutput, isJUnit5)) {
            logger.info("Test is already passing, no fixes needed")
            return true
        }

        logger.info("Test is failing, attempting to fix in up to {} iterations", numberOfIterations)

        for (index in 1..numberOfIterations) {
            logger.info("Starting fix iteration {}/{}", index, numberOfIterations)

            try {
                logger.debug("Sending test information to agent for analysis")
                agent.run(
                    markdown {
                        h1("Project Details")
                        +"Programming Language: ${projectConfig.language}"
                        +"Build System: ${projectConfig.buildTool.name}"
                        +"Source Location: ${projectConfig.sourceDir}"

                        h1("File Information")
                        +"File Path: $testFilePath"
                        h2("Current Content")
                        +currentFileContent

                        h1("Test Output")
                        +testOutput

                        h1("Instructions")
                        +"1. Analyze the test failure and identify the issue."
                        +"2. Fix the test by writing your changes DIRECTLY to the file at: $testFilePath"
                        +"3. The system will automatically run the test to verify your fix."
                        +"IMPORTANT: you can modify ONLY the test class."
                    }
                )
            } catch (e: Exception) {
                logger.error("Error in TestFixerAgent: ${e.message}", e)
                return false
            }

            logger.info("Agent provided a fix, rebuilding project and running test again")
            // Run the test again to see if it passes
            datasetManager.projectBuild(projectConfig)
            testOutput = runTest(projectConfig, testFilePath, className)
            logger.debug(
                "Test output after fix: {}",
                testOutput.take(500) + (if (testOutput.length > 500) "..." else "")
            )

            // Update the current file content for the next iteration if needed
            currentFileContent = testFile.readText()
            logger.debug("Updated file content loaded, size: {} bytes", currentFileContent.length)

            // Check if the test is now passing
            if (isTestPassing(testOutput, isJUnit5)) {
                logger.info("Test is now passing! Fix successful in iteration {}", index)
                return true
            } else {
                logger.warn("Test is still failing after iteration {}, continuing with next iteration", index)
            }
        }

        logger.warn("Failed to fix test after {} iterations", numberOfIterations)
        return false
    }
}
