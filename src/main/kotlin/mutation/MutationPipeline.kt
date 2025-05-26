package mutation

import data.ProjectConfiguration

/**
 * MutationPipeline defines the interface for executing a mutation testing pipeline.
 */
interface MutationPipeline {
    /**
     * Runs the Pitest pipeline
     *
     * @param projectConfiguration Configuration settings for the project to be tested
     *
     * @return Mutation score
     */
    fun getMutationScore(projectConfiguration: ProjectConfiguration): Int
}
