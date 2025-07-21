import languages.Java
import languages.LanguageConfig

/**
 * Main pipeline implementation
 */
suspend fun main() {
    val languageConfig: LanguageConfig = Java()

    // Sets up the projects from the dataset
    val projectConfigurations = languageConfig.datasetManager.setUpProjects(languageConfig)

    // Run an algorithm pipeline for each project for each test file
    for (projectConfiguration in projectConfigurations) {
        for (testIndex in projectConfiguration.targetTests.indices) {
            Pipeline.run(languageConfig, projectConfiguration, testIndex)
        }
    }
}
