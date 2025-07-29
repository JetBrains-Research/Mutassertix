package mutation

/**
 * Represents the result of a mutation testing run.
 *
 * @property mutationScore The mutation testing score as an integer percentage, which
 *         reflects the effectiveness of the tests in catching mutations. Higher scores are better.
 * @property killedMutantsCount The number of mutants that were detected as dead by the testing process.
 * @property survivedMutationList A list of mutations that survived the testing process,
 *         indicating parts of the code that were not covered or affected by existing tests.
 */
data class MutationResult(
    val mutationScore: Int,
    val killedMutantsCount: Int,
    val survivedMutationList: List<String>
)
