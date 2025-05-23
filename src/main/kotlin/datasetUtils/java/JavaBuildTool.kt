package datasetUtils.java

import data.BuildTool

/**
 * Represents an abstract build tool for Java-based projects.
 */
abstract class JavaBuildTool : BuildTool {
    companion object {
        /**
         * Creates and returns an instance of a build tool based on the provided name.
         *
         * @param name The name of the build tool, such as "gradle" or "maven".
         * @return An instance of the build tool corresponding to the given name.
         * @throws IllegalArgumentException if the provided name does not match any supported build tools.
         */
        fun createByName(name: String): BuildTool = when (name.lowercase()) {
            Gradle().name -> Gradle()
            Maven().name -> Maven()
            else -> throw IllegalArgumentException("Unsupported build tool: $name")
        }
    }
}

/**
 * Represents the Gradle build tool.
 *
 * @property name The name of the build tool, defaulting to "gradle".
 * @property projectDependencies A list of directories that contain compiled classes associated with the project,
 * using default Gradle paths.
 * @property buildCommand The command used to initiate a build process using Gradle.
 */
class Gradle(
    override val name: String = "gradle",
    override val projectDependencies: List<String> = listOf("build/classes/java/main", "build/classes/java/test"),
    override val buildCommand: String = "./gradlew build"
) : JavaBuildTool()

/**
 * Represents the Maven build tool.
 *
 * @property name Name of the build tool, defaulting to "maven".
 * @property projectDependencies List of default project dependencies for Maven builds,
 * including `target/classes` and `target/test-classes`.
 * @property buildCommand Default command used to build projects with Maven, set to `mvn clean install`.
 */
class Maven(
    override val name: String = "maven",
    override val projectDependencies: List<String> = listOf("target/classes", "target/test-classes"),
    override val buildCommand: String = "mvn clean install"
) : JavaBuildTool()
