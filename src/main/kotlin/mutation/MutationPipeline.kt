package mutation

import data.ProjectConfiguration

/**
 * MutationPipeline defines the interface for executing a mutation testing pipeline.
 */
interface MutationPipeline {
    fun run(projectConfiguration: ProjectConfiguration, testIndex: Int): MutationResult
}
