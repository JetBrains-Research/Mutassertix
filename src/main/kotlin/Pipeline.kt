import ai.AssertionGenerationAgent
import ai.ProjectBuildFixerAgent
import ai.TestFixerAgent
import ai.koog.prompt.executor.model.PromptExecutor
import data.ProjectConfig
import java.io.File
import languages.LanguageConfig
import mutation.EquivalentMutationDetector
import mutation.MutationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import utils.FilesUtils

object Pipeline {
    private val logger: Logger = LoggerFactory.getLogger(Pipeline::class.java)

    suspend fun run(
        languageConfig: LanguageConfig,
        projectConfig: ProjectConfig,
        executor: PromptExecutor,
        testIndex: Int
    ) {
        val processedMutations = HashSet<String>()

        val targetClassName = projectConfig.targetPairs[testIndex].targetClass
        val targetTestName = projectConfig.targetPairs[testIndex].targetTest

        logger.info("Running pipeline for target test: {}", targetTestName)

        // Get the target class file
        val classFilePath = FilesUtils.findClassFilePath(projectConfig.sourceDir, targetClassName)
        if (!classFilePath.second) return
        val classFile = File(classFilePath.first)

        // Get the target test file
        val testClassFilePath = FilesUtils.findClassFilePath(projectConfig.sourceDir, targetTestName)
        if (!testClassFilePath.second) return
        val testFile = File(testClassFilePath.first)

        while (true) {
            // Run initial mutation pipeline
            val initialMutationResult = runInitialMutationPipeline(languageConfig, projectConfig, testIndex)
            if (initialMutationResult.mutationScore == 100) return

            // Get the next mutation to process
            var currentMutation: String? = null
            for (mutation in initialMutationResult.survivedMutationList) {
                if (processedMutations.contains(mutation)) continue

                // Check if the mutation is equivalent to the current class
                if (EquivalentMutationDetector.detect(executor, classFile, mutation)) {
                    logger.info("Mutation {} is equivalent to the current class, skipping", mutation)
                    processedMutations.add(mutation)
                    continue
                }

                processedMutations.add(mutation)
                currentMutation = mutation
                break
            }
            currentMutation ?: break
            logger.info("Process mutation: {}", currentMutation)

            // Logs initial test file content
            val initialContent = testFile.readText()
            logger.debug("Initial content:\n{}", initialContent)

            // Run assertion generation agent
            if (!runAssertionGenerationAgent(
                    languageConfig,
                    projectConfig,
                    currentMutation,
                    executor,
                    testIndex,
                    testFile,
                    initialContent
                )
            ) {
                continue
            }

            // Fix compilation errors
            if (!fixCompilationErrors(languageConfig, projectConfig, executor, testFile, initialContent)) continue

            // Run test fixer agent
            if (!runTestFixerAgent(
                    languageConfig,
                    projectConfig,
                    executor,
                    testFile,
                    targetTestName,
                    initialContent
                )
            ) continue

            // Logs final test file content
            logger.debug("Final content:\n{}", testFile.readText())

            // Run the final mutation pipeline and check if the score improved
            val finalMutationResult = languageConfig.mutationPipeline.run(projectConfig, testIndex)
            if (finalMutationResult.mutationScore < initialMutationResult.mutationScore) {
                testFile.writeText(initialContent)
                languageConfig.datasetManager.projectBuild(projectConfig)
            }
        }
    }

    /**
     * Runs the initial mutation pipeline and checks if the mutation score is 100%.
     *
     * @param languageConfig The language configuration.
     * @param projectConfig The project configuration.
     * @param testIndex The index of the target test.
     * @return The mutation result if score is less than 100%, null otherwise.
     */
    private fun runInitialMutationPipeline(
        languageConfig: LanguageConfig,
        projectConfig: ProjectConfig,
        testIndex: Int
    ): MutationResult = languageConfig.mutationPipeline.run(projectConfig, testIndex)

    /**
     * Runs the assertion generation agent for each mutation in the list.
     *
     * @param projectConfig The project configuration.
     * @param mutation The mutation to process.
     * @param executor The prompt executor.
     * @return True if at least one mutation was processed successfully, false otherwise.
     */
    private suspend fun runAssertionGenerationAgent(
        languageConfig: LanguageConfig,
        projectConfig: ProjectConfig,
        mutation: String,
        executor: PromptExecutor,
        testIndex: Int,
        testFile: File,
        initialContent: String
    ): Boolean {
        logger.info("Running assertion generation agent")
        val result = AssertionGenerationAgent(executor).run(projectConfig, mutation, testIndex)
        return processAgentResult(result, languageConfig, projectConfig, testFile, initialContent)
    }

    /**
     * Fixes compilation errors in the project.
     *
     * @param languageConfig The language configuration.
     * @param projectConfig The project configuration.
     * @param executor The prompt executor.
     * @param testFile The test file.
     * @param initialContent The initial content of the test file.
     * @return True if compilation errors were fixed or there were none, false otherwise.
     */
    private suspend fun fixCompilationErrors(
        languageConfig: LanguageConfig,
        projectConfig: ProjectConfig,
        executor: PromptExecutor,
        testFile: File,
        initialContent: String
    ): Boolean {
        val result = ProjectBuildFixerAgent(executor).run(
            languageConfig,
            projectConfig,
            testFile.path,
        )
        return processAgentResult(result, languageConfig, projectConfig, testFile, initialContent)
    }

    /**
     * Runs the test fixer agent to fix failing tests.
     *
     * @param languageConfig The language configuration.
     * @param projectConfig The project configuration.
     * @param executor The prompt executor.
     * @param testFile The test file.
     * @param className The name of the target test.
     * @param initialContent The initial content of the test file.
     * @return True if tests were fixed or there were no failing tests, false otherwise.
     */
    private suspend fun runTestFixerAgent(
        languageConfig: LanguageConfig,
        projectConfig: ProjectConfig,
        executor: PromptExecutor,
        testFile: File,
        className: String,
        initialContent: String
    ): Boolean {
        logger.info("Running test fixer agent")
        val result = TestFixerAgent(executor).run(
            languageConfig,
            projectConfig,
            testFile.path,
            className,
        )
        return processAgentResult(result, languageConfig, projectConfig, testFile, initialContent)
    }

    /**
     * Processes the result of an agent's operation and manages the provided configurations and test file.
     *
     * @param agentResult A boolean indicating the outcome of the agent's result.
     * @param languageConfig The language configuration used for dataset management and project building.
     * @param projectConfig The configuration details of the project being processed.
     * @param testFile The file that may be updated based on the agent's result.
     * @param initialContent The initial content to revert to, in case the agent's result is false.
     * @return A boolean value of the agent result indicating success or failure.
     */
    private fun processAgentResult(
        agentResult: Boolean,
        languageConfig: LanguageConfig,
        projectConfig: ProjectConfig,
        testFile: File,
        initialContent: String
    ): Boolean {
        if (!agentResult) {
            testFile.writeText(initialContent)
        }
        languageConfig.datasetManager.projectBuild(projectConfig)
        return agentResult
    }
}
