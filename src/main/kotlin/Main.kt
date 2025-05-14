import pitest.PitestPipeline

fun main() {
    val projectConfigurations = JsonParser().getProjectConfigurations("src/main/resources/dataset.json")

    for (projectConfiguration in projectConfigurations) {
        val mutationScore = PitestPipeline().getMutationScore(projectConfiguration)
        println(mutationScore)
    }
}
