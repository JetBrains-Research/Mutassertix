import data.PropertiesReader
import kotlin.time.TimeSource
import languages.Java
import languages.LanguageConfig
import org.jetbrains.research.mutassertix.agent.PrivateAgentUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import utils.FilesUtils

private val logger: Logger = LoggerFactory.getLogger("Main")

/**
 * Main pipeline implementation
 */
suspend fun main() {
    val languageConfig: LanguageConfig = Java()

    // Set up the projects from the dataset
    val projectConfigurations = languageConfig.datasetManager.setUpProjects(languageConfig)

    // Run an algorithm pipeline for each project for each test file
    for (projectConfiguration in projectConfigurations) {
        logger.info("Running pipeline for project: {}", projectConfiguration.projectName)
        val startTime = TimeSource.Monotonic.markNow()

        // Create agents llm client and executor
        val llmClient = PrivateAgentUtils.getLLMClient(PropertiesReader.grazieToken)
        val executor = PrivateAgentUtils.getExecutor(llmClient)

        // Calculate the initial mutation score
        val initialMutationResult = languageConfig.mutationPipeline.run(projectConfiguration).mutationScore

        // Run a pipeline for each test file
        for (testIndex in projectConfiguration.targetPairs.indices) {
            Pipeline.run(languageConfig, projectConfiguration, executor, testIndex)
        }

        // Log the time
        val finishTime = TimeSource.Monotonic.markNow()
        logger.info("Time: {}", finishTime - startTime)

        // Report quota usage
        logger.info("Quota: {}", PrivateAgentUtils.getQuota(llmClient))

        // Report the results
        val finalMutationResult = languageConfig.mutationPipeline. run(projectConfiguration).mutationScore
        logger.info("Result: {}", "${projectConfiguration.projectName}\t$initialMutationResult\t$finalMutationResult\n")

        // Cache the project
        FilesUtils.cacheProject(projectConfiguration.projectName)
    }
}
