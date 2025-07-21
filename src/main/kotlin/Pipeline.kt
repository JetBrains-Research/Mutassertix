import ai.AssertionGenerationAgent
import ai.koog.prompt.markdown.markdown
import data.ProjectConfiguration
import java.io.File
import languages.LanguageConfig
import mutation.MutationResult

object Pipeline {
    private const val NUMBER_OF_ITERATIONS = 5

    suspend fun run(languageConfig: LanguageConfig, projectConfiguration: ProjectConfiguration, testIndex: Int) {
        repeat(NUMBER_OF_ITERATIONS) {
            println("> Running mutation pipeline for project ${projectConfiguration.projectName} and target test ${projectConfiguration.targetTests[testIndex]}")

            // Run mutation pipeline
            val mutationResult = languageConfig.mutationPipeline.run(projectConfiguration, testIndex)
            println("> Mutation score is ${mutationResult.mutationScore}")

            // Return if the mutation score is 100%
            if (mutationResult.mutationScore == 100) return

            // Get the target test file
            val testFile = getTestFile(projectConfiguration, testIndex)
            if (!testFile.exists()) {
                println("> Test file ${testFile.path} does not exist")
                return
            }

            // Logs initial test file content
            val initialTestFileContent = testFile.readText()
            println("> Initial test file content:\n$initialTestFileContent")

            println("> Running assertion generation agent")
            val userPrompt = assertionGenerationAgentUserPrompt(projectConfiguration, mutationResult)
            println("> User prompt:\n$userPrompt")
            AssertionGenerationAgent.run(userPrompt)

            // Logs final test file content
            val finalTestFileContent = testFile.readText()
            println("> Final test file content:\n$finalTestFileContent")

            val projectBuildResult = languageConfig.datasetManager.projectBuild(projectConfiguration)
        }
    }

    private fun assertionGenerationAgentUserPrompt(
        projectConfiguration: ProjectConfiguration,
        mutationResult: MutationResult
    ): String = markdown {
        h1("Project Details")
        +"Programming Language: ${projectConfiguration.language}"
        +"Build System: ${projectConfiguration.buildTool.name}"
        +"Source Location: ${projectConfiguration.sourceDir}"

        h1("Target Scope")
        +"Classes under test: ${projectConfiguration.targetClasses.joinToString(", ")}"
        +"Test classes: ${projectConfiguration.targetTests.joinToString(", ")}"

        h1("Survived Mutation Data")
        +mutationResult.survivedMutationList.joinToString("\n")
    }

    /**
     * Constructs a file reference to a specific test class within the project configuration.
     *
     * @param projectConfiguration The configuration of the project.
     * @param testIndex The index of the target test within the list of tests in the project configuration.
     * @return The file object pointing to the specified test class.
     */
    private fun getTestFile(projectConfiguration: ProjectConfiguration, testIndex: Int): File = File(
        "${projectConfiguration.sourceDir}/src/test/java/${
            projectConfiguration.targetTests[testIndex].replace(
                ".",
                "/"
            )
        }.java"
    )

    /**
     * Resets the content of a specified test file to its initial content.
     *
     * @param testFile The file to reset.
     * @param initialTestFileContent The original content to write back into the file.
     */
    private fun resetFile(testFile: File, initialTestFileContent: String) = testFile.writeText(
        initialTestFileContent
    )
}
