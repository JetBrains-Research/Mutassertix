package data

/**
 * Represents a report generated during the mutation testing pipeline.
 *
 * @property projectName Name of the project for which the report is generated.
 * @property initialMutationScore Mutation score obtained in the initial testing phase.
 * @property finalMutationScore Mutation score obtained after applying assertion generation.
 * @property difference Difference between the final mutation score and the initial mutation score.
 */
data class Report(
    val projectName: String,
    val initialMutationScore: Int,
    val finalMutationScore: Int,
    val difference: Int
)
