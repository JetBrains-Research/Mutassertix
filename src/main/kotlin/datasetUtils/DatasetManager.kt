package datasetUtils

import data.ProjectConfiguration

/**
 * Interface representing a manager responsible for handling operations on datasets.
 */
interface DatasetManager {
    /**
     * Parses a JSON file containing project configuration data and extracts the relevant configurations.
     *
     * @param filepath Path to the JSON file containing project configuration information
     * @return A list of ProjectConfiguration instances representing the extracted configurations
     */
    fun setUpProjects(filepath: String): List<ProjectConfiguration>

    /**
     * Rebuilds the project specified in the given ProjectConfiguration instance.
     *
     * @param projectConfiguration Configuration settings for the project to be rebuilt
     */
    fun projectRebuild(projectConfiguration: ProjectConfiguration)
}
