package data

/**
 * Represents a build tool used for managing and building a project.
 *
 * @property name The name of the build tool (e.g., Gradle, Maven).
 * @property projectDependencies A list of project-specific dependencies associated with the build tool.
 * @property buildCommand The command required to execute the build process using this build tool.
 */
interface BuildTool {
    val name: String
    val projectDependencies: List<String>
    val buildCommand: String
}
