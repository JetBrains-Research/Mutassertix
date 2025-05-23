import ai.Agent
import data.Report
import dataset.DatasetManager
import dataset.java.JavaDatasetManager
import mutation.MutationPipeline
import mutation.java.JavaMutationPipeline

/**
 * Main pipeline implementation
 */
fun main() {
    Agent().run("projects/cbor-java/src/main/java/co/nstant/in/cbor/CborBuilder.java")
//    val datasetManager: DatasetManager = JavaDatasetManager()
//    val mutationPipeline: MutationPipeline = JavaMutationPipeline()
//
//    val projectConfigurations = datasetManager.setUpProjects("src/main/resources/java.json")
//
//    val reports = mutableListOf<Report>()
//
//    for (projectConfiguration in projectConfigurations) {
//        val projectName = projectConfiguration.sourceDir.split("/").last()
//        println("> Running the pipeline for project \"$projectName\"")
//
//        println("> Running first mutation process")
//        val initialMutationScore = mutationPipeline.getMutationScore(projectConfiguration)
//
//        println("> Running AI assertion generation process")
//        // TODO AI implementation
//
//        println("> Rebuilding project")
//        datasetManager.projectBuild(projectConfiguration)
//
//        println("> Running second mutation process")
//        val finalMutationScore = mutationPipeline.getMutationScore(projectConfiguration)
//
//        reports.add(
//            Report(
//                projectName,
//                initialMutationScore,
//                finalMutationScore,
//                finalMutationScore - initialMutationScore
//            )
//        )
//    }
//    reportsOutput(reports)
}

/**
 * Displays a formatted output of mutation testing reports
 */
fun reportsOutput(report: MutableList<Report>) {
    // Define column headers and widths
    println("\nResults:")
    val format = "| %-25s | %15s | %15s | %15s |"
    val separator = "-".repeat(83)

    // Print header
    println(separator)
    println(
        String.format(
            format,
            "Project",
            "Initial Score",
            "Final Score",
            "Difference"
        )
    )
    println(separator)

    // Print each report row
    report.forEach { r ->
        println(
            String.format(
                format,
                r.projectName,
                r.initialMutationScore.toString(),
                r.finalMutationScore.toString(),
                r.difference.toString()
            )
        )
    }
    println(separator)
}
