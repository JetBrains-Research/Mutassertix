package data

/**
 * Represents the configuration settings for a project used in mutation testing.
 *
 * @param language Programming language used in the project
 * @param sourceDir Root directory containing source files
 * @param buildTool Build tool used in the project
 * @param languagePath Path to the compiler executable
 * @param projectDependencies List of project-specific dependencies to be included in the classpath
 * @param projectDependencies List of lib dependencies to be included in the classpath
 * @param targetClasses List specifying which classes to mutate
 * @param targetTests List specifying which tests to run
 */
data class ProjectConfiguration(
    val language: Language,
    val sourceDir: String,
    val buildTool: BuildTool,
    val languagePath: String,
    val projectDependencies: List<String>,
    val libraryDependencies: List<String>,
    val targetClasses: List<String>,
    val targetTests: List<String>
)
