package pitest

import data.ProjectConfiguration

/**
 * Generator class for creating PITest mutation testing command line arguments.
 */
class PitestCommandLineGenerator {

    /**
     * Generates the complete command line string to run PITest mutation testing.
     *
     * @param projectConfiguration Configuration settings for the project to be tested
     *
     * @return Complete command line string for executing PITest
     */
    fun getCommand(projectConfiguration: ProjectConfiguration): String {
        return "java -cp ${getCPLine(projectConfiguration.sourceDir, projectConfiguration.projectDependencies)}" +
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
     * @return Colon-separated classpath string containing all required dependencies
     */
    fun getCPLine(sourceDir: String, projectDependencies: List<String>): String {
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
        )

        return (projectDependencies.map { "$sourceDir/$it" } + pitestLibs.map { "build/libs/$it" }).joinToString(":")
    }
}
