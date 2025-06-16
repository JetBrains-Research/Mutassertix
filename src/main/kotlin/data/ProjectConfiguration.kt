package data

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
 * @param targetClasses List specifying which classes to mutate
 * @param targetTests List specifying which tests to run
 */
data class ProjectConfiguration(
    val projectName: String,
    val github: String,
    val language: String,
    val sourceDir: String,
    val buildTool: BuildTool,
    val languagePath: String,
    val libraryDependencies: List<String>,
    val targetClasses: List<String>,
    val targetTests: List<String>
)
