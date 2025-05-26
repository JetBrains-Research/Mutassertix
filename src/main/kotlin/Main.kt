import ai.Agent
import languages.LanguageConfig
import data.Report
import languages.Java
import utils.ReportPrinter

/**
 * Main pipeline implementation
 */
fun main() {
    val languageConfig: LanguageConfig = Java()

    val projectConfigurations = languageConfig.datasetManager.setUpProjects(languageConfig.name)

    val reports = mutableListOf<Report>()

    for (projectConfiguration in projectConfigurations) {
        println("> Running the pipeline for project ${projectConfiguration.projectName}")

        languageConfig.datasetManager.projectBuild(projectConfiguration)
        val initialMutationScore = languageConfig.mutationPipeline.getMutationScore(projectConfiguration)

        Agent.run(projectConfiguration, languageConfig.datasetManager, languageConfig.mutationPipeline)

        languageConfig.datasetManager.projectBuild(projectConfiguration)
        val finalMutationScore = languageConfig.mutationPipeline.getMutationScore(projectConfiguration)

        reports.add(
            Report(
                projectConfiguration.projectName,
                initialMutationScore,
                finalMutationScore,
                finalMutationScore - initialMutationScore
            )
        )
    }
    ReportPrinter.print(reports)
}
