package mutation.java

import data.ProjectConfiguration
import java.io.File
import mutation.MutationPipeline

/**
 * Represents a pipeline for running PITest mutation testing.
 */
class JavaMutationPipeline : MutationPipeline {
    override fun getMutationScore(projectConfiguration: ProjectConfiguration): Int {
        val pitestUtils = PitestUtils()

        // Start a new process to execute the PITest command
        val process = ProcessBuilder()
            .command("sh", "-c", pitestUtils.getCommand(projectConfiguration))
            .start()

        // Store the complete output from the PITest execution
        val output = process.inputStream.bufferedReader().use { it.readText() }

        // Wait for the PITest process to complete
        process.waitFor()

        // Remove report folder
        File(projectConfiguration.sourceDir + "/pitest").deleteRecursively()

        // Parse the PITest output and extract the mutation score
        val mutationScore = pitestUtils.getMutationScore(output)

        println("> Collecting mutation score $mutationScore")

        return mutationScore
    }
}