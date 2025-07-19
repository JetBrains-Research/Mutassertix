import languages.Java
import languages.LanguageConfig

/**
 * Main pipeline implementation
 */
fun main() {
    val languageConfig: LanguageConfig = Java()

    val projectConfigurations = languageConfig.datasetManager.setUpProjects(languageConfig)

    for (projectConfiguration in projectConfigurations) {
        println("> Running the pipeline for project ${projectConfiguration.projectName}")
        for (testIndex in projectConfiguration.targetTests.indices) {
            println("> Running the pipeline for target test ${projectConfiguration.targetTests[testIndex]}")
            Pipeline.run(languageConfig, projectConfiguration, testIndex)
        }
    }
}
