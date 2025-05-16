package data

/**
 * Represents the configuration settings for a project used in mutation testing.
 *
 * @param sourceDir Root directory containing source files
 * @param projectDependencies List of project-specific dependencies to be included in the classpath
 * @param projectDependencies List of lib dependencies to be included in the classpath
 * @param targetClass Pattern specifying which class to mutate
 * @param targetTest Pattern specifying which test to run
 */
data class ProjectConfiguration(
    val sourceDir: String,
    val projectDependencies: List<String>,
    val libraryDependencies: List<String>,
    val targetClass: String,
    val targetTest: String
)
