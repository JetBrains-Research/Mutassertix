package data

/**
 * Represents the configuration settings for a project used in mutation testing.
 *
 * @param language Programming language used in the project
 * @param sourceDir Root directory containing source files
 * @param buildTool Build tool used in the project
 * @param languagePath Path to the compiler executable
 * @param libraryDependencies List specifying library dependencies
 * @param targetClasses List specifying which classes to mutate
 * @param targetTests List specifying which tests to run
 */
data class ProjectConfiguration(
    val language: Language,
    val sourceDir: String,
    val buildTool: BuildTool,
    val languagePath: String,
    val libraryDependencies: List<String>,
    val targetClasses: List<String>,
    val targetTests: List<String>
)
