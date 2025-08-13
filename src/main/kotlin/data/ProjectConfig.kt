package data

import dataset.BuildTool

/**
 * Represents a pair of target class and its corresponding test class.
 *
 * @param targetClass The class to be targeted for mutation testing
 * @param targetTest The test class that tests the target class
 */
data class ClassTestPair(
    val targetClass: String,
    val targetTest: String
)

/**
 * Represents the configuration settings for a project used in mutation testing.
 *
 * @param projectName Name of the project
 * @param github URL to the project's GitHub repository
 * @param language Programming language used in the project
 * @param sourceDir Root directory containing source files
 * @param buildTool Build tool used in the project
 * @param languagePath Path to the compiler executable
 * @param libraryDependencies List specifying library dependencies
 * @param targetPairs List of pairs where each pair contains a target class and its corresponding test
 */
data class ProjectConfig(
    val projectName: String,
    val github: String,
    val language: String,
    val sourceDir: String,
    val buildTool: BuildTool,
    val languagePath: String,
    val libraryDependencies: List<String>,
    val targetPairs: List<ClassTestPair>
)
