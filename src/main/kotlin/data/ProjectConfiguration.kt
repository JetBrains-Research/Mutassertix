package data

/**
 * Represents the configuration settings for a project used in mutation testing.
 *
 * @param sourceDir Root directory containing source files
 * @param projectDependencies List of project-specific dependencies to be included in the classpath
 * @param projectDependencies List of lib dependencies to be included in the classpath
 * @param targetClasses List specifying which classes to mutate
 * @param targetTests List specifying which tests to run
 */
data class ProjectConfiguration(
    val sourceDir: String,
    val projectDependencies: List<String>,
    val libraryDependencies: List<String>,
    val targetClasses: List<String>,
    val targetTests: List<String>
)
