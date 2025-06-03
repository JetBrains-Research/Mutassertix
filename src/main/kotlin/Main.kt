import ai.Agent
import java.io.File
import languages.LanguageConfig
import languages.Java
import org.jetbrains.research.mutassertix.agent.AgentUtils

/**
 * Main pipeline implementation
 */
fun main() {
    val languageConfig: LanguageConfig = Java()

    val projectConfigurations = languageConfig.datasetManager.setUpProjects(languageConfig)

    val reportFile = File("report.txt").bufferedWriter()

    reportFile.write("Project\tLLM Model\tInitial Mutation Score\tFinal Mutation Score\tScore Improvement\n")

    val numberOfRepeats = 3

    for (llmModel in AgentUtils.getLLModels()) {
        for (projectConfiguration in projectConfigurations) {
            repeat(numberOfRepeats) {
                println("> Running the pipeline for project ${projectConfiguration.projectName}")

                // Reset project
                languageConfig.datasetManager.resetProject(projectConfiguration)

                // Calculate initial mutation score
                languageConfig.datasetManager.projectBuild(projectConfiguration)
                val initialMutationScore = languageConfig.mutationPipeline.getMutationScore(projectConfiguration)

                // Run Assertion Generation Agent
                Agent.run(
                    llmModel,
                    projectConfiguration,
                    languageConfig.datasetManager,
                    languageConfig.mutationPipeline
                )

                // Calculate final mutation score
                languageConfig.datasetManager.projectBuild(projectConfiguration)
                val finalMutationScore = languageConfig.mutationPipeline.getMutationScore(projectConfiguration)

                // Write report
                reportFile.write(
                    "${projectConfiguration.projectName}\t${llmModel.id}\t$initialMutationScore\t" +
                            "$finalMutationScore\t${finalMutationScore - initialMutationScore}\n"
                )
            }
        }
    }
    reportFile.close()
}
