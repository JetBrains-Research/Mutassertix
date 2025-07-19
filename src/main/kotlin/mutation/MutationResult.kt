package mutation

/**
 * Represents the result of a mutation testing run.
 *
 * @property survivedMutationList A list of mutations that survived the testing process,
 *         indicating parts of the code that were not covered or affected by existing tests.
 * @property mutationScore The mutation testing score as an integer percentage, which
 *         reflects the effectiveness of the tests in catching mutations. Higher scores are better.
 */
data class MutationResult(
    val survivedMutationList: List<String>,
    val mutationScore: Int
)
