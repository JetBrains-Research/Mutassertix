package languages

import dataset.DatasetManager
import mutation.MutationPipeline

/**
 * Represents a programming or natural language with properties related to datasets and processing pipelines.
 *
 * @property name The name of the language.
 * @property datasetManager The manager that handles dataset operations for the language.
 * @property mutationPipeline The pipeline responsible for applying mutations or transformations to the dataset.
 */
open class LanguageConfig(
    val name: String,
    val datasetManager: DatasetManager,
    val mutationPipeline: MutationPipeline,
)