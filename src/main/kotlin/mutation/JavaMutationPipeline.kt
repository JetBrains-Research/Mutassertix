package mutation

import data.ProjectConfig
import java.io.File
import java.net.URL
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Represents a pipeline for running PITest mutation testing.
 */
class JavaMutationPipeline : MutationPipeline {
    private val logger: Logger = LoggerFactory.getLogger(JavaMutationPipeline::class.java)
    private val libFolder = "build/libs"

    /**
     * Retrieves the mutation score from the PITest mutation testing report.
     *
     * @param projectConfig The configuration for the project.
     * @return The mutation score as an integer percentage.
     */
    private fun getMutationScore(projectConfig: ProjectConfig): Int {
        val mutationPattern = """<td>(\d+)%\s*<div class="coverage_bar">""".toRegex()

        val indexFile = File(projectConfig.sourceDir + "/pitest/index.html")

        if (!indexFile.exists()) return 0

        val matchResult =
            mutationPattern.findAll(indexFile.readText())
                .drop(1) // Skip the Line Coverage
                .firstOrNull()

        return matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    /**
     * Calculates the number of killed mutants from the PITest mutation testing report.
     *
     * @param projectConfig The configuration for the project.
     * @return The number of killed mutants.
     */
    private fun getKilledMutantsCount(projectConfig: ProjectConfig): Int {
        val killedMutantsPattern = """<div class="coverage_legend">(\d+)/(\d+)</div>""".toRegex()

        val indexFile = File(projectConfig.sourceDir + "/pitest/index.html")

        if (!indexFile.exists()) return 0

        val matchResult =
            killedMutantsPattern.findAll(indexFile.readText())
                .drop(1) // Skip the Line Coverage
                .firstOrNull()

        return matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    /**
     * Processes a single target class to extract survived mutations.
     *
     * @param projectConfig The configuration of the project.
     * @param targetClass The specific target class to analyze for survived mutations.
     * @return A list of strings describing the survived mutations for this specific class.
     */
    private fun processSingleClassMutations(projectConfig: ProjectConfig, targetClass: String): List<String> {
        val survivedMutations = mutableListOf<String>()

        val sourceDirPath = projectConfig.sourceDir
        val pitestDataDirPath = targetClass.substringBeforeLast(".")
        val classPath = targetClass.substringAfterLast(".") + ".java.html"

        val pitestDataFile = File("$sourceDirPath/pitest/$pitestDataDirPath/$classPath")
        if (!pitestDataFile.exists()) return emptyList()

        val fileContent = pitestDataFile.readText()

        // Pattern to find line numbers
        val lineNumberPattern = """<tr>\s*<td><a href='#[^']*_(\d+)'>(\d+)</a></td>\s*<td></td>\s*<td>""".toRegex()

        // Pattern to find survived mutations
        val survivedPattern =
            """<p class='SURVIVED'><span class='pop'>\d+\.<span><b>\d+</b><br/><b>Location : </b>([^<]+)<br/><b>Killed by : </b>none</span></span> ([^<]+) &rarr; SURVIVED</span>""".toRegex()

        // Find all line numbers and their positions in the file
        val lineNumberMatches = lineNumberPattern.findAll(fileContent)
        val lineNumbers = mutableMapOf<Int, String>()

        lineNumberMatches.forEach { match ->
            val position = match.range.first
            val lineNumber = match.groupValues[1]
            lineNumbers[position] = lineNumber
        }

        val survivedMatches = survivedPattern.findAll(fileContent)

        survivedMatches.forEachIndexed { _, matchResult ->
            val location = matchResult.groupValues[1]
            val mutationName = matchResult.groupValues[2]

            // Find the closest line number that appears before this mutation
            val position = matchResult.range.first
            val lineNumber = lineNumbers.entries
                .filter { it.key < position }
                .maxByOrNull { it.key }
                ?.value ?: "Unknown"

            // Format the information into a presentable string as required in the issue description
            val mutationInfo = "File: $targetClass, Line: $lineNumber, Location: $location, Mutation: $mutationName"
            survivedMutations.add(mutationInfo)
        }

        return survivedMutations
    }
    
    /**
     * Extracts and generates a list of survived mutations from the PITest mutation testing report for multiple target classes.
     *
     * @param projectConfig The configuration of the project.
     * @param targetClasses The list of target classes to analyze for survived mutations.
     * @return A list of strings describing the survived mutations, including line number, location, mutation type, and test coverage details.
     */
    private fun getSurvivedMutationList(projectConfig: ProjectConfig, targetClasses: List<String>): List<String> {
        val allSurvivedMutations = mutableListOf<String>()
        
        for (targetClass in targetClasses) {
            allSurvivedMutations.addAll(processSingleClassMutations(projectConfig, targetClass))
        }
        
        return allSurvivedMutations
    }

    /**
     * Generates the complete command line string to run PITest mutation testing for multiple target classes and tests.
     *
     * @param projectConfig The configuration for the project.
     * @param targetClasses List of target classes for mutation testing.
     * @param targetTests List of target tests for mutation testing.
     * @return Complete command line string for executing PITest
     */
    private fun getCommand(projectConfig: ProjectConfig, targetClasses: List<String>, targetTests: List<String>): String {
        val targetClassesStr = targetClasses.joinToString(",")
        val targetTestsStr = targetTests.joinToString(",")
        
        return "java -cp ${
            getCPLine(
                projectConfig.sourceDir,
                projectConfig.buildTool.projectDependencies,
                projectConfig.libraryDependencies
            )
        }" +
                " org.pitest.mutationtest.commandline.MutationCoverageReport" +
                " --reportDir ${projectConfig.sourceDir}/pitest" +
                " --targetClasses $targetClassesStr" +
                " --targetTests $targetTestsStr" +
                " --sourceDirs ${projectConfig.sourceDir}" +
                " --verbosity VERBOSE" +
                " --mutators ALL"
    }

    /**
     * Constructs the classpath string combining project dependencies and required PITest libraries.
     *
     * @param sourceDir Root directory containing source files
     * @param projectDependencies List of project-specific dependency paths
     * @param libraryDependencies List of lib dependency paths
     * @return Colon-separated classpath string containing all required dependencies
     */
    private fun getCPLine(
        sourceDir: String,
        projectDependencies: List<String>,
        libraryDependencies: List<String>
    ): String {
        installLibraryDependencies(libraryDependencies)

        val pitestLibs = listOf(
            "pitest-command-line-1.19.1.jar",
            "pitest-entry-1.19.1.jar",
            "pitest-1.19.1.jar",
            "pitest-junit5-plugin-1.2.2.jar",
            "junit-jupiter-api-5.12.2.jar",
            "junit-4.13.2.jar",
            "junit-platform-commons-1.12.2.jar",
            "junit-platform-launcher-1.12.2.jar",
            "junit-platform-engine-1.12.2.jar",
            "junit-jupiter-engine-5.12.2.jar",
            "kotlin-stdlib-2.1.21.jar",
            "opentest4j-1.3.0.jar",
            "hamcrest-core-1.3.jar",
            "mockito-core-5.18.0.jar",
            "mockito-junit-jupiter-5.18.0.jar",
            "testng-7.11.0.jar"
        ) + libraryDependencies.map { it.split("/").last() }

        return (projectDependencies.map { "$sourceDir/$it" } + pitestLibs.map { "$libFolder/$it" }).joinToString(":")
    }

    /**
     * Downloads and installs the specified library dependencies.
     *
     * @param libraryDependencies List of lib dependency paths
     */
    private fun installLibraryDependencies(libraryDependencies: List<String>) {
        for (lib in libraryDependencies) {
            val jarName = lib.split("/").last()

            val libFile = File(libFolder, jarName)

            if (libFile.exists()) continue

            try {
                URL(lib).openStream().use { input ->
                    libFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                logger.info("Successfully downloaded {}", jarName)
            } catch (e: Exception) {
                logger.error("Failed to download {}", jarName, e)
            }
        }
    }

    /**
     * Executes the mutation testing pipeline for the specified project configuration, target class, and target test.
     *
     * @param projectConfig The configuration for the project.
     * @param targetClass The specific target class for mutation testing.
     * @param targetTest The specific target test for mutation testing.
     * @return A MutationResult object containing the mutation testing score and a list of mutations that survived the tests.
     */
    private fun mutationPipeline(
        projectConfig: ProjectConfig,
        targetClass: String,
        targetTest: String
    ): MutationResult {
        return mutationPipeline(projectConfig, listOf(targetClass), listOf(targetTest))
    }
    
    /**
     * Executes the mutation testing pipeline for the specified project configuration, target classes, and target tests.
     *
     * @param projectConfig The configuration for the project.
     * @param targetClasses The list of target classes for mutation testing.
     * @param targetTests The list of target tests for mutation testing.
     * @return A MutationResult object containing the mutation testing score and a list of mutations that survived the tests.
     */
    private fun mutationPipeline(
        projectConfig: ProjectConfig,
        targetClasses: List<String>,
        targetTests: List<String>
    ): MutationResult {
        // Start a new process to execute the PITest command
        val process = ProcessBuilder()
            .command("sh", "-c", getCommand(projectConfig, targetClasses, targetTests))
            .redirectErrorStream(true)
            .start()

        process.inputStream.bufferedReader().use { it.readText() }

        // Wait for the PITest process to complete
        process.waitFor()

        // Collect data from the report
        val mutationScore = getMutationScore(projectConfig)
        logger.info("Mutation score: {}", mutationScore)
        val killedMutantsCount = getKilledMutantsCount(projectConfig)
        logger.info("Killed mutants count: {}", killedMutantsCount)
        val survivedMutationList = getSurvivedMutationList(projectConfig, targetClasses)
        logger.info("Survived mutations count: {}", survivedMutationList.size)
        logger.info("Survived mutations list:\n{}", survivedMutationList.joinToString("\n"))

        // Remove report folder
        File(projectConfig.sourceDir + "/pitest").deleteRecursively()

        return MutationResult(mutationScore, killedMutantsCount, survivedMutationList)
    }

    /**
     * Executes the mutation testing process for the specific target class-test pair.
     *
     * @param projectConfig The configuration for the project.
     * @param targetPairIndex The index of the target class-test pair to use.
     * @return A MutationResult object containing the mutation testing score and a list of survived mutations.
     */
    override fun run(projectConfig: ProjectConfig, targetPairIndex: Int): MutationResult {
        val pair = projectConfig.targetPairs[targetPairIndex]
        return mutationPipeline(projectConfig, pair.targetClass, pair.targetTest)
    }

    /**
     * Executes the mutation testing process for all target class-test pairs in the project.
     *
     * @param projectConfig The configuration for the project.
     * @return A MutationResult object containing the mutation testing score and a list of survived mutations.
     */
    override fun run(projectConfig: ProjectConfig): MutationResult {
        // Extract target classes and tests directly from targetPairs
        val targetClasses = projectConfig.targetPairs.map { it.targetClass }
        val targetTests = projectConfig.targetPairs.map { it.targetTest }
        
        // Run mutation testing for all target classes and tests at once
        return mutationPipeline(
            projectConfig,
            targetClasses,
            targetTests
        )
    }
}
