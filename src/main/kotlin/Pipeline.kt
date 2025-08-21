import ai.AssertionGenerationAgent
import ai.ProjectBuildFixerAgent
import ai.koog.prompt.executor.model.PromptExecutor
import data.ProjectConfig
import dataset.DatasetManager
import java.io.File
import mutation.EquivalentMutationDetector
import mutation.MutationPipeline
import mutation.MutationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import utils.FilesUtils

object Pipeline {
    private val logger: Logger = LoggerFactory.getLogger(Pipeline::class.java)

    suspend fun run(
        datasetManager: DatasetManager,
        mutationPipeline: MutationPipeline,
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
            val initialMutationResult = runInitialMutationPipeline(mutationPipeline, projectConfig, testIndex)
            if (initialMutationResult.mutationScore == 100) return

            // Get the next mutation to process
            var currentMutation: String? = null
            for (mutation in initialMutationResult.survivedMutationList) {
                if (processedMutations.contains(mutation)) continue

                // Check if the mutation is equivalent to the current class
                if (EquivalentMutationDetector.detect(projectConfig, executor, classFile, mutation)) {
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
                    datasetManager,
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

            // Fix project build errors
            if (!fixProjectBuildErrors(datasetManager, projectConfig, executor, testFile, initialContent)) continue

            // Logs final test file content
            logger.debug("Final content:\n{}", testFile.readText())

            // Run the final mutation pipeline and check if the score improved
            val finalMutationResult = mutationPipeline.run(projectConfig, testIndex)
            if (finalMutationResult.mutationScore < initialMutationResult.mutationScore) {
                testFile.writeText(initialContent)
                datasetManager.projectBuild(projectConfig)
            }
        }
    }

    /**
     * Executes the initial mutation pipeline using the provided mutation pipeline, project configuration,
     * and test index.
     *
     * @param mutationPipeline The mutation pipeline to be executed.
     * @param projectConfig The configuration of the project used during the mutation pipeline execution.
     * @param testIndex The index of the test case to be processed within the mutation pipeline.
     * @return The result of the mutation pipeline execution.
     */
    private fun runInitialMutationPipeline(
        mutationPipeline: MutationPipeline,
        projectConfig: ProjectConfig,
        testIndex: Int
    ): MutationResult = mutationPipeline.run(projectConfig, testIndex)

    /**
     * Executes the assertion generation agent to analyze a mutation and generate assertions based on the provided configuration and test details.
     *
     * @param datasetManager An instance of DatasetManager that provides access to necessary datasets.
     * @param projectConfig The configuration for the project used during assertion generation.
     * @param mutation The mutation string that describes the changes to be analyzed.
     * @param executor The PromptExecutor used to execute the assertion generation tasks.
     * @param testIndex The index of the test case for which assertions are being generated.
     * @param testFile A file instance pointing to the test file associated with the generation process.
     * @param initialContent The initial content of the test file before any modifications.
     * @return A Boolean indicating whether the assertion generation process succeeded or not.
     */
    private suspend fun runAssertionGenerationAgent(
        datasetManager: DatasetManager,
        projectConfig: ProjectConfig,
        mutation: String,
        executor: PromptExecutor,
        testIndex: Int,
        testFile: File,
        initialContent: String
    ): Boolean {
        logger.info("Running assertion generation agent")
        val result = AssertionGenerationAgent(executor).run(projectConfig, mutation, testIndex)
        return processAgentResult(result, datasetManager, projectConfig, testFile, initialContent)
    }

    /**
     * Fix project builds errors in the project.
     *
     * @param datasetManager The manager responsible for handling datasets and project configurations.
     * @param projectConfig The project configuration.
     * @param executor The prompt executor.
     * @param testFile The test file.
     * @param initialContent The initial content of the test file.
     * @return True if compilation errors were fixed or there were none, false otherwise.
     */
    private suspend fun fixProjectBuildErrors(
        datasetManager: DatasetManager,
        projectConfig: ProjectConfig,
        executor: PromptExecutor,
        testFile: File,
        initialContent: String
    ): Boolean {
        val result = ProjectBuildFixerAgent(executor).run(
            datasetManager,
            projectConfig,
            testFile.path,
        )
        return processAgentResult(result, datasetManager, projectConfig, testFile, initialContent)
    }

    /**
     * Processes the result from an agent by potentially*/
    private fun processAgentResult(
        agentResult: Boolean,
        datasetManager: DatasetManager,
        projectConfig: ProjectConfig,
        testFile: File,
        initialContent: String
    ): Boolean {
        if (!agentResult) {
            testFile.writeText(initialContent)
        }
        datasetManager.projectBuild(projectConfig)
        return agentResult
    }
}
