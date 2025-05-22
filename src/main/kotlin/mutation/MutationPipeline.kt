package mutation

import data.ProjectConfiguration

interface MutationPipeline {
    /**
     * Runs the Pitest pipeline
     *
     * @param projectConfiguration Configuration settings for the project to be tested
     *
     * @return Complete command line string for executing PITest
     */
    fun getMutationScore(projectConfiguration: ProjectConfiguration): Int
}
