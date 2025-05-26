package dataset

import data.ProjectConfiguration
import java.io.File

/**
 * Interface representing a manager responsible for handling operations on datasets.
 */
abstract class DatasetManager {
    protected val projectsDir = File("projects")

    /**
     * Parses a JSON file containing project configuration data and extracts the relevant configurations.
     *
     * @param languageName Name of the language for which the projects are being extracted.
     * @return A list of ProjectConfiguration instances representing the extracted configurations
     */
    abstract fun setUpProjects(languageName: String): List<ProjectConfiguration>

    /**
     * Builds the project specified in the given ProjectConfiguration instance.
     *
     * @param projectConfiguration Configuration settings for the project to be rebuilt
     * @return Building result
     */
    abstract fun projectBuild(projectConfiguration: ProjectConfiguration): String

    /**
     * Clones a project repository from the given GitHub URL.
     *
     * @param github The URL of the GitHub repository to be cloned.
     * @return True if the repository was cloned successfully, otherwise false.
     */
    protected fun cloneProject(github: String): Boolean {
        try {
            if (!projectsDir.exists()) {
                projectsDir.mkdir()
            }

            val projectName = github.split("/").last()
            val targetDir = "${projectsDir.path}/$projectName"

            if (File(targetDir).exists()) {
                File(targetDir).deleteRecursively()
            }

            val process = ProcessBuilder("git", "clone", "https://github.com/$github", targetDir).start()

            process.inputStream.bufferedReader().use { it.readText() }
            process.errorStream.bufferedReader().use { it.readText() }

            process.waitFor()

            return File(targetDir).exists()
        } catch (_: Exception) {
            return false
        }
    }
}
