package mutation

import data.ProjectConfig

/**
 * MutationPipeline defines the interface for executing a mutation testing pipeline.
 */
interface MutationPipeline {
    fun run(projectConfig: ProjectConfig): MutationResult
    fun run(projectConfig: ProjectConfig, targetPairIndex: Int): MutationResult
}
