package pitest

import data.ProjectConfiguration
import java.io.File
import java.net.URL

/**
 * Generator class for creating PITest mutation testing command line arguments.
 */
class PitestCommandLineGenerator {
    private val libFolder = "build/libs"

    /**
     * Generates the complete command line string to run PITest mutation testing.
     *
     * @param projectConfiguration Configuration settings for the project to be tested
     *
     * @return Complete command line string for executing PITest
     */
    fun getCommand(projectConfiguration: ProjectConfiguration): String {
        return "java -cp ${
            getCPLine(
                projectConfiguration.sourceDir,
                projectConfiguration.projectDependencies,
                projectConfiguration.libraryDependencies
            )
        }" +
                " org.pitest.mutationtest.commandline.MutationCoverageReport" +
                " --reportDir ${projectConfiguration.sourceDir}/pitest" +
                " --targetClasses ${projectConfiguration.targetClass}" +
                " --targetTests ${projectConfiguration.targetTest}" +
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
    fun getCPLine(sourceDir: String, projectDependencies: List<String>, libraryDependencies: List<String>): String {
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
            "opentest4j-1.3.0.jar",
            "hamcrest-core-1.3.jar"
        ) + libraryDependencies.map { it.split("/").last() }

        return (projectDependencies.map { "$sourceDir/$it" } + pitestLibs.map { "$libFolder/$it" }).joinToString(":")
    }

    /**
     * Downloads and installs the specified library dependencies.
     *
     * @param libraryDependencies List of lib dependency paths
     */
    fun installLibraryDependencies(libraryDependencies: List<String>) {
        for (lib in libraryDependencies) {
            val jarName = lib.split("/").last()

            val libFile = File(libFolder, jarName)

            if (libFile.exists()) {
                println("> File $jarName exists")
                continue
            }

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
}
