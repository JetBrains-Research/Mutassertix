package mutation

import data.ProjectConfiguration
import java.io.File
import java.net.URL

/**
 * Represents a pipeline for running PITest mutation testing.
 */
class JavaMutationPipeline : MutationPipeline {
    private val libFolder = "build/libs"

    /**
     * Retrieves the mutation score from the PITest mutation testing report.
     *
     * @param projectConfiguration The configuration for the project.
     * @return The mutation score as an integer percentage.
     */
    fun getMutationScore(projectConfiguration: ProjectConfiguration): Int {
        val mutationPattern = """<td>(\d+)%\s*<div class="coverage_bar">""".toRegex()

        val indexFile = File(projectConfiguration.sourceDir + "/pitest/index.html")

        if (!indexFile.exists()) return 0

        val matchResult =
            mutationPattern.findAll(indexFile.readText())
                .drop(1) // Skip the Line Coverage
                .firstOrNull()

        return matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    /**
     * Extracts and generates a list of survived mutations from the PITest mutation testing report.
     *
     * @param projectConfiguration The configuration of the project.
     * @param testIndex Index of the test from the targetClasses list in the project configuration.
     * @return A list of strings describing the survived mutations, including line number, location, mutation type, and test coverage details.
     */
    fun getSurvivedMutationsList(projectConfiguration: ProjectConfiguration, testIndex: Int): List<String> {
        val sourceDirPath = projectConfiguration.sourceDir
        val pitestDataDirPath = projectConfiguration.targetClasses[testIndex].substringBeforeLast(".")
        val classPath = projectConfiguration.targetClasses[testIndex].substringAfterLast(".") + ".java.html"

        val pitestDataFile = File("$sourceDirPath/pitest/$pitestDataDirPath/$classPath")
        if (!pitestDataFile.exists()) return emptyList()

        val fileContent = pitestDataFile.readText()

        // Pattern to find line numbers
        val lineNumberPattern = """<tr>\s*<td><a href='#[^']*_(\d+)'>(\d+)</a></td>\s*<td></td>\s*<td>""".toRegex()

        // Pattern to find survived mutations
        val survivedPattern =
            """<p class='SURVIVED'><span class='pop'>\d+\.<span><b>\d+</b><br/><b>Location : </b>([^<]+)<br/><b>Killed by : </b>none</span></span> ([^<]+) &rarr; SURVIVED</span>""".toRegex()

        // Pattern to find test coverage information
        val coveragePattern =
            """<div class="covered-tests" id="[^"]*" style="display:none;">Covered by tests:\s*<ul>\s*<li>([^<]*)</li>""".toRegex()

        // Find all line numbers and their positions in the file
        val lineNumberMatches = lineNumberPattern.findAll(fileContent)
        val lineNumbers = mutableMapOf<Int, String>()

        lineNumberMatches.forEach { match ->
            val position = match.range.first
            val lineNumber = match.groupValues[1]
            lineNumbers[position] = lineNumber
        }

        val survivedMatches = survivedPattern.findAll(fileContent)
        val coverageMatches = coveragePattern.findAll(fileContent)

        // Combine the matches into a list of strings
        val survivedMutations = mutableListOf<String>()

        survivedMatches.forEachIndexed { index, matchResult ->
            val location = matchResult.groupValues[1]
            val mutationName = matchResult.groupValues[2]

            // Get the corresponding coverage information if available
            val coverageInfo = if (index < coverageMatches.count()) {
                coverageMatches.elementAt(index).groupValues[1]
            } else {
                "No test coverage information"
            }

            // Find the closest line number that appears before this mutation
            val position = matchResult.range.first
            val lineNumber = lineNumbers.entries
                .filter { it.key < position }
                .maxByOrNull { it.key }
                ?.value ?: "Unknown"

            // Format the information into a presentable string as required in the issue description
            val mutationInfo =
                "Line: $lineNumber, Location: $location, Mutation: $mutationName, Test: $coverageInfo"
            survivedMutations.add(mutationInfo)
        }

        return survivedMutations
    }

    /**
     * Generates the complete command line string to run PITest mutation testing.
     *
     * @return Complete command line string for executing PITest
     */
    fun getCommand(projectConfiguration: ProjectConfiguration): String {
        return "java -cp ${
            getCPLine(
                projectConfiguration.sourceDir,
                projectConfiguration.buildTool.projectDependencies,
                projectConfiguration.libraryDependencies
            )
        }" +
                " org.pitest.mutationtest.commandline.MutationCoverageReport" +
                " --reportDir ${projectConfiguration.sourceDir}/pitest" +
                " --targetClasses ${projectConfiguration.targetClasses.joinToString(",")}" +
                " --targetTests ${projectConfiguration.targetTests.joinToString(",")}" +
                " --sourceDirs ${projectConfiguration.sourceDir}" +
                " --verbosity VERBOSE"
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
                println("> Successfully downloaded $jarName")
            } catch (_: Exception) {
                println("> ERROR: Failed to download $jarName")
            }
        }
    }

    /**
     * Executes the mutation testing process using PITest and returns the results.
     *
     * @param projectConfiguration The configuration for the project.
     * @param testIndex The index of the specific test within the project's targetClasses list to be used
     *        for mutation testing.
     * @return A MutationResult object containing the mutation testing score and a list of survived mutations.
     */
    override fun run(projectConfiguration: ProjectConfiguration, testIndex: Int): MutationResult {
        // Start a new process to execute the PITest command
        val process = ProcessBuilder()
            .command("sh", "-c", getCommand(projectConfiguration))
            .redirectErrorStream(true)
            .start()

        process.inputStream.bufferedReader().use { it.readText() }

        // Wait for the PITest process to complete
        process.waitFor()

        // Collect data from the report
        val mutationScore = getMutationScore(projectConfiguration)
        val survivedMutationsList = getSurvivedMutationsList(projectConfiguration, testIndex)

        // Remove report folder
        File(projectConfiguration.sourceDir + "/pitest").deleteRecursively()

        return MutationResult(survivedMutationsList, mutationScore)
    }
}